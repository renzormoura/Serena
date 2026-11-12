package br.edu.univassouras.sereno.academico;

import br.edu.univassouras.sereno.comum.Anulavel;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * O que os outros módulos sabem de um aluno.
 *
 * @param anoIngresso      nulo quando ninguém informou (o cadastro não pede)
 * @param semestreIngresso nulo junto com o ano
 */
public record AlunoResumo(
        UUID id,
        UUID usuarioId,
        String matricula,
        UUID cursoId,
        String curso,
        UUID regulamentoId,
        @Anulavel Integer anoIngresso,
        @Anulavel Integer semestreIngresso) {

    /** Período do curso na data informada; vazio sem ingresso. */
    public Optional<Integer> periodoEm(LocalDate hoje) {
        if (anoIngresso == null || semestreIngresso == null) {
            return Optional.empty();
        }
        return Optional.of(Periodos.atual(anoIngresso, semestreIngresso, hoje));
    }
}
