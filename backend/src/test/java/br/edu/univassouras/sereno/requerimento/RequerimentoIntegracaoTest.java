package br.edu.univassouras.sereno.requerimento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import br.edu.univassouras.sereno.IntegracaoBase;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class RequerimentoIntegracaoTest extends IntegracaoBase {

    private String lancarEDeferir(Cookie aluno, Cookie coord, int item, String horas) throws Exception {
        String marca = UUID.randomUUID().toString();
        var lancado = mvc.post().uri("/api/v1/certificados").multipart()
                .file(new MockMultipartFile("arquivo", "c.pdf", "application/pdf",
                        ("%PDF-1.4\n% " + marca + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII)))
                .file(new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE, """
                        {"titulo":"Atividade","horasCertificado":%s,"itemNumero":%d}""".formatted(horas, item)
                        .getBytes(StandardCharsets.UTF_8)))
                .with(csrf()).cookie(aluno)
                .exchange();
        String corpo = lancado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(corpo, "$.id");
        assertThat(mvc.post().uri("/api/v1/coordenacao/certificados/" + id + "/decisao").with(csrf()).cookie(coord)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"versao\":%d,\"decisao\":\"DEFERIR\"}".formatted(JsonPath.<Integer>read(corpo, "$.versao"))))
                .hasStatusOk();
        return id;
    }

    private String alunoId(Cookie coord, String matricula) throws Exception {
        String lista = mvc.get().uri("/api/v1/coordenacao/alunos").cookie(coord).exchange()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.<List<String>>read(lista, "$[?(@.matricula == '" + matricula + "')].id").getFirst();
    }

    @Test
    void geraORequerimentoOficialEmXlsxEPdfComAsHorasDeferidas() throws Exception {
        Conta aluno = novoAluno();
        Cookie coord = entrar("coordenacao@dev.local", "sereno-dev");
        // Como na planilha: três cursos livres (10), uma monitoria (18) e uma palestra (22).
        lancarEDeferir(aluno.sessao(), coord, 10, "70");
        lancarEDeferir(aluno.sessao(), coord, 10, "32");
        lancarEDeferir(aluno.sessao(), coord, 18, "50");
        lancarEDeferir(aluno.sessao(), coord, 22, "2");
        String alunoId = alunoId(coord, aluno.matricula());

        var gerado = mvc.post().uri("/api/v1/coordenacao/alunos/" + alunoId + "/requerimentos").with(csrf()).cookie(coord).exchange();

        assertThat(gerado).hasStatus(HttpStatus.CREATED);
        String corpo = gerado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<Integer>read(corpo, "$.numero")).isEqualTo(1);
        assertThat(JsonPath.<Double>read(corpo, "$.totalHoras")).isEqualTo(95.0);
        assertThat(JsonPath.<String>read(corpo, "$.coordenador")).isEqualTo("Coordenação");
        String id = JsonPath.read(corpo, "$.id");
        String hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // XLSX no layout da aba INTEGRALIZAÇÃO: item 1 na linha 15, item 52 na 66, total na G67.
        var xlsx = mvc.get().uri("/api/v1/coordenacao/requerimentos/" + id + "/xlsx").cookie(coord).exchange();
        assertThat(xlsx).hasStatusOk();
        assertThat(xlsx.getResponse().getHeader("Content-Disposition")).contains("requerimento-" + aluno.matricula() + "-1.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx.getResponse().getContentAsByteArray()))) {
            Sheet s = wb.getSheet("INTEGRALIZAÇÃO");
            assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("PRÓ-REITORIA DE CIÊNCIAS TECNOLÓGICAS E EXATAS");
            assertThat(s.getRow(3).getCell(2).getNumericCellValue()).isEqualTo(95.0);
            assertThat(s.getRow(4).getCell(2).getStringCellValue()).isEqualTo(hoje);
            assertThat(s.getRow(8).getCell(2).getStringCellValue()).isEqualTo(aluno.matricula());
            assertThat(s.getRow(13).getCell(6).getStringCellValue()).isEqualTo("HORAS INTEGRALIZADAS");
            assertThat(s.getRow(14).getCell(1).getStringCellValue()).isEqualTo("1");
            assertThat(s.getRow(14).getCell(0).getStringCellValue()).isEqualTo("CIDADANIA");
            assertThat(s.getRow(23).getCell(1).getStringCellValue()).isEqualTo("10");
            assertThat(s.getRow(23).getCell(5).getNumericCellValue()).isEqualTo(2.0);
            assertThat(s.getRow(23).getCell(6).getNumericCellValue()).isEqualTo(40.0);
            assertThat(s.getRow(31).getCell(6).getNumericCellValue()).isEqualTo(50.0);
            assertThat(s.getRow(65).getCell(1).getStringCellValue()).isEqualTo("52");
            assertThat(s.getRow(66).getCell(0).getStringCellValue()).isEqualTo("TOTAL DE HORAS INTEGRALIZADAS NOS ITENS ACIMA");
            assertThat(s.getRow(66).getCell(6).getNumericCellValue()).isEqualTo(95.0);
        }

        var pdf = mvc.get().uri("/api/v1/coordenacao/requerimentos/" + id + "/pdf").cookie(coord).exchange();
        assertThat(pdf).hasStatusOk().hasContentType(MediaType.APPLICATION_PDF);
        try (PDDocument doc = Loader.loadPDF(pdf.getResponse().getContentAsByteArray())) {
            String texto = new PDFTextStripper().getText(doc);
            assertThat(texto).contains("CURSO DE ENGENHARIA", aluno.matricula(), "Coordenação", hoje, "Monitoria");
        }

        // A lista guarda o histórico: gerar de novo cria o nº 2.
        mvc.post().uri("/api/v1/coordenacao/alunos/" + alunoId + "/requerimentos").with(csrf()).cookie(coord).exchange();
        assertThat(mvc.get().uri("/api/v1/coordenacao/alunos/" + alunoId + "/requerimentos").cookie(coord))
                .bodyJson().extractingPath("$[*].numero").isEqualTo(List.of(2, 1));
    }

    @Test
    void alunoGeraOProprioRequerimentoSemDeferimentoPreenchido() throws Exception {
        Conta aluno = novoAluno();
        assertThat(mvc.post().uri("/api/v1/alunos/me/requerimentos").with(csrf()).cookie(aluno.sessao()))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT).bodyJson().extractingPath("$.codigo").isEqualTo("sem-horas");

        Cookie coord = entrar("coordenacao@dev.local", "sereno-dev");
        lancarEDeferir(aluno.sessao(), coord, 18, "50");

        var gerado = mvc.post().uri("/api/v1/alunos/me/requerimentos").with(csrf()).cookie(aluno.sessao()).exchange();
        assertThat(gerado).hasStatus(HttpStatus.CREATED);
        String corpo = gerado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<Boolean>read(corpo, "$.geradoPeloAluno")).isTrue();
        assertThat(JsonPath.<Object>read(corpo, "$.coordenador")).isNull();
        assertThat(JsonPath.<Object>read(corpo, "$.dataDeferimento")).isNull();
        assertThat(JsonPath.<Double>read(corpo, "$.totalHoras")).isEqualTo(50.0);
        String id = JsonPath.read(corpo, "$.id");

        var xlsx = mvc.get().uri("/api/v1/alunos/me/requerimentos/" + id + "/xlsx").cookie(aluno.sessao()).exchange();
        assertThat(xlsx).hasStatusOk();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx.getResponse().getContentAsByteArray()))) {
            Sheet s = wb.getSheet("INTEGRALIZAÇÃO");
            assertThat(s.getRow(3).getCell(2).getNumericCellValue()).isEqualTo(50.0);
            assertThat(s.getRow(4).getCell(2).getStringCellValue()).isEmpty();
            assertThat(s.getRow(3).getCell(4).getStringCellValue()).isEmpty();
        }
        assertThat(mvc.get().uri("/api/v1/alunos/me/requerimentos/" + id + "/pdf").cookie(aluno.sessao())).hasStatusOk();

        // Outro aluno não baixa; a coordenação vê o requerimento na ficha.
        assertThat(mvc.get().uri("/api/v1/alunos/me/requerimentos/" + id + "/pdf").cookie(novoAluno().sessao()))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/v1/coordenacao/alunos/" + alunoId(coord, aluno.matricula()) + "/requerimentos").cookie(coord))
                .bodyJson().extractingPath("$[0].geradoPeloAluno").isEqualTo(true);
    }

    @Test
    void semCertificadoDeferidoNaoGera() throws Exception {
        Conta aluno = novoAluno();
        Cookie coord = entrar("coordenacao@dev.local", "sereno-dev");
        String alunoId = alunoId(coord, aluno.matricula());

        assertThat(mvc.post().uri("/api/v1/coordenacao/alunos/" + alunoId + "/requerimentos").with(csrf()).cookie(coord))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("sem-horas");
    }
}
