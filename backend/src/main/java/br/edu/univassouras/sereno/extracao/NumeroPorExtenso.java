package br.edu.univassouras.sereno.extracao;

import java.util.Map;
import java.util.OptionalInt;

/** "vinte", "cento e vinte", "quarenta e cinco" → número. Cobre 1 a 999, o que basta para cargas horárias. */
final class NumeroPorExtenso {

    private static final Map<String, Integer> VALORES = Map.ofEntries(
            Map.entry("um", 1), Map.entry("uma", 1), Map.entry("dois", 2), Map.entry("duas", 2), Map.entry("tres", 3),
            Map.entry("quatro", 4), Map.entry("cinco", 5), Map.entry("seis", 6), Map.entry("sete", 7), Map.entry("oito", 8),
            Map.entry("nove", 9), Map.entry("dez", 10), Map.entry("onze", 11), Map.entry("doze", 12), Map.entry("treze", 13),
            Map.entry("catorze", 14), Map.entry("quatorze", 14), Map.entry("quinze", 15), Map.entry("dezesseis", 16),
            Map.entry("dezessete", 17), Map.entry("dezoito", 18), Map.entry("dezenove", 19), Map.entry("vinte", 20),
            Map.entry("trinta", 30), Map.entry("quarenta", 40), Map.entry("cinquenta", 50), Map.entry("sessenta", 60),
            Map.entry("setenta", 70), Map.entry("oitenta", 80), Map.entry("noventa", 90), Map.entry("cem", 100),
            Map.entry("cento", 100), Map.entry("duzentos", 200), Map.entry("duzentas", 200), Map.entry("trezentos", 300),
            Map.entry("trezentas", 300), Map.entry("quatrocentos", 400), Map.entry("quatrocentas", 400),
            Map.entry("quinhentos", 500), Map.entry("quinhentas", 500), Map.entry("seiscentos", 600),
            Map.entry("setecentos", 700), Map.entry("oitocentos", 800), Map.entry("novecentos", 900));

    /** Regex (sem acento, minúsculo) que casa um número por extenso inteiro. */
    static final String PADRAO;

    static {
        String palavra = "(?:" + String.join("|", VALORES.keySet().stream().sorted((a, b) -> b.length() - a.length()).toList()) + ")";
        PADRAO = palavra + "(?:\\s+e\\s+" + palavra + ")*";
    }

    private NumeroPorExtenso() {
    }

    static OptionalInt valor(String texto) {
        int total = 0;
        for (String parte : texto.strip().split("\\s+e\\s+")) {
            Integer v = VALORES.get(parte.strip());
            if (v == null) {
                return OptionalInt.empty();
            }
            total += v;
        }
        return total > 0 ? OptionalInt.of(total) : OptionalInt.empty();
    }
}
