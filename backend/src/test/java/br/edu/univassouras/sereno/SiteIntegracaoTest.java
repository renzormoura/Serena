package br.edu.univassouras.sereno;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** O backend serve as telas (build do frontend) no mesmo endereço da API, como no deploy com Docker. */
class SiteIntegracaoTest extends IntegracaoBase {

    @Test
    void telasSaoPublicasERotasDoReactDevolvemOIndex() {
        // Na raiz, a página inicial do Spring Boot encaminha para o index.html (o MockMvc não segue o encaminhamento).
        assertThat(mvc.get().uri("/")).hasStatusOk().hasForwardedUrl("index.html");
        assertThat(mvc.get().uri("/coordenacao/fila")).hasStatusOk().bodyText().contains("tela-de-teste");
        assertThat(mvc.get().uri("/certificados/123")).hasStatusOk().bodyText().contains("tela-de-teste");
    }

    @Test
    void apiContinuaProtegidaEArquivoInexistenteDa404() {
        assertThat(mvc.get().uri("/api/v1/certificados")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/assets/nao-existe.js")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/v1/rota-que-nao-existe").cookie(novoAluno().sessao())).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void administradorInicialVemDasVariaveisDeAmbiente() {
        assertThat(mvc.get().uri("/api/v1/me").cookie(entrar("admin-inicial@teste.local", "inicial-123!")))
                .hasStatusOk().bodyJson().extractingPath("$.papeis[0]").isEqualTo("ADMIN");
    }
}
