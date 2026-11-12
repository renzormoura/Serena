package br.edu.univassouras.sereno.certificados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class PainelIntegracaoTest extends CoordenacaoBase {

    private String painel(Cookie coord) throws Exception {
        return corpo(mvc.get().uri("/api/v1/coordenacao/painel").cookie(coord).exchange());
    }

    @Test
    void painelAcompanhaFilaEDecisoesEExportaParaXlsx() throws Exception {
        Cookie coord = coordenacao();
        String antes = painel(coord);

        Conta aluno = novoAluno();
        Lancado a = lancar(aluno.sessao(), 18, "60");
        lancar(aluno.sessao(), 21, "4");
        decidir(coord, a.id(), a.versao(), "\"decisao\":\"DEFERIR\"");

        String depois = painel(coord);
        assertThat(JsonPath.<Integer>read(depois, "$.alunos")).isEqualTo(JsonPath.<Integer>read(antes, "$.alunos") + 1);
        assertThat(JsonPath.<Integer>read(depois, "$.aguardandoAnalise"))
                .isEqualTo(JsonPath.<Integer>read(antes, "$.aguardandoAnalise") + 1);
        assertThat(JsonPath.<Double>read(depois, "$.diasMedioAnalise")).isNotNull();
        assertThat(JsonPath.<String>read(depois, "$.modoFaixas")).isEqualTo("HORAS");
        assertThat(JsonPath.<List<String>>read(depois, "$.filtros.cursos[*].nome")).contains("Engenharia Civil");

        // Filtro por ano de ingresso: os alunos de teste entram em 2022; 1990 não tem ninguém.
        assertThat(mvc.get().uri("/api/v1/coordenacao/painel?ingresso=1990").cookie(coord))
                .bodyJson().extractingPath("$.alunos").isEqualTo(0);

        var xlsx = mvc.get().uri("/api/v1/coordenacao/painel/xlsx").cookie(coord).exchange();
        assertThat(xlsx).hasStatusOk();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx.getResponse().getContentAsByteArray()))) {
            Sheet alunos = wb.getSheet("Alunos");
            assertThat(alunos.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Matrícula");
            List<String> matriculas = new ArrayList<>();
            for (Row r : alunos) {
                if (r.getRowNum() > 0) {
                    matriculas.add(r.getCell(2).getStringCellValue());
                }
            }
            assertThat(matriculas).contains(aluno.matricula());
            assertThat(wb.getSheet("Em atenção")).isNotNull();
        }
    }

    @Test
    void comTotalExigidoDefinidoPeloAdminOPainelPassaAPercentual() throws Exception {
        Cookie admin = entrar("admin@dev.local", SENHA_DEV);
        Cookie coord = coordenacao();
        assertThat(mvc.put().uri("/api/v1/admin/regulamentos/vigente/horas-exigidas").with(csrf()).cookie(coord)
                .contentType(MediaType.APPLICATION_JSON).content("{\"horasExigidas\":200}"))
                .hasStatus(HttpStatus.FORBIDDEN);
        try {
            assertThat(mvc.put().uri("/api/v1/admin/regulamentos/vigente/horas-exigidas").with(csrf()).cookie(admin)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"horasExigidas\":200}"))
                    .hasStatusOk().bodyJson().extractingPath("$.horasExigidas").isEqualTo(200);

            String p = painel(coord);
            assertThat(JsonPath.<String>read(p, "$.modoFaixas")).isEqualTo("PERCENTUAL");
            assertThat(JsonPath.<Boolean>read(p, "$.atencaoDisponivel")).isTrue();
            assertThat(JsonPath.<Number>read(p, "$.exigidas").intValue()).isEqualTo(200);
        } finally {
            // O total vale para todo o contexto de teste: volta a "a definir".
            assertThat(mvc.put().uri("/api/v1/admin/regulamentos/vigente/horas-exigidas").with(csrf()).cookie(admin)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"horasExigidas\":null}"))
                    .hasStatusOk();
        }
    }

    @Test
    void alunoNaoVeOPainel() {
        assertThat(mvc.get().uri("/api/v1/coordenacao/painel").cookie(novoAluno().sessao())).hasStatus(HttpStatus.FORBIDDEN);
    }
}
