package br.edu.univassouras.sereno.certificados;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Exporta o painel em XLSX: uma aba por visão, com filtro e cabeçalho fixo, prontas para a coordenação trabalhar. */
final class ExportacaoPainel {

    private ExportacaoPainel() {
    }

    static byte[] xlsx(PainelService.Dados dados) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            CellStyle negrito = wb.createCellStyle();
            Font fonte = wb.createFont();
            fonte.setBold(true);
            negrito.setFont(fonte);

            Sheet alunos = aba(wb, negrito, "Alunos", "Nome", "E-mail", "Matrícula", "Curso", "Ingresso", "Período",
                    "Integralizadas (h)", "Em análise (h)", "Exigidas (h)", "% concluído", "Para analisar", "Em ajuste");
            int n = 1;
            for (PainelService.LinhaAluno l : dados.alunos()) {
                Row r = alunos.createRow(n++);
                texto(r, 0, l.aluno().nome());
                texto(r, 1, l.aluno().email());
                texto(r, 2, l.aluno().aluno().matricula());
                texto(r, 3, l.aluno().aluno().curso());
                if (l.aluno().aluno().anoIngresso() != null) {
                    texto(r, 4, l.aluno().aluno().anoIngresso() + "." + l.aluno().aluno().semestreIngresso());
                }
                l.periodo().ifPresent(periodo -> numero(r, 5, periodo));
                numero(r, 6, l.saldo().integralizadas());
                numero(r, 7, l.saldo().pendentes());
                l.saldo().exigidas().ifPresent(e -> numero(r, 8, e));
                l.saldo().percentual().ifPresent(p -> numero(r, 9, p));
                numero(r, 10, l.paraAnalisar());
                numero(r, 11, l.emAjuste());
            }
            ajustar(alunos, 12, n);

            PainelDto p = dados.painel();
            Sheet atencao = aba(wb, negrito, "Em atenção", "Nome", "Matrícula", "Curso", "Período", "Integralizadas (h)", "% concluído");
            n = 1;
            for (PainelDto.AlunoEmAtencao a : p.emAtencao()) {
                Row r = atencao.createRow(n++);
                texto(r, 0, a.nome());
                texto(r, 1, a.matricula());
                texto(r, 2, a.curso());
                numero(r, 3, a.periodo());
                numero(r, 4, a.integralizadas());
                numero(r, 5, a.percentual());
            }
            if (!p.atencaoDisponivel()) {
                texto(atencao.createRow(1), 0, "Defina o total de horas exigidas no regulamento para calcular esta lista.");
                n = 2;
            }
            ajustar(atencao, 6, n);

            Sheet itens = aba(wb, negrito, "Decisões por item", "Item", "Nome", "Validados", "Reprovados", "Ajustes pedidos");
            n = 1;
            for (PainelDto.DecisoesPorItem i : p.decisoesPorItem()) {
                Row r = itens.createRow(n++);
                numero(r, 0, i.numero());
                texto(r, 1, i.nome());
                numero(r, 2, i.deferidos());
                numero(r, 3, i.indeferidos());
                numero(r, 4, i.ajustes());
            }
            ajustar(itens, 5, n);

            Sheet faixas = aba(wb, negrito, "Faixas", p.modoFaixas().equals("PERCENTUAL") ? "Faixa (% exigido)" : "Faixa (horas)", "Alunos");
            n = 1;
            for (PainelDto.Faixa f : p.faixas()) {
                Row r = faixas.createRow(n++);
                texto(r, 0, f.rotulo());
                numero(r, 1, f.alunos());
            }
            ajustar(faixas, 2, n);

            wb.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Sheet aba(Workbook wb, CellStyle negrito, String nome, String... colunas) {
        Sheet s = wb.createSheet(nome);
        Row cabecalho = s.createRow(0);
        for (int i = 0; i < colunas.length; i++) {
            Cell c = cabecalho.createCell(i);
            c.setCellValue(colunas[i]);
            c.setCellStyle(negrito);
        }
        s.createFreezePane(0, 1);
        return s;
    }

    private static void ajustar(Sheet s, int colunas, int linhas) {
        if (linhas > 1) {
            s.setAutoFilter(new CellRangeAddress(0, linhas - 1, 0, colunas - 1));
        }
        for (int i = 0; i < colunas; i++) {
            s.autoSizeColumn(i);
            s.setColumnWidth(i, Math.min(s.getColumnWidth(i) + 512, 60 * 256));
        }
    }

    private static void texto(Row r, int coluna, String valor) {
        r.createCell(coluna).setCellValue(valor);
    }

    private static void numero(Row r, int coluna, Number valor) {
        r.createCell(coluna).setCellValue(valor instanceof BigDecimal b ? b.doubleValue() : valor.doubleValue());
    }
}
