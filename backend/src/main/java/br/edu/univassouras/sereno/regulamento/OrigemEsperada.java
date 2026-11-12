package br.edu.univassouras.sereno.regulamento;

/** De onde a atividade de um item costuma vir. Separa itens parecidos, como palestra interna (22) e externa (48). */
public enum OrigemEsperada {
    /** Oferecida pela Universidade de Vassouras. */
    INTERNA,
    /** Fora da universidade. */
    EXTERNA,
    QUALQUER
}
