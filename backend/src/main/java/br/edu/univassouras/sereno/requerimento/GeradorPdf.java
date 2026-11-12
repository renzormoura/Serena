package br.edu.univassouras.sereno.requerimento;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Requerimento em PDF, com o conteúdo e a ordem da aba INTEGRALIZAÇÃO. A macroatividade vira uma faixa acima de
 * cada grupo (em vez da coluna vertical mesclada da planilha), para a tabela poder quebrar entre páginas.
 */
final class GeradorPdf {

    private static final Color BORDA = new Color(0x33, 0x33, 0x33);
    private static final Color FAIXA = new Color(0xF2, 0xEA, 0xEC);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private GeradorPdf() {
    }

    static byte[] gerar(Requerimento r) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 32, 32);
        try {
            PdfWriter.getInstance(doc, saida);
            doc.addTitle("Requerimento de integralização — " + r.getNomeAluno());
            doc.addCreator("Sereno");
            doc.open();

            Image logo = Image.getInstance(TextosRequerimento.LOGO);
            logo.setAlt(TextosRequerimento.UNIVERSIDADE);
            logo.scaleToFit(200, 52);
            logo.setAlignment(Element.ALIGN_CENTER);
            logo.setSpacingAfter(6);
            doc.add(logo);
            doc.add(centralizado(TextosRequerimento.PRO_REITORIA, fonte(12, Font.NORMAL), 2));
            doc.add(centralizado(TextosRequerimento.curso(r.getCurso()), fonte(11, Font.NORMAL), 14));

            doc.add(topo(r));
            doc.add(centralizado(TextosRequerimento.destinatario(r.getCurso()), fonte(10, Font.BOLD), 8));
            doc.add(dadosDoAluno(r));

            Paragraph pedido = new Paragraph(TextosRequerimento.PEDIDO, fonte(9, Font.NORMAL));
            pedido.setAlignment(Element.ALIGN_JUSTIFIED);
            pedido.setSpacingBefore(10);
            pedido.setSpacingAfter(10);
            doc.add(pedido);

            doc.add(tabela(r));

            Paragraph rodape = new Paragraph("Requerimento nº " + r.getNumero() + " gerado "
                    + (r.isGeradoPeloAluno() ? "pelo aluno " : "") + "no Sereno em "
                    + DATA_HORA.format(r.getGeradoEm()) + ".", fonte(7.5f, Font.NORMAL, Color.GRAY));
            rodape.setSpacingBefore(8);
            doc.add(rodape);
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("Não foi possível gerar o PDF do requerimento", e);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
        return saida.toByteArray();
    }

    /** Total e data à esquerda; assinatura da coordenação à direita. */
    private static PdfPTable topo(Requerimento r) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] {22, 22, 6, 50});
        t.setWidthPercentage(100);
        t.addCell(sem(TextosRequerimento.HORAS_INTEGRALIZADAS, fonte(10, Font.NORMAL), Element.ALIGN_RIGHT));
        PdfPCell total = sem(TextosRequerimento.horas(r.getTotalHoras()) + " h", fonte(16, Font.BOLD), Element.ALIGN_LEFT);
        total.setBorder(Rectangle.BOTTOM);
        total.setBorderWidthBottom(1.2f);
        t.addCell(total);
        t.addCell(sem("", fonte(10, Font.NORMAL), Element.ALIGN_LEFT));
        PdfPCell nome = sem(r.getCoordenadorNome().orElse(" "), fonte(10, Font.NORMAL), Element.ALIGN_CENTER);
        nome.setRowspan(2);
        nome.setVerticalAlignment(Element.ALIGN_BOTTOM);
        nome.setBorder(Rectangle.BOTTOM);
        nome.setBorderWidthBottom(1.2f);
        nome.setPaddingBottom(4);
        t.addCell(nome);

        t.addCell(sem(TextosRequerimento.DEFERIDO_EM, fonte(10, Font.NORMAL), Element.ALIGN_RIGHT));
        PdfPCell data = sem(r.getDataDeferimento().map(d -> d.format(TextosRequerimento.DATA)).orElse(" "), fonte(10, Font.BOLD),
                Element.ALIGN_LEFT);
        if (r.getDataDeferimento().isEmpty()) {
            // Em branco, com linha, para a coordenação preencher.
            data.setBorder(Rectangle.BOTTOM);
            data.setBorderWidthBottom(0.8f);
        }
        t.addCell(data);
        t.addCell(sem("", fonte(10, Font.NORMAL), Element.ALIGN_LEFT));

        t.addCell(sem("", fonte(10, Font.NORMAL), Element.ALIGN_LEFT));
        t.addCell(sem("", fonte(10, Font.NORMAL), Element.ALIGN_LEFT));
        t.addCell(sem("", fonte(10, Font.NORMAL), Element.ALIGN_LEFT));
        t.addCell(sem(TextosRequerimento.COORDENADOR, fonte(10, Font.NORMAL), Element.ALIGN_CENTER));
        t.setSpacingAfter(14);
        return t;
    }

    private static PdfPTable dadosDoAluno(Requerimento r) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] {26, 74});
        t.setWidthPercentage(100);
        String[][] dados = {
            {TextosRequerimento.NOME, r.getNomeAluno()},
            {TextosRequerimento.MATRICULA, r.getMatricula()},
            {TextosRequerimento.RG, ""},
            {TextosRequerimento.CPF, ""},
        };
        for (String[] linha : dados) {
            PdfPCell rotulo = sem(linha[0], fonte(9, Font.NORMAL), Element.ALIGN_RIGHT);
            rotulo.setPaddingRight(6);
            t.addCell(rotulo);
            PdfPCell valor = sem(linha[1], fonte(10, Font.BOLD), Element.ALIGN_LEFT);
            valor.setBorder(Rectangle.BOTTOM);
            valor.setBorderWidthBottom(0.8f);
            valor.setMinimumHeight(18);
            t.addCell(valor);
        }
        return t;
    }

    private static PdfPTable tabela(Requerimento r) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] {6, 26, 36, 8, 6, 18});
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        for (String titulo : TextosRequerimento.CABECALHO) {
            PdfPCell c = celula(titulo, fonte(7.5f, Font.BOLD), Element.ALIGN_CENTER);
            c.setBackgroundColor(FAIXA);
            t.addCell(c);
        }
        String macroAtual = null;
        for (Requerimento.Linha l : r.getLinhas()) {
            if (!l.getMacroatividade().equals(macroAtual)) {
                macroAtual = l.getMacroatividade();
                PdfPCell faixa = celula(macroAtual.toUpperCase(Locale.ROOT), fonte(8.5f, Font.BOLD), Element.ALIGN_LEFT);
                faixa.setColspan(6);
                faixa.setBackgroundColor(FAIXA);
                faixa.setPadding(5);
                t.addCell(faixa);
            }
            Font normal = fonte(8, Font.NORMAL);
            t.addCell(celula(String.valueOf(l.getItem()), fonte(8, Font.BOLD), Element.ALIGN_CENTER));
            t.addCell(celula(l.getNome(), fonte(8, Font.BOLD), Element.ALIGN_LEFT));
            t.addCell(celula(l.getDescricao() == null ? "" : l.getDescricao(), normal, Element.ALIGN_LEFT));
            t.addCell(celula(TextosRequerimento.horas(l.getHorasPorOcorrencia()), normal, Element.ALIGN_CENTER));
            boolean temHoras = l.getQuantidade() > 0;
            t.addCell(celula(temHoras ? String.valueOf(l.getQuantidade()) : "", normal, Element.ALIGN_CENTER));
            t.addCell(celula(temHoras ? TextosRequerimento.horas(l.getHorasIntegralizadas()) : "", fonte(8, Font.BOLD), Element.ALIGN_CENTER));
        }
        PdfPCell rotulo = celula(TextosRequerimento.TOTAL, fonte(10, Font.BOLD), Element.ALIGN_RIGHT);
        rotulo.setColspan(5);
        rotulo.setPadding(6);
        t.addCell(rotulo);
        PdfPCell total = celula(TextosRequerimento.horas(r.getTotalHoras()), fonte(12, Font.BOLD), Element.ALIGN_CENTER);
        total.setPadding(6);
        t.addCell(total);
        return t;
    }

    private static Paragraph centralizado(String texto, Font fonte, float espacoDepois) {
        Paragraph p = new Paragraph(texto, fonte);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(espacoDepois);
        return p;
    }

    private static PdfPCell celula(String texto, Font fonte, int alinhamento) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte));
        c.setHorizontalAlignment(alinhamento);
        c.setVerticalAlignment(alinhamento == Element.ALIGN_LEFT ? Element.ALIGN_TOP : Element.ALIGN_MIDDLE);
        c.setBorderColor(BORDA);
        c.setBorderWidth(0.6f);
        c.setPadding(3.5f);
        return c;
    }

    private static PdfPCell sem(String texto, Font fonte, int alinhamento) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte));
        c.setHorizontalAlignment(alinhamento);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(2);
        return c;
    }

    private static Font fonte(float tamanho, int estilo) {
        return fonte(tamanho, estilo, Color.BLACK);
    }

    /** Helvetica com codificação CP1252: cobre todos os acentos do português. */
    private static Font fonte(float tamanho, int estilo, Color cor) {
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED, tamanho, estilo, cor);
    }
}
