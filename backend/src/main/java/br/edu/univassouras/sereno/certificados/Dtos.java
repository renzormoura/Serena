package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.calculo.SaldoHoras;
import br.edu.univassouras.sereno.classificacao.Confianca;
import br.edu.univassouras.sereno.comum.Anulavel;
import br.edu.univassouras.sereno.extracao.MetodoExtracao;
import br.edu.univassouras.sereno.extracao.ResultadoExtracao;
import br.edu.univassouras.sereno.regulamento.Catalogo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

record ItemResumo(int numero, String nome, BigDecimal horasPorOcorrencia, String macroCodigo, String macroNome) {

    static ItemResumo de(Catalogo.ItemComMacro i) {
        return new ItemResumo(i.item().numero(), i.item().nome(), i.item().horasPorOcorrencia(), i.macro().codigo(), i.macro().nome());
    }
}

record ArquivoResumo(String nome, String tipoMime, long tamanhoBytes) {
}

/**
 * O que a leitura automática encontrou, para o aluno conferir.
 *
 * @param nomeConfere nulo quando não havia texto para comparar
 */
record LeituraDto(MetodoExtracao metodo, @Anulavel Boolean nomeConfere, List<String> trechos, List<String> avisos) {

    static LeituraDto de(ResultadoExtracao r) {
        return new LeituraDto(r.metodo(), r.nomeConfere().orElse(null), r.trechos(), r.avisos());
    }
}

/**
 * {@code titulo}, {@code horasCertificado} e {@code item} só ficam nulos enquanto o certificado está sendo lido
 * ou esperando a conferência do aluno.
 *
 * @param horasQueContam quanto o certificado soma na projeção (com limites); nulo se não conta (ex.: indeferido)
 * @param leitura        resultado da leitura automática; nulo em lançamentos manuais
 * @param observacaoAluno o recado do aluno para quem analisa
 * @param observacaoCoordenacao o que a coordenação escreveu na última decisão (motivo do ajuste ou do indeferimento)
 * @param avaliadoEm     quando a coordenação decidiu pela última vez
 * @param editavel       o aluno pode alterar os dados agora
 * @param excluivel      o aluno pode excluir agora
 */
record CertificadoDto(
        UUID id,
        StatusCertificado status,
        @Anulavel String titulo,
        @Anulavel String instituicao,
        @Anulavel LocalDate dataInicio,
        @Anulavel LocalDate dataFim,
        @Anulavel BigDecimal horasCertificado,
        @Anulavel ItemResumo item,
        @Anulavel BigDecimal horasQueContam,
        List<String> cortes,
        OrigemClassificacao origemClassificacao,
        ArquivoResumo arquivo,
        @Anulavel LeituraDto leitura,
        @Anulavel SugestaoItemDto sugestaoItem,
        @Anulavel String observacaoAluno,
        @Anulavel String observacaoCoordenacao,
        @Anulavel Instant avaliadoEm,
        boolean editavel,
        boolean excluivel,
        long versao,
        Instant criadoEm) {

    static CertificadoDto de(Certificado c, Catalogo catalogo, Optional<SaldoHoras.Detalhe> detalhe,
            Optional<ResultadoExtracao> leitura) {
        Optional<Catalogo.ItemComMacro> item = c.getItemNumero().flatMap(catalogo::item);
        Arquivo a = c.getArquivo();
        return new CertificadoDto(c.getId(), c.getStatus(), c.getTitulo().orElse(null), c.getInstituicao().orElse(null),
                c.getDataInicio().orElse(null), c.getDataFim().orElse(null), c.getHorasCertificado().orElse(null),
                item.map(ItemResumo::de).orElse(null),
                detalhe.map(SaldoHoras.Detalhe::horasContabilizadas).orElse(null),
                detalhe.map(SaldoHoras.Detalhe::cortes).orElse(List.of()),
                c.getOrigemClassificacao(), new ArquivoResumo(a.getNomeOriginal(), a.getTipoMime(), a.getTamanhoBytes()),
                leitura.map(LeituraDto::de).orElse(null),
                SugestaoItemDto.de(c, catalogo).orElse(null),
                c.getObservacaoAluno().orElse(null), c.getObservacaoCoordenacao().orElse(null), c.getAvaliadoEm().orElse(null),
                c.getStatus().editavelPeloAluno(), c.getStatus().excluivelPeloAluno(), c.getVersao(), c.getCriadoEm());
    }
}

/**
 * Item sugerido pela leitura automática, com alternativas. Fica registrado mesmo depois que o aluno confirma,
 * para medir o acerto das regras.
 */
record SugestaoItemDto(ItemResumo item, Confianca confianca, String motivo, List<ItemResumo> alternativas) {

    static Optional<SugestaoItemDto> de(Certificado c, Catalogo catalogo) {
        return c.getItemSugerido().flatMap(catalogo::item).map(principal -> new SugestaoItemDto(
                ItemResumo.de(principal), c.getConfiancaSugestao().orElse(Confianca.BAIXA), c.getMotivoSugestao().orElse(""),
                c.getAlternativasSugeridas().stream().flatMap(n -> catalogo.item(n).stream()).map(ItemResumo::de).toList()));
    }
}

/** Resultado por arquivo de um envio em lote: ou o certificado criado, ou o erro daquele arquivo. */
record ResultadoEnvio(String nomeArquivo, @Anulavel CertificadoDto certificado, @Anulavel String erroCodigo,
        @Anulavel String erroMensagem) {
}

record SimulacaoDto(ItemResumo item, BigDecimal horasBrutas, BigDecimal horasQueContam, List<String> cortes) {
}

/**
 * Contadores do aluno. {@code exigidas}, {@code faltantes} e {@code percentual} ficam nulos enquanto o regulamento
 * não define o total exigido.
 */
record SaldoDto(
        @Anulavel BigDecimal exigidas,
        BigDecimal integralizadas,
        BigDecimal pendentes,
        BigDecimal projecao,
        @Anulavel BigDecimal faltantes,
        @Anulavel Integer percentual,
        List<SaldoPorMacro> porMacro,
        List<SaldoPorItem> porItem,
        Map<StatusCertificado, Long> certificadosPorStatus) {

    /** @param horas integralizadas; @param horasProjetadas se tudo que está pendente for deferido */
    record SaldoPorMacro(String codigo, String nome, BigDecimal horas, BigDecimal horasProjetadas, @Anulavel BigDecimal limiteHoras) {
    }

    record SaldoPorItem(int numero, String nome, BigDecimal horas) {
    }

    static SaldoDto de(SaldoHoras s, Catalogo regulamento, Map<StatusCertificado, Long> contagem) {
        Map<StatusCertificado, Long> porStatus = new EnumMap<>(StatusCertificado.class);
        Arrays.stream(StatusCertificado.values()).forEach(st -> porStatus.put(st, contagem.getOrDefault(st, 0L)));
        return new SaldoDto(s.exigidas().orElse(null), s.integralizadas(), s.pendentes(), s.projecao(),
                s.faltantes().orElse(null), s.percentual().orElse(null),
                regulamento.macroatividades().stream()
                        .map(m -> new SaldoPorMacro(m.codigo(), m.nome(), s.porMacro().getOrDefault(m.codigo(), BigDecimal.ZERO),
                                s.porMacroProjecao().getOrDefault(m.codigo(), BigDecimal.ZERO), m.limiteHoras()))
                        .toList(),
                s.porItem().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> new SaldoPorItem(e.getKey(), regulamento.item(e.getKey()).map(i -> i.item().nome()).orElse("Item " + e.getKey()), e.getValue()))
                        .toList(),
                porStatus);
    }
}
