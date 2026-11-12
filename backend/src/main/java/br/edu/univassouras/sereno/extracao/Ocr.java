package br.edu.univassouras.sereno.extracao;

import java.awt.image.BufferedImage;
import java.util.Optional;

/** Reconhecimento de texto em imagem. Pode não estar disponível (sem Tesseract instalado). */
interface Ocr {

    boolean disponivel();

    Optional<String> reconhecer(BufferedImage imagem);

    Ocr INDISPONIVEL = new Ocr() {
        @Override
        public boolean disponivel() {
            return false;
        }

        @Override
        public Optional<String> reconhecer(BufferedImage imagem) {
            return Optional.empty();
        }
    };
}
