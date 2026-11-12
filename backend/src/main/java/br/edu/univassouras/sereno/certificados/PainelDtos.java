package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.comum.Anulavel;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Painel da coordenação (PLANO-DO-PROJETO.md §13).
 *
 * @param exigidas            total exigido pelo regulamento; nulo enquanto a decisão D1 não for registrada
 * @param percentualConcluiram dos alunos, quantos já integralizaram tudo; nulo sem total exigido
 * @param diasMedioAnalise    tempo médio entre o aluno salvar e a coordenação decidir; nulo sem decisões
 * @param modoFaixas          PERCENTUAL (com total exigido) ou HORAS (sem ele)
 * @param atencaoDisponivel   a lista de alunos em atenção só existe com o total exigido
 */
record PainelDto(
        FiltrosDisponiveis filtros,
        int alunos,
        @Anulavel BigDecimal exigidas,
        long concluiram,
        @Anulavel Integer percentualConcluiram,
        long aguardandoAnalise,
        @Anulavel Double diasMedioAnalise,
        String modoFaixas,
        List<Faixa> faixas,
        List<PorIngresso> porIngresso,
        List<ItemUsado> itensMaisUsados,
        List<DecisoesPorItem> decisoesPorItem,
        boolean atencaoDisponivel,
        int periodoAtencao,
        int percentualAtencao,
        List<AlunoEmAtencao> emAtencao,
        Automacao automacao) {

    record CursoOpcao(UUID id, String nome) {
    }

    record FiltrosDisponiveis(List<CursoOpcao> cursos, List<Integer> anosIngresso) {
    }

    record Faixa(String rotulo, long alunos) {
    }

    /** @param anoIngresso nulo agrupa os alunos sem ingresso informado */
    record PorIngresso(@Anulavel Integer anoIngresso, long alunos, BigDecimal mediaIntegralizadas) {
    }

    record ItemUsado(int numero, String nome, long certificados) {
    }

    record DecisoesPorItem(int numero, String nome, long deferidos, long indeferidos, long ajustes) {
    }

    record AlunoEmAtencao(UUID id, String nome, String matricula, String curso, int periodo, BigDecimal integralizadas,
            int percentual) {
    }

    /** @param percentualAceitas nulo enquanto nenhum certificado com sugestão foi conferido */
    record Automacao(long conferidos, @Anulavel Integer percentualAceitas, List<Troca> trocasMaisComuns) {
    }

    record Troca(int sugerido, String nomeSugerido, int escolhido, String nomeEscolhido, long vezes) {
    }
}
