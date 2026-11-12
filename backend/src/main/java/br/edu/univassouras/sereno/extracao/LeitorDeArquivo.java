package br.edu.univassouras.sereno.extracao;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Obtém o texto de um PDF ou imagem: primeiro o texto embutido no PDF; se não houver (certificado escaneado ou
 * foto), OCR. Certificados têm 1 ou 2 páginas; mais que isso é ignorado.
 */
final class LeitorDeArquivo {

    static final int PAGINAS_MAXIMAS = 3;
    static final int PAGINAS_OCR = 2;
    private static final float DPI_OCR = 300f;

    private final Ocr ocr;

    LeitorDeArquivo(Ocr ocr) {
        this.ocr = ocr;
    }

    record Leitura(MetodoExtracao metodo, String texto, Optional<String> aviso, MetadadosPdf pdf) {

        Leitura(MetodoExtracao metodo, String texto, Optional<String> aviso) {
            this(metodo, texto, aviso, MetadadosPdf.VAZIO);
        }

        Leitura comPdf(MetadadosPdf metadados) {
            return new Leitura(metodo, texto, aviso, metadados);
        }
    }

    private static MetadadosPdf metadados(PDDocument pdf) {
        var info = pdf.getDocumentInformation();
        return new MetadadosPdf(limpo(info.getProducer()), limpo(info.getCreator()),
                Optional.ofNullable(info.getCreationDate()).map(java.util.Calendar::toInstant),
                Optional.ofNullable(info.getModificationDate()).map(java.util.Calendar::toInstant));
    }

    private static Optional<String> limpo(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String s = valor.strip();
        return Optional.of(s.length() > 300 ? s.substring(0, 300) : s);
    }

    Leitura ler(byte[] conteudo, String tipoMime) {
        return switch (tipoMime) {
            case "application/pdf" -> lerPdf(conteudo);
            case "image/png", "image/jpeg" -> lerImagem(conteudo);
            default -> new Leitura(MetodoExtracao.FALHA, "", Optional.of("Tipo de arquivo não suportado pela leitura automática."));
        };
    }

    private Leitura lerPdf(byte[] conteudo) {
        try (PDDocument pdf = Loader.loadPDF(conteudo)) {
            PDFTextStripper extrator = new PDFTextStripper();
            extrator.setSortByPosition(true);
            extrator.setEndPage(Math.min(pdf.getNumberOfPages(), PAGINAS_MAXIMAS));
            String texto = extrator.getText(pdf);
            MetadadosPdf meta = metadados(pdf);
            if (!TextoNormalizado.de(texto).vazio()) {
                return new Leitura(MetodoExtracao.TEXTO, texto, Optional.empty(), meta);
            }
            if (!ocr.disponivel()) {
                return semTexto().comPdf(meta);
            }
            PDFRenderer renderizador = new PDFRenderer(pdf);
            List<String> paginas = new ArrayList<>();
            for (int i = 0; i < Math.min(pdf.getNumberOfPages(), PAGINAS_OCR); i++) {
                ocr.reconhecer(renderizador.renderImageWithDPI(i, DPI_OCR, ImageType.GRAY)).ifPresent(paginas::add);
            }
            return resultadoOcr(String.join("\n", paginas)).comPdf(meta);
        } catch (InvalidPasswordException e) {
            return new Leitura(MetodoExtracao.FALHA, "", Optional.of("O PDF está protegido por senha; preencha os dados manualmente."));
        } catch (IOException | RuntimeException e) {
            return new Leitura(MetodoExtracao.FALHA, "", Optional.of("Não conseguimos abrir o PDF; preencha os dados manualmente."));
        }
    }

    private Leitura lerImagem(byte[] conteudo) {
        if (!ocr.disponivel()) {
            return semTexto();
        }
        try {
            BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(conteudo));
            if (imagem == null) {
                return new Leitura(MetodoExtracao.FALHA, "", Optional.of("Não conseguimos abrir a imagem; preencha os dados manualmente."));
            }
            return resultadoOcr(ocr.reconhecer(imagem).orElse(""));
        } catch (IOException | RuntimeException e) {
            return new Leitura(MetodoExtracao.FALHA, "", Optional.of("Não conseguimos abrir a imagem; preencha os dados manualmente."));
        }
    }

    private static Leitura resultadoOcr(String texto) {
        if (TextoNormalizado.de(texto).vazio()) {
            return new Leitura(MetodoExtracao.SEM_TEXTO, texto,
                    Optional.of("Não conseguimos ler o texto da imagem. Confira se está nítida ou preencha manualmente."));
        }
        return new Leitura(MetodoExtracao.OCR, texto,
                Optional.of("Lemos o texto a partir da imagem; erros de leitura são mais comuns. Confira os dados."));
    }

    private static Leitura semTexto() {
        return new Leitura(MetodoExtracao.SEM_TEXTO, "",
                Optional.of("O arquivo não tem texto legível (parece uma imagem). Preencha os dados manualmente."));
    }
}
