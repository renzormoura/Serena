package br.edu.univassouras.sereno.certificados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class LoginEExclusaoIntegracaoTest extends CoordenacaoBase {

    @Test
    void entraComAMatriculaNoCampoDeEmail() {
        Conta aluno = novoAluno();

        Cookie pelaMatricula = entrar(aluno.matricula().toLowerCase(), SENHA);
        assertThat(mvc.get().uri("/api/v1/me").cookie(pelaMatricula)).hasStatusOk();
        assertThat(mvc.post().uri("/api/v1/auth/login").with(csrf())
                .param("email", aluno.matricula()).param("senha", "senha-errada-1"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post().uri("/api/v1/auth/login").with(csrf())
                .param("email", "MATRICULA-QUE-NAO-EXISTE").param("senha", SENHA))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        // O e-mail continua funcionando.
        assertThat(mvc.get().uri("/api/v1/me").cookie(entrar(aluno.email(), SENHA))).hasStatusOk();
    }

    @Test
    void coordenacaoExcluiCertificadoComMotivoEOAlunoEhAvisado() throws Exception {
        Conta aluno = novoAluno();
        Lancado c = lancar(aluno.sessao(), 18, "60");
        Cookie coord = coordenacao();
        var validado = decidir(coord, c.id(), c.versao(), "\"decisao\":\"DEFERIR\"");
        long versao = versao(validado);
        assertThat(mvc.get().uri("/api/v1/alunos/me/saldo").cookie(aluno.sessao()))
                .bodyJson().extractingPath("$.integralizadas").isEqualTo(50.0);

        String uri = "/api/v1/coordenacao/certificados/" + c.id() + "/exclusao";
        assertThat(mvc.post().uri(uri).with(csrf()).cookie(coord).contentType(MediaType.APPLICATION_JSON)
                .content("{\"versao\":%d}".formatted(versao)))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("justificativa-obrigatoria");
        assertThat(mvc.post().uri(uri).with(csrf()).cookie(aluno.sessao()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"versao\":%d,\"justificativa\":\"x\"}".formatted(versao)))
                .hasStatus(HttpStatus.FORBIDDEN);

        assertThat(mvc.post().uri(uri).with(csrf()).cookie(coord).contentType(MediaType.APPLICATION_JSON)
                .content("{\"versao\":%d,\"justificativa\":\"Certificado enviado em duplicidade.\"}".formatted(versao)))
                .hasStatus(HttpStatus.NO_CONTENT);

        // Some da conta do aluno e do saldo.
        assertThat(JsonPath.<List<String>>read(corpo(mvc.get().uri("/api/v1/certificados").cookie(aluno.sessao()).exchange()),
                "$[*].id")).doesNotContain(c.id());
        assertThat(mvc.get().uri("/api/v1/alunos/me/saldo").cookie(aluno.sessao()))
                .bodyJson().extractingPath("$.integralizadas").isEqualTo(0);
        assertThat(mvc.get().uri("/api/v1/coordenacao/certificados/" + c.id()).cookie(coord)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.post().uri(uri).with(csrf()).cookie(coord).contentType(MediaType.APPLICATION_JSON)
                .content("{\"versao\":%d,\"justificativa\":\"de novo\"}".formatted(versao + 1)))
                .hasStatus(HttpStatus.NOT_FOUND);

        String[] caixa = new String[1];
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).until(() -> {
            caixa[0] = corpo(mvc.get().uri("/api/v1/avisos").cookie(aluno.sessao()).exchange());
            return JsonPath.<List<String>>read(caixa[0], "$.avisos[*].titulo").contains("Certificado excluído pela coordenação");
        });
        assertThat(caixa[0]).contains("Certificado enviado em duplicidade.");
    }
}
