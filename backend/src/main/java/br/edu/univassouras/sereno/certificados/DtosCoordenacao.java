package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.academico.AlunoComConta;
import br.edu.univassouras.sereno.comum.Anulavel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Quem enviou o certificado, como a coordenação vê. */
record AlunoNaFila(UUID id, String nome, String matricula, String curso) {

    static AlunoNaFila de(AlunoComConta a) {
        return new AlunoNaFila(a.aluno().id(), a.nome(), a.aluno().matricula(), a.aluno().curso());
    }
}

/**
 * Um certificado esperando análise.
 *
 * @param prioridade 0 = alerta forte, 1 = alerta de atenção, 2 = sem alertas (pode ir no lote)
 * @param alunoTrocouSugestao o aluno escolheu um item diferente do sugerido pela leitura
 * @param alertas    triagem automática, do mais grave para o menos grave
 */
record ItemDaFila(CertificadoDto certificado, AlunoNaFila aluno, @Anulavel Instant enviadoParaAnalise, int prioridade,
        boolean alunoTrocouSugestao, List<TriagemDeCertificados.Alerta> alertas) {
}

/** Uma decisão do histórico, com o nome de quem decidiu. */
record AvaliacaoDto(
        TipoAvaliacao decisao,
        @Anulavel String justificativa,
        String avaliador,
        @Anulavel Integer itemAnterior,
        @Anulavel Integer itemNovo,
        @Anulavel BigDecimal horasAnteriores,
        @Anulavel BigDecimal horasNovas,
        Instant criadoEm) {
}

/** Tudo que a coordenação precisa para decidir sobre um certificado. */
record AnaliseDto(CertificadoDto certificado, AlunoNaFila aluno, SaldoDto saldoAluno, List<AvaliacaoDto> historico,
        List<TriagemDeCertificados.Alerta> alertas) {
}

/**
 * Linha da lista de alunos da coordenação.
 *
 * @param periodo      período do curso hoje, contado do ingresso; nulo sem ingresso informado
 * @param paraAnalisar certificados conferidos pelo aluno esperando a coordenação
 * @param emAjuste     certificados devolvidos ao aluno para correção
 */
record AlunoNaCoordenacao(
        UUID id,
        String nome,
        String email,
        String matricula,
        String curso,
        @Anulavel Integer anoIngresso,
        @Anulavel Integer semestreIngresso,
        @Anulavel Integer periodo,
        BigDecimal integralizadas,
        BigDecimal pendentes,
        @Anulavel BigDecimal exigidas,
        @Anulavel Integer percentual,
        long paraAnalisar,
        long emAjuste) {
}

record FichaAlunoDto(AlunoNaCoordenacao aluno, SaldoDto saldo, List<CertificadoDto> certificados) {
}

/** Resultado de um certificado num deferimento em lote: deferido, ou o motivo de não ter sido. */
record ResultadoLote(UUID id, boolean deferido, @Anulavel String erroCodigo, @Anulavel String erroMensagem) {
}
