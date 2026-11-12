package br.edu.univassouras.sereno.certificados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/** Fase 5.1: triagem na fila, observação do aluno, avisos no site e ingresso informado pela coordenação. */
class Fase51IntegracaoTest extends CoordenacaoBase {

    private Lancado lancar(Cookie aluno, String dados, byte[] arquivo) throws Exception {
        var resposta = mvc.post().uri("/api/v1/certificados").multipart()
                .file(new MockMultipartFile("arquivo", "c.pdf", "application/pdf", arquivo))
                .file(new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE, dados.getBytes(StandardCharsets.UTF_8)))
                .with(csrf()).cookie(aluno)
                .exchange();
        assertThat(resposta).hasStatus(HttpStatus.CREATED);
        String corpo = corpo(resposta);
        return new Lancado(JsonPath.read(corpo, "$.id"), JsonPath.<Integer>read(corpo, "$.versao"));
    }

    private static String dados(String titulo, String extra) {
        return """
                {"titulo":"%s","instituicao":"Universidade de Vassouras","dataInicio":"2025-05-14","horasCertificado":12,
                 "itemNumero":25%s}""".formatted(titulo, extra);
    }

    private List<String> alertasNaFila(Cookie coord, String id) throws Exception {
        return JsonPath.read(corpo(mvc.get().uri("/api/v1/coordenacao/fila").cookie(coord).exchange()),
                "$[?(@.certificado.id == '" + id + "')].alertas[*].tipo");
    }

    @Test
    void filaApontaArquivoDeOutraContaECertificadoRepetido() throws Exception {
        Conta ana = novoAluno();
        Conta bruno = novoAluno();
        byte[] mesmoPdf = ("%PDF-1.4\n% compartilhado " + UUID.randomUUID() + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII);
        String titulo = "Semana Acadêmica " + UUID.randomUUID().toString().substring(0, 6);

        Lancado daAna = lancar(ana.sessao(), dados(titulo, ""), mesmoPdf);
        Lancado doBruno = lancar(bruno.sessao(), dados("Outro evento", ""), mesmoPdf);
        // A Ana manda o mesmo certificado de novo, como foto (arquivo diferente).
        Lancado repetido = lancar(ana.sessao(), dados(titulo.toUpperCase(), ""),
                ("%PDF-1.4\n% foto " + UUID.randomUUID() + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII));

        Cookie coord = coordenacao();
        assertThat(alertasNaFila(coord, daAna.id())).contains("ARQUIVO_DE_OUTRA_CONTA", "CERTIFICADO_REPETIDO");
        assertThat(alertasNaFila(coord, doBruno.id())).contains("ARQUIVO_DE_OUTRA_CONTA").doesNotContain("CERTIFICADO_REPETIDO");
        assertThat(alertasNaFila(coord, repetido.id())).contains("CERTIFICADO_REPETIDO").doesNotContain("ARQUIVO_DE_OUTRA_CONTA");

        // Alerta forte põe o certificado no topo (prioridade 0) e aparece também na análise.
        assertThat(JsonPath.<List<Integer>>read(corpo(mvc.get().uri("/api/v1/coordenacao/fila").cookie(coord).exchange()),
                "$[?(@.certificado.id == '" + daAna.id() + "')].prioridade")).containsExactly(0);
        assertThat(mvc.get().uri("/api/v1/coordenacao/certificados/" + daAna.id()).cookie(coord))
                .bodyJson().extractingPath("$.alertas[0].gravidade").isEqualTo("FORTE");
    }

    @Test
    void observacaoDoAlunoChegaNaAnaliseELimiteDe250() throws Exception {
        Conta aluno = novoAluno();
        byte[] pdf = ("%PDF-1.4\n% obs " + UUID.randomUUID() + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII);
        Lancado c = lancar(aluno.sessao(), dados("Palestra", ",\"observacao\":\"Fui monitor do evento também.\""), pdf);

        assertThat(mvc.get().uri("/api/v1/coordenacao/certificados/" + c.id()).cookie(coordenacao()))
                .bodyJson().extractingPath("$.certificado.observacaoAluno").isEqualTo("Fui monitor do evento também.");

        var longa = mvc.post().uri("/api/v1/certificados").multipart()
                .file(new MockMultipartFile("arquivo", "c.pdf", "application/pdf",
                        ("%PDF-1.4\n% longa " + UUID.randomUUID() + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII)))
                .file(new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE,
                        dados("Palestra", ",\"observacao\":\"" + "x".repeat(251) + "\"").getBytes(StandardCharsets.UTF_8)))
                .with(csrf()).cookie(aluno.sessao())
                .exchange();
        assertThat(longa).hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.campos.observacao").isNotNull();
    }

    @Test
    void alunoRecebeAvisoNoSiteComOMotivo() throws Exception {
        Conta aluno = novoAluno();
        Lancado c = lancar(aluno.sessao(), dados("Curso X", ""),
                ("%PDF-1.4\n% aviso " + UUID.randomUUID() + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII));

        decidir(coordenacao(), c.id(), c.versao(), "\"decisao\":\"INDEFERIR\",\"justificativa\":\"Falta a carga horária.\"");

        String[] caixa = new String[1];
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).until(() -> {
            caixa[0] = corpo(mvc.get().uri("/api/v1/avisos").cookie(aluno.sessao()).exchange());
            return JsonPath.<Integer>read(caixa[0], "$.naoLidos") > 0;
        });
        assertThat(JsonPath.<String>read(caixa[0], "$.avisos[0].titulo")).isEqualTo("Certificado reprovado");
        assertThat(JsonPath.<String>read(caixa[0], "$.avisos[0].mensagem")).contains("Falta a carga horária.");
        assertThat(JsonPath.<String>read(caixa[0], "$.avisos[0].link")).isEqualTo("/certificados/" + c.id());

        assertThat(mvc.post().uri("/api/v1/avisos/lidos").with(csrf()).cookie(aluno.sessao())).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/v1/avisos").cookie(aluno.sessao())).bodyJson().extractingPath("$.naoLidos").isEqualTo(0);
    }

    @Test
    void coordenacaoInformaOIngressoEOPeriodoAparece() throws Exception {
        Conta aluno = novoAluno();
        Cookie coord = coordenacao();
        String lista = corpo(mvc.get().uri("/api/v1/coordenacao/alunos").cookie(coord).exchange());
        String alunoId = JsonPath.<List<String>>read(lista, "$[?(@.matricula == '" + aluno.matricula() + "')].id").getFirst();
        assertThat(JsonPath.<List<Object>>read(lista, "$[?(@.matricula == '" + aluno.matricula() + "')].periodo")).containsExactly((Object) null);

        assertThat(mvc.put().uri("/api/v1/coordenacao/alunos/" + alunoId + "/ingresso").with(csrf()).cookie(coord)
                .contentType(MediaType.APPLICATION_JSON).content("{\"anoIngresso\":2022}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(mvc.put().uri("/api/v1/coordenacao/alunos/" + alunoId + "/ingresso").with(csrf()).cookie(coord)
                .contentType(MediaType.APPLICATION_JSON).content("{\"anoIngresso\":2022,\"semestreIngresso\":1}"))
                .hasStatusOk().bodyJson().extractingPath("$.aluno.periodo").isNotNull();
    }
}
