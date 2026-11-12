package br.edu.univassouras.sereno.academico;

import java.time.LocalDate;

/** Em que período do curso o aluno está, contado a partir do ingresso (1º semestre: jan–jun). */
public final class Periodos {

    private Periodos() {
    }

    /** Ingresso em 2021.1, hoje em 2026.2: 12º período. Nunca menor que 1. */
    public static int atual(int anoIngresso, int semestreIngresso, LocalDate hoje) {
        int semestreAtual = hoje.getMonthValue() <= 6 ? 1 : 2;
        return Math.max(1, (hoje.getYear() - anoIngresso) * 2 + (semestreAtual - semestreIngresso) + 1);
    }
}
