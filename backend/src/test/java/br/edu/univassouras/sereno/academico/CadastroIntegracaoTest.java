package br.edu.univassouras.sereno.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import br.edu.univassouras.sereno.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class CadastroIntegracaoTest extends IntegracaoBase {

    private String corpo(String email, String senha, String matricula) {
        return """
                {"nome":"Maria Teste","email":"%s","senha":"%s","matricula":"%s",
                 "cursoId":"%s"}"""
                .formatted(email, senha, matricula, ENGENHARIA_CIVIL);
    }

    @Test
    void cadastroPermiteEntrarEMostraONome() {
        assertThat(mvc.post().uri("/api/v1/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Maria.Teste@Teste.Local", SENHA, "CAD001")))
                .hasStatus(HttpStatus.CREATED);

        // E-mail é normalizado: login funciona com outra caixa.
        var sessao = entrar("maria.teste@teste.local", SENHA);
        assertThat(mvc.get().uri("/api/v1/me").cookie(sessao))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.nome").isEqualTo("Maria Teste");
    }

    @Test
    void recusaEmailEMatriculaRepetidos() {
        Conta existente = novoAluno();

        assertThat(mvc.post().uri("/api/v1/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(existente.email().toUpperCase(), SENHA, "OUTRA123")))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("email-em-uso");

        assertThat(mvc.post().uri("/api/v1/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo("novo-email@teste.local", SENHA, existente.matricula())))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("matricula-em-uso");
    }

    @Test
    void senhaPrecisaDe8CaracteresUmaLetraEUmEspecial() {
        for (String fraca : new String[] {"ab-1", "abcdefgh1", "12345678!"}) {
            assertThat(mvc.post().uri("/api/v1/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(corpo("fraca@teste.local", fraca, "CAD002")))
                    .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                    .bodyJson().extractingPath("$.codigo").isEqualTo("senha-fraca");
        }
        assertThat(mvc.post().uri("/api/v1/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(corpo("boa@teste.local", "abcdef-1", "CAD003")))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void listaAsQuatroEngenhariasSemLogin() {
        var cursos = mvc.get().uri("/api/v1/cursos").exchange();
        assertThat(cursos).hasStatusOk();
        assertThat(com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(
                new String(cursos.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8), "$[*].nome"))
                .containsExactlyInAnyOrder("Engenharia Civil", "Engenharia de Software", "Engenharia Elétrica", "Engenharia Química");
    }
}
