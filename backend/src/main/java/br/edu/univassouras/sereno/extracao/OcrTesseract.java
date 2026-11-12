package br.edu.univassouras.sereno.extracao;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tesseract via Tess4J, em português. O OCR é pesado em CPU: no máximo {@code paralelos} leituras ao mesmo tempo.
 * Uma instância de {@link Tesseract} por chamada, porque ela não é thread-safe.
 */
final class OcrTesseract implements Ocr {

    private static final Logger log = LoggerFactory.getLogger(OcrTesseract.class);

    private final Path tessdata;
    private final Semaphore vagas;

    OcrTesseract(Path tessdata, int paralelos) {
        this.tessdata = tessdata;
        this.vagas = new Semaphore(paralelos);
    }

    @Override
    public boolean disponivel() {
        return true;
    }

    @Override
    public Optional<String> reconhecer(BufferedImage imagem) {
        try {
            vagas.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessdata.toString());
            tesseract.setLanguage("por");
            tesseract.setVariable("user_defined_dpi", "300");
            return Optional.ofNullable(tesseract.doOCR(imagem)).map(String::strip).filter(t -> !t.isEmpty());
        } catch (TesseractException | Error e) {
            // UnsatisfiedLinkError e afins quando a biblioteca nativa não carrega: leitura segue sem OCR.
            log.warn("OCR falhou: {}", e.toString());
            return Optional.empty();
        } finally {
            vagas.release();
        }
    }
}
