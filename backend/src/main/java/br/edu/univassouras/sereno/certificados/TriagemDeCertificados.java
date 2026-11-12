package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.calculo.SaldoHoras;
import br.edu.univassouras.sereno.comum.Textos;
import br.edu.univassouras.sereno.extracao.MetodoExtracao;
import br.edu.univassouras.sereno.extracao.MetadadosPdf;
import br.edu.univassouras.sereno.extracao.ResultadoExtracao;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Triagem automática por regras (sem IA): aponta o que merece um olhar antes de validar. As regras e os cortes
 * foram aprovados em 11/11/2026; os cortes ficam em {@code sereno.triagem.*}.
 */
@Component
class TriagemDeCertificados {

    /** Programas que editam PDFs ou imagens. Certificados emitidos por plataformas raramente passam por eles. */
    private static final List<String> EDITORES = List.of("ilovepdf", "smallpdf", "sejda", "pdfescape", "pdf-xchange",
            "pdfelement", "foxit phantompdf", "photoshop", "gimp", "paint.net", "pdf candy", "pdf24", "soda pdf", "nitro");

    /** Palavras de quem atesta uma atividade. Sem nenhuma delas, o documento pode não ser um certificado. */
    private static final List<String> SINAIS_DE_CERTIFICADO = List.of("certific", "particip", "conclu", "ministr",
            "organiz", "apresent", "monitor", "carga horaria", "declara", "atesta", "horas");

    /** Documentos que costumam ser enviados por engano. */
    private static final List<String> NAO_CERTIFICADOS = List.of("boleto", "comprovante de pagamento",
            "declaracao de matricula", "comprovante de matricula", "comprovante de inscricao", "inscricao realizada",
            "inscricao confirmada", "nota fiscal", "recibo de pagamento", "linha digitavel");

    record Alerta(TipoAlerta tipo, TipoAlerta.Gravidade gravidade, String mensagem) {

        static Alerta de(TipoAlerta tipo, String mensagem) {
            return new Alerta(tipo, tipo.gravidade(), mensagem);
        }
    }

    /**
     * @param outrosDoAluno       os demais certificados ativos do mesmo aluno
     * @param arquivoEmOutraConta o mesmo arquivo (mesmo hash) também está ativo na conta de outro aluno
     * @param ingresso            início do semestre de ingresso, se informado
     */
    record Contexto(Certificado certificado, Optional<ResultadoExtracao> leitura, Optional<SaldoHoras.Detalhe> detalhe,
            List<Certificado> outrosDoAluno, boolean arquivoEmOutraConta, Optional<LocalDate> ingresso) {
    }

    private final int anosMaximos;
    private final BigDecimal horasMaximas;

    TriagemDeCertificados(@Value("${sereno.triagem.anos-maximos:5}") int anosMaximos,
            @Value("${sereno.triagem.horas-maximas:200}") BigDecimal horasMaximas) {
        this.anosMaximos = anosMaximos;
        this.horasMaximas = horasMaximas;
    }

    /** Alertas do certificado, do mais grave para o menos grave. */
    List<Alerta> avaliar(Contexto ctx, LocalDate hoje) {
        Certificado c = ctx.certificado();
        Optional<ResultadoExtracao> leitura = ctx.leitura();
        List<Alerta> alertas = new ArrayList<>();

        // 1. Nome diferente
        if (leitura.flatMap(ResultadoExtracao::nomeConfere).filter(confere -> !confere).isPresent()) {
            alertas.add(Alerta.de(TipoAlerta.NOME_DIFERENTE, "O nome do aluno não aparece no certificado."));
        }
        // 2. Arquivo usado por outra conta
        if (ctx.arquivoEmOutraConta()) {
            alertas.add(Alerta.de(TipoAlerta.ARQUIVO_DE_OUTRA_CONTA,
                    "Este mesmo arquivo também foi enviado por outro aluno."));
        }
        // 3. Certificado repetido com outro arquivo
        ctx.outrosDoAluno().stream()
                .filter(outro -> !outro.getId().equals(c.getId()) && mesmaAtividade(c, outro))
                .findFirst()
                .ifPresent(outro -> alertas.add(Alerta.de(TipoAlerta.CERTIFICADO_REPETIDO,
                        "O aluno já enviou um certificado com o mesmo nome e a mesma data (\""
                                + outro.getTitulo().orElse("") + "\").")));
        // 4. Horas diferentes do certificado
        Optional<BigDecimal> lidas = leitura.flatMap(ResultadoExtracao::horas);
        if (lidas.isPresent() && c.getHorasCertificado().isPresent() && lidas.get().compareTo(c.getHorasCertificado().get()) != 0) {
            alertas.add(Alerta.de(TipoAlerta.HORAS_DIFERENTES, "O aluno informou " + horas(c.getHorasCertificado().get())
                    + ", mas o certificado diz " + horas(lidas.get()) + "."));
        }
        // 5. Item trocado
        if (c.getItemSugerido().isPresent() && c.getItemNumero().isPresent()
                && !c.getItemSugerido().get().equals(c.getItemNumero().get())) {
            alertas.add(Alerta.de(TipoAlerta.ITEM_TROCADO, "A leitura sugeriu o item " + c.getItemSugerido().get()
                    + " e o aluno escolheu o " + c.getItemNumero().get() + "."));
        }
        // 6. Arquivo ilegível
        if (leitura.map(ResultadoExtracao::metodo).filter(m -> m == MetodoExtracao.SEM_TEXTO || m == MetodoExtracao.FALHA).isPresent()) {
            alertas.add(Alerta.de(TipoAlerta.ARQUIVO_ILEGIVEL,
                    "Não foi possível ler o texto do arquivo. Confira os dados direto na imagem."));
        }
        // 7. Data suspeita
        dataSuspeita(c, ctx.ingresso(), hoje).ifPresent(m -> alertas.add(Alerta.de(TipoAlerta.DATA_SUSPEITA, m)));
        // 8. Carga horária fora do comum
        c.getHorasCertificado().ifPresent(h -> {
            if (h.signum() == 0) {
                alertas.add(Alerta.de(TipoAlerta.HORAS_FORA_DO_COMUM, "Carga horária de 0 h."));
            } else if (h.compareTo(horasMaximas) > 0) {
                alertas.add(Alerta.de(TipoAlerta.HORAS_FORA_DO_COMUM,
                        "Carga horária de " + horas(h) + ", acima de " + horas(horasMaximas) + "."));
            }
        });
        // 9. Limite atingido
        ctx.detalhe().filter(d -> !d.cortes().isEmpty()).ifPresent(d -> alertas.add(Alerta.de(TipoAlerta.LIMITE_ATINGIDO,
                "Vai contar " + horas(d.horasContabilizadas()) + ": " + String.join(" ", d.cortes()))));
        // 10. Não parece certificado
        leitura.map(ResultadoExtracao::texto).filter(t -> !t.isBlank()).flatMap(TriagemDeCertificados::naoPareceCertificado)
                .ifPresent(m -> alertas.add(Alerta.de(TipoAlerta.NAO_PARECE_CERTIFICADO, m)));
        // 11. PDF editado
        leitura.map(ResultadoExtracao::pdf).flatMap(TriagemDeCertificados::pdfEditado)
                .ifPresent(m -> alertas.add(Alerta.de(TipoAlerta.PDF_EDITADO, m)));

        alertas.sort(Comparator.comparing(Alerta::gravidade));
        return alertas;
    }

    /** Mesmo título (sem acentos e caixa) e mesma data; se as duas instituições existem, também iguais. */
    static boolean mesmaAtividade(Certificado a, Certificado b) {
        if (a.getTitulo().isEmpty() || b.getTitulo().isEmpty()) {
            return false;
        }
        boolean mesmoTitulo = Textos.paraBusca(a.getTitulo().get()).equals(Textos.paraBusca(b.getTitulo().get()));
        boolean mesmaData = Objects.equals(a.getDataInicio().or(a::getDataFim).orElse(null), b.getDataInicio().or(b::getDataFim).orElse(null));
        boolean instituicaoCompativel = a.getInstituicao().isEmpty() || b.getInstituicao().isEmpty()
                || Textos.paraBusca(a.getInstituicao().get()).equals(Textos.paraBusca(b.getInstituicao().get()));
        return mesmoTitulo && mesmaData && instituicaoCompativel;
    }

    private Optional<String> dataSuspeita(Certificado c, Optional<LocalDate> ingresso, LocalDate hoje) {
        Optional<LocalDate> data = c.getDataInicio().or(c::getDataFim);
        if (data.isEmpty()) {
            return Optional.of("O certificado está sem data.");
        }
        if (data.get().isBefore(hoje.minusYears(anosMaximos))) {
            return Optional.of("Atividade de " + data.get().getYear() + ", há mais de " + anosMaximos + " anos.");
        }
        if (ingresso.isPresent() && data.get().isBefore(ingresso.get())) {
            return Optional.of("Atividade anterior ao ingresso do aluno no curso.");
        }
        return Optional.empty();
    }

    static Optional<String> naoPareceCertificado(String texto) {
        String busca = " " + Textos.paraBusca(texto) + " ";
        Optional<String> documento = NAO_CERTIFICADOS.stream().filter(n -> busca.contains(Textos.paraBusca(n))).findFirst();
        if (documento.isPresent()) {
            return Optional.of("O texto parece de outro tipo de documento (\"" + documento.get() + "\").");
        }
        boolean temSinal = SINAIS_DE_CERTIFICADO.stream().anyMatch(s -> busca.contains(s));
        return temSinal ? Optional.empty()
                : Optional.of("O texto não tem palavras de certificado (\"certificamos\", \"participou\", \"carga horária\").");
    }

    static Optional<String> pdfEditado(MetadadosPdf pdf) {
        String programas = (pdf.produtor().orElse("") + " " + pdf.criador().orElse("")).toLowerCase(Locale.ROOT);
        Optional<String> editor = EDITORES.stream().filter(programas::contains).findFirst();
        if (editor.isPresent()) {
            return Optional.of("O PDF passou por um editor (" + pdf.produtor().or(pdf::criador).orElse(editor.get()) + ").");
        }
        if (pdf.criadoEm().isPresent() && pdf.modificadoEm().isPresent()
                && Duration.between(pdf.criadoEm().get(), pdf.modificadoEm().get()).toHours() >= 24) {
            return Optional.of("O PDF foi modificado depois de criado.");
        }
        return Optional.empty();
    }

    private static String horas(BigDecimal h) {
        return h.stripTrailingZeros().toPlainString().replace('.', ',') + " h";
    }
}
