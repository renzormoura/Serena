package br.edu.univassouras.sereno.classificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import br.edu.univassouras.sereno.IntegracaoBase;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class SugestaoNoFluxoIntegracaoTest extends IntegracaoBase {

    private static byte[] pdf(String linha) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            try (PDPageContentStream c = new PDPageContentStream(doc, pagina)) {
                c.beginText();
                c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                c.newLineAtOffset(40, 700);
                c.showText(linha);
                c.endText();
            }
            doc.save(saida);
            return saida.toByteArray();
        }
    }

    /** Envia, espera a leitura e devolve o certificado já pronto para conferência. */
    private String enviarELer(Cookie sessao, String linha) throws Exception {
        var envio = mvc.post().uri("/api/v1/certificados/envios").multipart()
                .file(new MockMultipartFile("arquivos", "c.pdf", "application/pdf", pdf(linha)))
                .with(csrf()).cookie(sessao).exchange();
        String id = JsonPath.read(envio.getResponse().getContentAsString(StandardCharsets.UTF_8), "$[0].certificado.id");
        String[] corpo = new String[1];
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(200)).until(() -> {
            corpo[0] = mvc.get().uri("/api/v1/certificados/" + id).cookie(sessao).exchange()
                    .getResponse().getContentAsString(StandardCharsets.UTF_8);
            return !"PROCESSANDO".equals(JsonPath.read(corpo[0], "$.status"));
        });
        return corpo[0];
    }

    private void confirmar(Cookie sessao, String certificado, int item) {
        String id = JsonPath.read(certificado, "$.id");
        assertThat(mvc.put().uri("/api/v1/certificados/" + id).with(csrf()).cookie(sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"versao":%d,"dados":{"titulo":"Atividade","horasCertificado":4,"itemNumero":%d}}"""
                        .formatted(JsonPath.<Integer>read(certificado, "$.versao"), item)))
                .hasStatusOk();
    }

    @Test
    void leituraJaSugereEPreencheOItemComAlternativas() throws Exception {
        Conta aluno = novoAluno();

        String lido = enviarELer(aluno.sessao(),
                "Universidade de Vassouras. Certificamos que o aluno atuou como monitor da disciplina Calculo I, 60 horas.");

        assertThat(JsonPath.<Integer>read(lido, "$.item.numero")).isEqualTo(18);
        assertThat(JsonPath.<Integer>read(lido, "$.sugestaoItem.item.numero")).isEqualTo(18);
        assertThat(JsonPath.<String>read(lido, "$.sugestaoItem.confianca")).isEqualTo("ALTA");
        assertThat(JsonPath.<String>read(lido, "$.sugestaoItem.motivo")).contains("monitor");
        assertThat(JsonPath.<String>read(lido, "$.origemClassificacao")).isEqualTo("REGRA");
    }

    @Test
    void confiancaBaixaNaoPreencheOItemMasMostraAsOpcoes() throws Exception {
        Conta aluno = novoAluno();

        String lido = enviarELer(aluno.sessao(), "Certificamos que o aluno participou da palestra Pontes de Concreto, 2 horas.");

        assertThat(JsonPath.<Object>read(lido, "$.item")).isNull();
        assertThat(JsonPath.<String>read(lido, "$.sugestaoItem.confianca")).isEqualTo("BAIXA");
        assertThat(JsonPath.<java.util.List<Object>>read(lido, "$.sugestaoItem.alternativas")).isNotEmpty();
    }

    @Test
    void acuraciaContaAcertosETrocasDosAlunos() throws Exception {
        Conta aluno = novoAluno();
        Cookie coordenacao = entrar("coordenacao@dev.local", "sereno-dev");
        var antes = mvc.get().uri("/api/v1/admin/classificacao/acuracia").cookie(coordenacao).exchange();
        int avaliadosAntes = JsonPath.read(antes.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.avaliados");
        int acertosAntes = JsonPath.read(antes.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.acertos");

        // Aceitou a sugestão (18) num certificado e trocou (39 → 50) no outro.
        confirmar(aluno.sessao(), enviarELer(aluno.sessao(),
                "Universidade de Vassouras. O aluno atuou como monitor da disciplina Topografia."), 18);
        String visita = enviarELer(aluno.sessao(), "Universidade de Vassouras. Visita tecnica a Usina Hidreletrica de Funil.");
        assertThat(JsonPath.<Integer>read(visita, "$.sugestaoItem.item.numero")).isEqualTo(39);
        confirmar(aluno.sessao(), visita, 50);

        var depois = mvc.get().uri("/api/v1/admin/classificacao/acuracia").cookie(coordenacao).exchange();
        String corpo = depois.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<Integer>read(corpo, "$.avaliados")).isEqualTo(avaliadosAntes + 2);
        assertThat(JsonPath.<Integer>read(corpo, "$.acertos")).isEqualTo(acertosAntes + 1);
        assertThat(JsonPath.<java.util.List<Integer>>read(corpo, "$.trocasMaisComuns[?(@.sugerido == 39 && @.escolhido == 50)].vezes"))
                .isNotEmpty();

        // Aluno não vê a medição.
        assertThat(mvc.get().uri("/api/v1/admin/classificacao/acuracia").cookie(aluno.sessao())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void administracaoAjustaPalavrasChaveESoElaPode() {
        Cookie admin = entrar("admin@dev.local", "sereno-dev");
        Cookie coordenacao = entrar("coordenacao@dev.local", "sereno-dev");
        String corpo = """
                {"palavrasChave":["projeto rondon","rondon","!operacao rondon"],"origemEsperada":"QUALQUER"}""";

        assertThat(mvc.put().uri("/api/v1/admin/regulamentos/vigente/itens/52/classificacao").with(csrf()).cookie(admin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatusOk()
                .bodyJson().extractingPath("$.palavrasChave[2]").isEqualTo("!operacao rondon");
        assertThat(mvc.put().uri("/api/v1/admin/regulamentos/vigente/itens/52/classificacao").with(csrf()).cookie(coordenacao)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.put().uri("/api/v1/admin/regulamentos/vigente/itens/99/classificacao").with(csrf()).cookie(admin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
