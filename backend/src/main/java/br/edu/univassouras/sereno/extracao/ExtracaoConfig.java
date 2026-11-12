package br.edu.univassouras.sereno.extracao;

import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OCR só é ligado quando {@code sereno.ocr.tessdata} aponta para uma pasta com {@code por.traineddata}. Sem isso a
 * aplicação funciona normalmente: PDFs com texto são lidos e imagens ficam para preenchimento manual.
 */
@Configuration(proxyBeanMethods = false)
class ExtracaoConfig {

    private static final Logger log = LoggerFactory.getLogger(ExtracaoConfig.class);

    @Bean
    Ocr ocr(@Value("${sereno.ocr.tessdata:}") String tessdata, @Value("${sereno.ocr.paralelos:2}") int paralelos) {
        if (tessdata.isBlank()) {
            log.info("OCR desligado (sereno.ocr.tessdata vazio). Imagens e PDFs escaneados ficarão para preenchimento manual.");
            return Ocr.INDISPONIVEL;
        }
        Path pasta = Path.of(tessdata);
        if (!Files.isRegularFile(pasta.resolve("por.traineddata"))) {
            log.warn("OCR desligado: {} não contém por.traineddata.", pasta.toAbsolutePath());
            return Ocr.INDISPONIVEL;
        }
        log.info("OCR ligado com Tesseract (tessdata em {}).", pasta.toAbsolutePath());
        return new OcrTesseract(pasta, paralelos);
    }
}
