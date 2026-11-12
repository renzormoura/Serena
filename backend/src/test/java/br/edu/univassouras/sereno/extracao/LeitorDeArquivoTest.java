package br.edu.univassouras.sereno.extracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

class LeitorDeArquivoTest {

    /** Modelo de português do Tesseract. Os testes de OCR só rodam onde ele existe (ver README). */
    private static final Path TESSDATA = Path.of(".tessdata");

    private static boolean ocrDisponivel() {
        return Files.isRegularFile(TESSDATA.resolve("por.traineddata"));
    }

    private static BufferedImage certificadoFotografado() {
        BufferedImage imagem = new BufferedImage(1600, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, imagem.getWidth(), imagem.getHeight());
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SERIF, Font.BOLD, 64));
        g.drawString("CERTIFICADO", 560, 140);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 38));
        g.drawString("Certificamos que Lucas Andrade Silva participou da", 90, 300);
        g.drawString("Semana Academica de Engenharia, realizada em 14/05/2024,", 90, 370);
        g.drawString("com carga horaria de 12 horas.", 90, 440);
        g.dispose();
        return imagem;
    }

    private static byte[] png(BufferedImage imagem) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);
        return saida.toByteArray();
    }

    @Test
    void pdfComTextoEmbutidoNaoPrecisaDeOcr() throws IOException {
        byte[] pdf;
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            try (PDPageContentStream c = new PDPageContentStream(doc, pagina)) {
                c.beginText();
                c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                c.newLineAtOffset(50, 700);
                c.showText("Certificamos que Lucas Andrade Silva participou do evento com carga horaria de 6 horas.");
                c.endText();
            }
            doc.save(saida);
            pdf = saida.toByteArray();
        }

        LeitorDeArquivo.Leitura leitura = new LeitorDeArquivo(Ocr.INDISPONIVEL).ler(pdf, "application/pdf");

        assertThat(leitura.metodo()).isEqualTo(MetodoExtracao.TEXTO);
        assertThat(leitura.texto()).contains("carga horaria de 6 horas");
    }

    @Test
    void imagemSemOcrDisponivelViraPreenchimentoManual() throws IOException {
        LeitorDeArquivo.Leitura leitura = new LeitorDeArquivo(Ocr.INDISPONIVEL).ler(png(certificadoFotografado()), "image/png");

        assertThat(leitura.metodo()).isEqualTo(MetodoExtracao.SEM_TEXTO);
        assertThat(leitura.aviso()).isPresent();
    }

    @Test
    void arquivoCorrompidoNaoDerrubaALeitura() {
        LeitorDeArquivo.Leitura leitura = new LeitorDeArquivo(Ocr.INDISPONIVEL)
                .ler("%PDF-1.4 lixo".getBytes(), "application/pdf");

        assertThat(leitura.metodo()).isEqualTo(MetodoExtracao.FALHA);
    }

    @Test
    void fotoDeCertificadoELidaPorOcr() throws IOException {
        assumeTrue(ocrDisponivel(), "Sem backend/.tessdata/por.traineddata: teste de OCR pulado");
        LeitorDeArquivo leitor = new LeitorDeArquivo(new OcrTesseract(TESSDATA, 1));

        LeitorDeArquivo.Leitura leitura = leitor.ler(png(certificadoFotografado()), "image/png");
        ExtratorDeCampos.Campos campos = new ExtratorDeCampos(LocalDate.of(2026, 11, 11))
                .extrair(leitura.texto(), "Lucas Andrade Silva");

        assertThat(leitura.metodo()).isEqualTo(MetodoExtracao.OCR);
        assertThat(campos.horas()).hasValueSatisfying(h -> assertThat(h).isEqualByComparingTo("12"));
        assertThat(campos.dataInicio()).contains(LocalDate.of(2024, 5, 14));
        assertThat(campos.nomeConfere()).contains(true);
    }

    @Test
    void pdfEscaneadoSemTextoCaiNoOcr() throws IOException {
        assumeTrue(ocrDisponivel(), "Sem backend/.tessdata/por.traineddata: teste de OCR pulado");
        byte[] pdf;
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            PDImageXObject imagem = LosslessFactory.createFromImage(doc, certificadoFotografado());
            try (PDPageContentStream c = new PDPageContentStream(doc, pagina)) {
                c.drawImage(imagem, 20, 400, 572, 250);
            }
            doc.save(saida);
            pdf = saida.toByteArray();
        }

        LeitorDeArquivo.Leitura leitura = new LeitorDeArquivo(new OcrTesseract(TESSDATA, 1)).ler(pdf, "application/pdf");

        assertThat(leitura.metodo()).isEqualTo(MetodoExtracao.OCR);
        assertThat(leitura.texto().toLowerCase()).contains("12 horas");
    }
}
