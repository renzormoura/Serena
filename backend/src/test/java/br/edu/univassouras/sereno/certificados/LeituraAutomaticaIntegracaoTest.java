package br.edu.univassouras.sereno.certificados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import br.edu.univassouras.sereno.IntegracaoBase;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class LeituraAutomaticaIntegracaoTest extends IntegracaoBase {

    /** Um PDF de verdade, com texto embutido, como os emitidos pelas plataformas de curso. */
    static byte[] pdfComTexto(String... linhas) throws IOException {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            pdf.addPage(pagina);
            try (PDPageContentStream conteudo = new PDPageContentStream(pdf, pagina)) {
                conteudo.beginText();
                conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                conteudo.newLineAtOffset(50, 700);
                for (String linha : linhas) {
                    conteudo.showText(linha);
                    conteudo.newLineAtOffset(0, -18);
                }
                conteudo.endText();
            }
            pdf.save(saida);
            return saida.toByteArray();
        }
    }

    static byte[] png() throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", saida);
        return saida.toByteArray();
    }

    private MvcTestResult enviar(Cookie sessao, MockMultipartFile... arquivos) {
        var requisicao = mvc.post().uri("/api/v1/certificados/envios").multipart();
        for (MockMultipartFile arquivo : arquivos) {
            requisicao = requisicao.file(arquivo);
        }
        return requisicao.with(csrf()).cookie(sessao).exchange();
    }

    private String aguardarFimDaLeitura(Cookie sessao, String id) {
        String[] corpo = new String[1];
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(200)).until(() -> {
            corpo[0] = mvc.get().uri("/api/v1/certificados/" + id).cookie(sessao).exchange()
                    .getResponse().getContentAsString(StandardCharsets.UTF_8);
            return !"PROCESSANDO".equals(JsonPath.read(corpo[0], "$.status"));
        });
        return corpo[0];
    }

    @Test
    void pdfComTextoEhLidoEFicaEsperandoConferencia() throws Exception {
        Conta aluno = novoAluno();
        String nome = JsonPath.read(mvc.get().uri("/api/v1/me").cookie(aluno.sessao()).exchange()
                .getResponse().getContentAsString(StandardCharsets.UTF_8), "$.nome");
        byte[] pdf = pdfComTexto("CERTIFICADO", "Certificamos que " + nome + " participou da Oficina de Robotica,",
                "realizada em 10/09/2025, com carga horaria de 8 horas.");

        var envio = enviar(aluno.sessao(), new MockMultipartFile("arquivos", "oficina.pdf", "application/pdf", pdf));

        assertThat(envio).hasStatus(HttpStatus.ACCEPTED);
        String id = JsonPath.read(envio.getResponse().getContentAsString(StandardCharsets.UTF_8), "$[0].certificado.id");

        String lido = aguardarFimDaLeitura(aluno.sessao(), id);
        assertThat((String) JsonPath.read(lido, "$.status")).isEqualTo("AGUARDANDO_CONFIRMACAO");
        assertThat((Double) JsonPath.read(lido, "$.horasCertificado")).isEqualTo(8.0);
        assertThat((String) JsonPath.read(lido, "$.dataInicio")).isEqualTo("2025-09-10");
        assertThat((String) JsonPath.read(lido, "$.titulo")).isEqualTo("Oficina de Robotica");
        assertThat((String) JsonPath.read(lido, "$.leitura.metodo")).isEqualTo("TEXTO");
        assertThat((Boolean) JsonPath.read(lido, "$.leitura.nomeConfere")).isTrue();
        // Fase F3: o item também vem sugerido pelas regras ("oficina" → 21 · Oficinas Acadêmicas).
        assertThat(JsonPath.<Integer>read(lido, "$.sugestaoItem.item.numero")).isEqualTo(21);

        // Enquanto não é conferido, não conta no saldo.
        assertThat(mvc.get().uri("/api/v1/alunos/me/saldo").cookie(aluno.sessao()))
                .bodyJson().extractingPath("$.projecao").isEqualTo(0);

        // O aluno confere, escolhe o item e salva: vai para a fila da coordenação.
        assertThat(mvc.put().uri("/api/v1/certificados/" + id).with(csrf()).cookie(aluno.sessao())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"versao":%d,"dados":{"titulo":"Oficina de Robótica","horasCertificado":8,"itemNumero":21,
                         "dataInicio":"2025-09-10"}}""".formatted(JsonPath.<Integer>read(lido, "$.versao"))))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("CONFIRMADO");
        assertThat(mvc.get().uri("/api/v1/alunos/me/saldo").cookie(aluno.sessao()))
                .bodyJson().extractingPath("$.projecao").isEqualTo(10.0);
    }

    @Test
    void imagemSemOcrFicaParaPreenchimentoManualComAviso() throws Exception {
        Conta aluno = novoAluno();

        var envio = enviar(aluno.sessao(), new MockMultipartFile("arquivos", "foto.png", "image/png", png()));
        String id = JsonPath.read(envio.getResponse().getContentAsString(StandardCharsets.UTF_8), "$[0].certificado.id");

        String lido = aguardarFimDaLeitura(aluno.sessao(), id);
        assertThat((String) JsonPath.read(lido, "$.status")).isEqualTo("AGUARDANDO_CONFIRMACAO");
        assertThat((String) JsonPath.read(lido, "$.leitura.metodo")).isEqualTo("SEM_TEXTO");
        assertThat(JsonPath.<java.util.List<String>>read(lido, "$.leitura.avisos")).isNotEmpty();
        assertThat(JsonPath.<Object>read(lido, "$.horasCertificado")).isNull();
    }

    @Test
    void enviarVariosArquivosTrataCadaUmAParte() throws Exception {
        Conta aluno = novoAluno();
        byte[] repetido = pdfComTexto("Certificado A", "carga horaria de 2 horas");

        var envio = enviar(aluno.sessao(),
                new MockMultipartFile("arquivos", "a.pdf", "application/pdf", repetido),
                new MockMultipartFile("arquivos", "a-de-novo.pdf", "application/pdf", repetido),
                new MockMultipartFile("arquivos", "pagina.pdf", "application/pdf", "<html></html>".getBytes(StandardCharsets.UTF_8)));

        assertThat(envio).hasStatus(HttpStatus.ACCEPTED);
        String corpo = envio.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<String>read(corpo, "$[0].certificado.status")).isEqualTo("PROCESSANDO");
        assertThat(JsonPath.<String>read(corpo, "$[1].erroCodigo")).isEqualTo("arquivo-duplicado");
        assertThat(JsonPath.<String>read(corpo, "$[2].erroCodigo")).isEqualTo("arquivo-tipo");
    }

    @Test
    void conferirSemItemOuHorasEhRecusado() throws Exception {
        Conta aluno = novoAluno();
        var envio = enviar(aluno.sessao(), new MockMultipartFile("arquivos", "x.pdf", "application/pdf",
                pdfComTexto("Certificado sem nada util aqui para leitura automatica")));
        String id = JsonPath.read(envio.getResponse().getContentAsString(StandardCharsets.UTF_8), "$[0].certificado.id");
        String lido = aguardarFimDaLeitura(aluno.sessao(), id);

        assertThat(mvc.put().uri("/api/v1/certificados/" + id).with(csrf()).cookie(aluno.sessao())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"versao":%d,"dados":{"titulo":"Algo"}}""".formatted(JsonPath.<Integer>read(lido, "$.versao"))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.campos.itemNumero").isNotNull();
    }
}
