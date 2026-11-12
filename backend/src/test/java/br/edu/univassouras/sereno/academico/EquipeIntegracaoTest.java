package br.edu.univassouras.sereno.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import br.edu.univassouras.sereno.IntegracaoBase;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class EquipeIntegracaoTest extends IntegracaoBase {

    static final String QUIMICA = "00000000-0000-0000-0000-0000000000c4";

    private MvcTestResult criar(Cookie quem, String papel, String cursos, String email) {
        return mvc.post().uri("/api/v1/coordenacao/equipe").with(csrf()).cookie(quem)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Nova Pessoa","email":"%s","senha":"inicial-123","papel":"%s","cursoIds":%s}"""
                        .formatted(email, papel, cursos))
                .exchange();
    }

    private static String email() {
        return "equipe-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.local";
    }

    @Test
    void coordenadorCriaCoordenadorQueJaEntraEVeAFila() {
        Cookie coord = entrar("coordenacao@dev.local", "sereno-dev");
        String email = email();

        var criado = criar(coord, "COORDENADOR", "[\"" + ENGENHARIA_CIVIL + "\"]", email);

        assertThat(criado).hasStatus(HttpStatus.CREATED);
        assertThat(criado).bodyJson().extractingPath("$.cursos[0].nome").isEqualTo("Engenharia Civil");
        Cookie novo = entrar(email, "inicial-123");
        assertThat(mvc.get().uri("/api/v1/coordenacao/fila").cookie(novo)).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/coordenacao/equipe").cookie(coord))
                .bodyJson().extractingPath("$.membros[?(@.email == '" + email + "')].papel").isEqualTo(java.util.List.of("COORDENADOR"));
    }

    @Test
    void coordenadorNaoCriaAdminNemDaCursoQueNaoCoordena() {
        // Um coordenador só de Civil.
        Cookie admin = entrar("admin@dev.local", "sereno-dev");
        String soCivil = email();
        assertThat(criar(admin, "COORDENADOR", "[\"" + ENGENHARIA_CIVIL + "\"]", soCivil)).hasStatus(HttpStatus.CREATED);
        Cookie coord = entrar(soCivil, "inicial-123");

        assertThat(criar(coord, "ADMIN", "[]", email())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(criar(coord, "COORDENADOR", "[\"" + QUIMICA + "\"]", email())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(criar(coord, "COORDENADOR", "[]", email()))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT).bodyJson().extractingPath("$.codigo").isEqualTo("sem-curso");
        assertThat(mvc.get().uri("/api/v1/coordenacao/equipe").cookie(coord))
                .bodyJson().extractingPath("$.podeCriarAdmin").isEqualTo(false);
    }

    @Test
    void adminCriaAdminERegrasDeContaValem() {
        Cookie admin = entrar("admin@dev.local", "sereno-dev");
        String email = email();

        assertThat(criar(admin, "ADMIN", "null", email)).hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.papel").isEqualTo("ADMIN");
        assertThat(criar(admin, "ADMIN", "null", email)).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("email-em-uso");
        assertThat(mvc.post().uri("/api/v1/coordenacao/equipe").with(csrf()).cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"X","email":"%s","senha":"semespecial1","papel":"ADMIN"}""".formatted(email())))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT).bodyJson().extractingPath("$.codigo").isEqualTo("senha-fraca");
        assertThat(criar(admin, "ALUNO", "null", email())).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void alunoNaoVeAEquipe() {
        assertThat(mvc.get().uri("/api/v1/coordenacao/equipe").cookie(novoAluno().sessao())).hasStatus(HttpStatus.FORBIDDEN);
    }
}
