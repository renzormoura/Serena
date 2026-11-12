package br.edu.univassouras.sereno.requerimento;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Requerimento em XLSX, no mesmo layout da aba INTEGRALIZAÇÃO da planilha modelo: mesmas colunas, larguras,
 * células mescladas, fontes e bordas. Os valores vêm prontos (sem fórmulas): é a fotografia do deferimento.
 */
final class GeradorXlsx {

    /** Larguras das colunas A–G na planilha modelo, em caracteres. */
    private static final double[] LARGURAS = {10.4, 8.9, 46.8, 38.1, 12.4, 5.2, 30.1};
    private static final int PRIMEIRA_LINHA_ITENS = 14;

    private GeradorXlsx() {
    }

    static byte[] gerar(Requerimento r) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Sheet s = wb.createSheet("INTEGRALIZAÇÃO");
            for (int i = 0; i < LARGURAS.length; i++) {
                s.setColumnWidth(i, (int) Math.round(LARGURAS[i] * 256));
            }
            Estilos e = new Estilos(wb);

            texto(s, 0, 54, 0, 6, "", e.titulo(20));
            logo(wb, s);
            texto(s, 1, 28.8, 0, 6, TextosRequerimento.PRO_REITORIA, e.titulo(20));
            texto(s, 2, 23.2, 0, 6, TextosRequerimento.curso(r.getCurso()), e.titulo(16));

            texto(s, 3, 39.6, 0, 1, TextosRequerimento.HORAS_INTEGRALIZADAS, e.rotulo(12));
            numero(linha(s, 3, 39.6), 2, r.getTotalHoras(), e.preenchido(24));
            texto(s, 4, 19.1, 0, 1, TextosRequerimento.DEFERIDO_EM, e.rotulo(11));
            celula(linha(s, 4, 19.1), 2, r.getDataDeferimento().map(d -> d.format(TextosRequerimento.DATA)).orElse(""), e.preenchido(12));

            // Espaço da assinatura (E4:G5), com o nome de quem deferiu acima da linha.
            for (int c = 4; c <= 6; c++) {
                celula(s.getRow(3), c, c == 4 ? r.getCoordenadorNome().orElse("") : "", e.assinatura());
                celula(s.getRow(4), c, "", e.assinatura());
            }
            s.addMergedRegion(new CellRangeAddress(3, 4, 4, 6));

            texto(s, 5, 21.15, 4, 6, TextosRequerimento.COORDENADOR, e.linhaDeAssinatura());
            RegionUtil.setBorderTop(BorderStyle.THICK, new CellRangeAddress(5, 5, 4, 6), s);

            texto(s, 6, 17.6, 0, 4, TextosRequerimento.destinatario(r.getCurso()), e.titulo(12));

            String[][] dados = {
                {TextosRequerimento.NOME, r.getNomeAluno()},
                {TextosRequerimento.MATRICULA, r.getMatricula()},
                {TextosRequerimento.RG, ""},
                {TextosRequerimento.CPF, ""},
            };
            for (int i = 0; i < dados.length; i++) {
                int n = 7 + i;
                texto(s, n, 22.15, 0, 1, dados[i][0], e.rotulo(11));
                texto(s, n, 22.15, 2, 6, dados[i][1], e.preenchido(11));
                RegionUtil.setBorderBottom(BorderStyle.THICK, new CellRangeAddress(n, n, 2, 6), s);
            }

            texto(s, 11, 34.9, 0, 6, TextosRequerimento.PEDIDO, e.paragrafo());

            Row cabecalho = linha(s, 13, 18.5);
            for (int i = 0; i < TextosRequerimento.CABECALHO.length; i++) {
                celula(cabecalho, i + 1, TextosRequerimento.CABECALHO[i], e.cabecalho());
            }

            List<Requerimento.Linha> linhas = r.getLinhas();
            int n = PRIMEIRA_LINHA_ITENS;
            int inicioGrupo = n;
            for (int i = 0; i < linhas.size(); i++) {
                Requerimento.Linha l = linhas.get(i);
                Row row = linha(s, n, altura(l));
                celula(row, 0, "", e.tabela(false, HorizontalAlignment.CENTER));
                celula(row, 1, String.valueOf(l.getItem()), e.tabela(true, HorizontalAlignment.CENTER));
                celula(row, 2, l.getNome(), e.tabela(true, HorizontalAlignment.LEFT));
                celula(row, 3, l.getDescricao() == null ? "" : l.getDescricao(), e.tabela(false, HorizontalAlignment.LEFT));
                numero(row, 4, l.getHorasPorOcorrencia(), e.tabela(false, HorizontalAlignment.CENTER));
                if (l.getQuantidade() > 0) {
                    numero(row, 5, BigDecimal.valueOf(l.getQuantidade()), e.tabela(false, HorizontalAlignment.CENTER));
                    numero(row, 6, l.getHorasIntegralizadas(), e.tabela(false, HorizontalAlignment.CENTER));
                } else {
                    celula(row, 5, "", e.tabela(false, HorizontalAlignment.CENTER));
                    celula(row, 6, "", e.tabela(false, HorizontalAlignment.CENTER));
                }
                boolean fimDoGrupo = i == linhas.size() - 1 || !linhas.get(i + 1).getMacroatividade().equals(l.getMacroatividade());
                if (fimDoGrupo) {
                    Cell macro = s.getRow(inicioGrupo).getCell(0);
                    macro.setCellValue(l.getMacroatividade().toUpperCase(Locale.ROOT));
                    macro.setCellStyle(e.macro());
                    if (n > inicioGrupo) {
                        CellRangeAddress grupo = new CellRangeAddress(inicioGrupo, n, 0, 0);
                        s.addMergedRegion(grupo);
                        RegionUtil.setBorderTop(BorderStyle.THICK, grupo, s);
                        RegionUtil.setBorderBottom(BorderStyle.THICK, grupo, s);
                        RegionUtil.setBorderLeft(BorderStyle.THICK, grupo, s);
                        RegionUtil.setBorderRight(BorderStyle.THICK, grupo, s);
                    }
                    inicioGrupo = n + 1;
                }
                n++;
            }

            Row total = linha(s, n, 41.45);
            for (int c = 0; c <= 5; c++) {
                celula(total, c, c == 0 ? TextosRequerimento.TOTAL : "", e.total(HorizontalAlignment.RIGHT));
            }
            numero(total, 6, r.getTotalHoras(), e.total(HorizontalAlignment.CENTER));
            s.addMergedRegion(new CellRangeAddress(n, n, 0, 5));

            PrintSetup impressao = s.getPrintSetup();
            impressao.setPaperSize(PrintSetup.A4_PAPERSIZE);
            impressao.setLandscape(false);
            impressao.setFitWidth((short) 1);
            impressao.setFitHeight((short) 0);
            s.setFitToPage(true);
            s.setMargin(Sheet.LeftMargin, 0.2);
            s.setMargin(Sheet.RightMargin, 0.2);
            s.setMargin(Sheet.TopMargin, 0.4);
            s.setMargin(Sheet.BottomMargin, 0.4);
            wb.setPrintArea(0, 0, 6, 0, n);
            s.setRepeatingRows(new CellRangeAddress(13, 13, 0, 6));

            wb.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Logo da universidade centralizada na linha 1, como na planilha modelo. */
    private static void logo(Workbook wb, Sheet s) {
        int indice = wb.addPicture(TextosRequerimento.LOGO, Workbook.PICTURE_TYPE_PNG);
        org.apache.poi.ss.usermodel.ClientAnchor ancora = wb.getCreationHelper().createClientAnchor();
        // Coluna C (~333 px de largura) e linha 1 (54 pt = 72 px): a logo (438×113) reduzida a 254×66, centralizada.
        ancora.setCol1(2);
        ancora.setRow1(0);
        ancora.setDx1(Units.pixelToEMU(40));
        ancora.setDy1(Units.pixelToEMU(3));
        ancora.setCol2(2);
        ancora.setRow2(0);
        ancora.setDx2(Units.pixelToEMU(294));
        ancora.setDy2(Units.pixelToEMU(69));
        ancora.setAnchorType(org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
        s.createDrawingPatriarch().createPicture(ancora, indice);
    }

    /** Altura aproximada para o texto da descrição caber, como na planilha modelo. */
    private static double altura(Requerimento.Linha l) {
        int linhasDescricao = l.getDescricao() == null ? 1 : (int) Math.ceil(l.getDescricao().length() / 42.0);
        int linhasNome = (int) Math.ceil(l.getNome().length() / 48.0);
        return Math.max(19.5, Math.max(linhasDescricao, linhasNome) * 17.0 + 3.5);
    }

    private static Row linha(Sheet s, int indice, double altura) {
        Row row = s.getRow(indice);
        if (row == null) {
            row = s.createRow(indice);
        }
        row.setHeightInPoints((float) altura);
        return row;
    }

    private static void texto(Sheet s, int linha, double altura, int de, int ate, String valor, CellStyle estilo) {
        Row row = linha(s, linha, altura);
        for (int c = de; c <= ate; c++) {
            celula(row, c, c == de ? valor : "", estilo);
        }
        if (ate > de) {
            s.addMergedRegion(new CellRangeAddress(linha, linha, de, ate));
        }
    }

    private static void celula(Row row, int coluna, String valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor);
        cell.setCellStyle(estilo);
    }

    private static void numero(Row row, int coluna, BigDecimal valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor.doubleValue());
        cell.setCellStyle(estilo);
    }

    /** Estilos reaproveitados (o XLSX tem limite de estilos por arquivo). */
    private static final class Estilos {

        private final Workbook wb;
        private final java.util.Map<String, CellStyle> cache = new java.util.HashMap<>();

        Estilos(Workbook wb) {
            this.wb = wb;
        }

        CellStyle titulo(int tamanho) {
            return estilo("titulo" + tamanho, "Calibri", tamanho, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
        }

        CellStyle rotulo(int tamanho) {
            return estilo("rotulo" + tamanho, "Calibri", tamanho, false, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER, false);
        }

        CellStyle preenchido(int tamanho) {
            CellStyle e = estilo("preenchido" + tamanho, "Calibri", tamanho, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false);
            e.setBorderBottom(BorderStyle.THICK);
            return e;
        }

        CellStyle assinatura() {
            return estilo("assinatura", "Calibri", 12, false, HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM, true);
        }

        CellStyle linhaDeAssinatura() {
            CellStyle e = estilo("linhaAssinatura", "Calibri", 14, false, HorizontalAlignment.CENTER, VerticalAlignment.TOP, false);
            e.setBorderTop(BorderStyle.THICK);
            return e;
        }

        CellStyle paragrafo() {
            return estilo("paragrafo", "Calibri", 11, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true);
        }

        CellStyle cabecalho() {
            return bordas(estilo("cabecalho", "Times New Roman", 11, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true));
        }

        CellStyle tabela(boolean negrito, HorizontalAlignment horizontal) {
            VerticalAlignment vertical = horizontal == HorizontalAlignment.LEFT ? VerticalAlignment.TOP : VerticalAlignment.CENTER;
            return bordas(estilo("tabela" + negrito + horizontal, "Times New Roman", 12, negrito, horizontal, vertical, true));
        }

        CellStyle macro() {
            CellStyle e = bordas(estilo("macro", "Times New Roman", 16, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true));
            e.setRotation((short) 90);
            return e;
        }

        CellStyle total(HorizontalAlignment horizontal) {
            return bordas(estilo("total" + horizontal, "Calibri", 24, false, horizontal, VerticalAlignment.CENTER, false));
        }

        private CellStyle estilo(String chave, String fonte, int tamanho, boolean negrito, HorizontalAlignment horizontal,
                VerticalAlignment vertical, boolean quebrar) {
            return cache.computeIfAbsent(chave, k -> {
                Font f = wb.createFont();
                f.setFontName(fonte);
                f.setFontHeightInPoints((short) tamanho);
                f.setBold(negrito);
                CellStyle e = wb.createCellStyle();
                e.setFont(f);
                e.setAlignment(horizontal);
                e.setVerticalAlignment(vertical);
                e.setWrapText(quebrar);
                return e;
            });
        }

        private static CellStyle bordas(CellStyle e) {
            e.setBorderTop(BorderStyle.THICK);
            e.setBorderBottom(BorderStyle.THICK);
            e.setBorderLeft(BorderStyle.THICK);
            e.setBorderRight(BorderStyle.THICK);
            return e;
        }
    }
}
