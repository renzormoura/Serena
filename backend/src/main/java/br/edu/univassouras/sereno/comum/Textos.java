package br.edu.univassouras.sereno.comum;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Normalização de texto para comparação: sem acento, minúsculo, só letras e números, sem palavras de ligação. */
public final class Textos {

    private static final Set<String> LIGACAO = Set.of("a", "o", "as", "os", "de", "da", "do", "das", "dos", "e", "em",
            "no", "na", "nos", "nas", "ao", "aos", "para", "por", "pela", "pelo", "com", "um", "uma");

    private Textos() {
    }

    /**
     * "Curso de Extensão — SQL" → "curso extensao sql". Usado tanto no texto quanto nas palavras-chave, então
     * "curso de extensão" e "curso extensao" casam.
     */
    public static String paraBusca(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return Arrays.stream(semAcento.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(p -> !p.isEmpty() && !LIGACAO.contains(p))
                .collect(Collectors.joining(" "));
    }
}
