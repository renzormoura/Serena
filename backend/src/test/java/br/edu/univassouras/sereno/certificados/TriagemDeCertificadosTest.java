package br.edu.univassouras.sereno.certificados;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.univassouras.sereno.calculo.SaldoHoras;
import br.edu.univassouras.sereno.classificacao.Confianca;
import br.edu.univassouras.sereno.classificacao.SugestaoDeItem;
import br.edu.univassouras.sereno.extracao.MetadadosPdf;
import br.edu.univassouras.sereno.extracao.MetodoExtracao;
import br.edu.univassouras.sereno.extracao.ResultadoExtracao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** As 11 regras da triagem automática, uma a uma. */
class TriagemDeCertificadosTest {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 11);
    static final UUID ALUNO = UUID.randomUUID();
    static final TriagemDeCertificados TRIAGEM = new TriagemDeCertificados(5, new BigDecimal("200"));

    static Certificado certificado(String titulo, String horas, int item, LocalDate data) {
        Arquivo arquivo = new Arquivo("chave-" + UUID.randomUUID(), "c.pdf", "application/pdf", 10, UUID.randomUUID().toString(), ALUNO);
        return Certificado.lancadoPeloAluno(ALUNO, arquivo, UUID.randomUUID(),
                new DadosCertificado(titulo, "Universidade de Vassouras", data, null, new BigDecimal(horas), item, null));
    }

    static ResultadoExtracao leitura(MetodoExtracao metodo, String texto, String horasLidas, Boolean nomeConfere, MetadadosPdf pdf) {
        return new ResultadoExtracao(metodo, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.ofNullable(horasLidas).map(BigDecimal::new), Optional.ofNullable(nomeConfere), List.of(), List.of(), texto, pdf);
    }

    static final String TEXTO_OK = "Certificamos que Ana participou da Oficina de Robótica, com carga horária de 8 horas.";

    static List<TipoAlerta> tipos(TriagemDeCertificados.Contexto ctx) {
        return TRIAGEM.avaliar(ctx, HOJE).stream().map(TriagemDeCertificados.Alerta::tipo).toList();
    }

    static TriagemDeCertificados.Contexto contexto(Certificado c, ResultadoExtracao leitura) {
        return new TriagemDeCertificados.Contexto(c, Optional.ofNullable(leitura), Optional.empty(), List.of(c), false, Optional.empty());
    }

    @Test
    void certificadoNormalNaoTemAlerta() {
        Certificado c = certificado("Oficina de Robótica", "8", 21, LocalDate.of(2025, 9, 10));
        assertThat(tipos(contexto(c, leitura(MetodoExtracao.TEXTO, TEXTO_OK, "8", true, MetadadosPdf.VAZIO)))).isEmpty();
    }

    @Test
    void nomeDiferenteHorasDiferentesEArquivoDeOutraContaSaoFortes() {
        Certificado c = certificado("Oficina", "80", 21, LocalDate.of(2025, 9, 10));
        var ctx = new TriagemDeCertificados.Contexto(c, Optional.of(leitura(MetodoExtracao.TEXTO, TEXTO_OK, "8", false, MetadadosPdf.VAZIO)),
                Optional.empty(), List.of(c), true, Optional.empty());

        var alertas = TRIAGEM.avaliar(ctx, HOJE);

        assertThat(alertas).extracting(TriagemDeCertificados.Alerta::tipo)
                .containsExactlyInAnyOrder(TipoAlerta.NOME_DIFERENTE, TipoAlerta.ARQUIVO_DE_OUTRA_CONTA, TipoAlerta.HORAS_DIFERENTES);
        assertThat(alertas).allMatch(a -> a.gravidade() == TipoAlerta.Gravidade.FORTE);
        assertThat(alertas).filteredOn(a -> a.tipo() == TipoAlerta.HORAS_DIFERENTES)
                .singleElement().extracting(TriagemDeCertificados.Alerta::mensagem).asString().contains("80 h", "8 h");
    }

    @Test
    void mesmoCertificadoEnviadoDuasVezesComArquivosDiferentes() {
        Certificado a = certificado("Semana Acadêmica de Engenharia", "12", 25, LocalDate.of(2025, 5, 14));
        Certificado b = certificado("SEMANA ACADEMICA DE ENGENHARIA", "12", 25, LocalDate.of(2025, 5, 14));
        Certificado outraData = certificado("Semana Acadêmica de Engenharia", "12", 25, LocalDate.of(2024, 5, 14));

        var ctx = new TriagemDeCertificados.Contexto(a, Optional.empty(), Optional.empty(), List.of(a, b, outraData), false, Optional.empty());
        assertThat(tipos(ctx)).containsExactly(TipoAlerta.CERTIFICADO_REPETIDO);

        var semRepetido = new TriagemDeCertificados.Contexto(a, Optional.empty(), Optional.empty(), List.of(a, outraData), false, Optional.empty());
        assertThat(tipos(semRepetido)).isEmpty();
    }

    @Test
    void itemTrocadoPeloAluno() {
        Arquivo arquivo = new Arquivo("chave-x", "c.pdf", "application/pdf", 10, "hash-x", ALUNO);
        Certificado c = Certificado.enviado(ALUNO, arquivo, UUID.randomUUID());
        c.receberLeitura(new Certificado.Sugestao(Optional.of("Curso"), Optional.empty(), Optional.of(LocalDate.of(2025, 3, 1)),
                Optional.empty(), Optional.of(new BigDecimal("40")),
                new SugestaoDeItem(List.of(new SugestaoDeItem.Candidato(10, 3, List.of("curso"))), Confianca.ALTA, "")));
        c.editarPeloAluno(new DadosCertificado("Curso", null, LocalDate.of(2025, 3, 1), null, new BigDecimal("40"), 46, null));

        assertThat(tipos(contexto(c, null))).containsExactly(TipoAlerta.ITEM_TROCADO);
    }

    @Test
    void arquivoIlegivelEDocumentoQueNaoECertificado() {
        Certificado c = certificado("Foto", "4", 21, LocalDate.of(2025, 9, 10));
        assertThat(tipos(contexto(c, leitura(MetodoExtracao.SEM_TEXTO, "", null, null, MetadadosPdf.VAZIO))))
                .containsExactly(TipoAlerta.ARQUIVO_ILEGIVEL);
        assertThat(tipos(contexto(c, leitura(MetodoExtracao.TEXTO, "Declaração de matrícula: o aluno está regularmente matriculado.",
                null, null, MetadadosPdf.VAZIO)))).containsExactly(TipoAlerta.NAO_PARECE_CERTIFICADO);
        assertThat(tipos(contexto(c, leitura(MetodoExtracao.TEXTO, "Pedido número 123. Total pago R$ 50,00.",
                null, null, MetadadosPdf.VAZIO)))).containsExactly(TipoAlerta.NAO_PARECE_CERTIFICADO);
    }

    @Test
    void datasSuspeitas() {
        assertThat(tipos(contexto(certificado("A", "4", 21, null), null))).containsExactly(TipoAlerta.DATA_SUSPEITA);
        assertThat(tipos(contexto(certificado("A", "4", 21, LocalDate.of(2019, 1, 10)), null))).containsExactly(TipoAlerta.DATA_SUSPEITA);

        Certificado antesDoIngresso = certificado("A", "4", 21, LocalDate.of(2022, 3, 1));
        var ctx = new TriagemDeCertificados.Contexto(antesDoIngresso, Optional.empty(), Optional.empty(), List.of(antesDoIngresso),
                false, Optional.of(LocalDate.of(2023, 1, 1)));
        assertThat(TRIAGEM.avaliar(ctx, HOJE)).singleElement()
                .extracting(TriagemDeCertificados.Alerta::mensagem).asString().contains("anterior ao ingresso");
    }

    @Test
    void cargaHorariaForaDoComumELimiteAtingido() {
        LocalDate data = LocalDate.of(2025, 9, 10);
        assertThat(tipos(contexto(certificado("A", "0", 21, data), null))).containsExactly(TipoAlerta.HORAS_FORA_DO_COMUM);
        assertThat(tipos(contexto(certificado("A", "360", 10, data), null))).containsExactly(TipoAlerta.HORAS_FORA_DO_COMUM);

        Certificado c = certificado("A", "4", 21, data);
        var detalhe = new SaldoHoras.Detalhe(c.getId(), new BigDecimal("10"), BigDecimal.ZERO, List.of("Limite de 2 ocorrências no item 21."));
        var ctx = new TriagemDeCertificados.Contexto(c, Optional.empty(), Optional.of(detalhe), List.of(c), false, Optional.empty());
        assertThat(tipos(ctx)).containsExactly(TipoAlerta.LIMITE_ATINGIDO);
    }

    @Test
    void pdfEditadoEhSoInformacao() {
        Certificado c = certificado("Oficina de Robótica", "8", 21, LocalDate.of(2025, 9, 10));
        MetadadosPdf editado = new MetadadosPdf(Optional.of("iLovePDF"), Optional.empty(), Optional.empty(), Optional.empty());
        var alertas = TRIAGEM.avaliar(contexto(c, leitura(MetodoExtracao.TEXTO, TEXTO_OK, "8", true, editado)), HOJE);
        assertThat(alertas).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(TipoAlerta.PDF_EDITADO);
            assertThat(a.gravidade()).isEqualTo(TipoAlerta.Gravidade.INFORMACAO);
        });

        MetadadosPdf modificado = new MetadadosPdf(Optional.of("Moodle"), Optional.empty(),
                Optional.of(Instant.parse("2025-01-01T10:00:00Z")), Optional.of(Instant.parse("2025-03-01T10:00:00Z")));
        assertThat(tipos(contexto(c, leitura(MetodoExtracao.TEXTO, TEXTO_OK, "8", true, modificado))))
                .containsExactly(TipoAlerta.PDF_EDITADO);
    }

    @Test
    void alertasVemDoMaisGraveParaOMenosGrave() {
        Certificado c = certificado("Oficina", "500", 21, null);
        var alertas = TRIAGEM.avaliar(contexto(c, leitura(MetodoExtracao.TEXTO, TEXTO_OK, "8", false, MetadadosPdf.VAZIO)), HOJE);
        assertThat(alertas).extracting(TriagemDeCertificados.Alerta::gravidade).isSorted();
        assertThat(alertas.getFirst().gravidade()).isEqualTo(TipoAlerta.Gravidade.FORTE);
    }
}
