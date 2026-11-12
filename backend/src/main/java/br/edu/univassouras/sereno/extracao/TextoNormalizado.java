package br.edu.univassouras.sereno.extracao;

import java.text.Normalizer;
import java.util.Locale;

/**
 * O texto do certificado em duas formas com o mesmo comprimento: {@code original} (espaços já colapsados) e
 * {@code busca} (minúsculo, sem acentos). Uma posição encontrada em {@code busca} vale em {@code original}, o que
 * permite procurar sem se preocupar com acentos e mostrar o trecho como está no certificado.
 */
record TextoNormalizado(String original, String busca) {

    static TextoNormalizado de(String bruto) {
        String limpo = bruto
                .replace(' ', ' ')
                .replaceAll("(\\p{L})-\\s*\\n\\s*(\\p{L})", "$1$2")   // "ho-\nrária" → "horária"
                .replaceAll("\\s+", " ")
                .strip();
        StringBuilder busca = new StringBuilder(limpo.length());
        for (int i = 0; i < limpo.length(); i++) {
            busca.append(semAcento(limpo.charAt(i)));
        }
        // Erros típicos de OCR dentro de palavras: "particip0u" → "participou", "CERT1FICADO" → "certificado".
        String corrigido = busca.toString()
                .replaceAll("(?<=\\p{L})0(?=\\p{L})", "o")
                .replaceAll("(?<=\\p{L})1(?=\\p{L})", "i");
        return new TextoNormalizado(limpo, corrigido);
    }

    /** Um caractere vira exatamente um caractere, para manter o alinhamento entre as duas formas. */
    private static char semAcento(char c) {
        String decomposto = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
        String minusculo = decomposto.substring(0, 1).toLowerCase(Locale.ROOT);
        return minusculo.length() == 1 ? minusculo.charAt(0) : c;
    }

    static String normalizar(String texto) {
        return de(texto).busca();
    }

    String trecho(int inicio, int fim) {
        return original.substring(Math.max(0, inicio), Math.min(original.length(), fim)).strip();
    }

    boolean vazio() {
        return original.chars().filter(Character::isLetter).count() < 20;
    }
}
