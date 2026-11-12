package br.edu.univassouras.sereno.certificados.armazenamento;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tipos aceitos, reconhecidos pelos primeiros bytes do conteúdo — nunca pela extensão ou pelo
 * Content-Type enviado pelo navegador, que o cliente controla.
 */
public enum TipoArquivo {
    PDF("application/pdf", "pdf", new byte[] {'%', 'P', 'D', 'F', '-'}),
    PNG("image/png", "png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
    JPEG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    private final String mime;
    private final String extensao;
    private final byte[] assinatura;

    TipoArquivo(String mime, String extensao, byte[] assinatura) {
        this.mime = mime;
        this.extensao = extensao;
        this.assinatura = assinatura;
    }

    public String mime() {
        return mime;
    }

    public String extensao() {
        return extensao;
    }

    public static Optional<TipoArquivo> detectar(byte[] conteudo) {
        return Arrays.stream(values())
                .filter(t -> conteudo.length >= t.assinatura.length
                        && Arrays.equals(conteudo, 0, t.assinatura.length, t.assinatura, 0, t.assinatura.length))
                .findFirst();
    }
}
