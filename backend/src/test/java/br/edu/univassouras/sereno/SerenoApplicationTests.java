package br.edu.univassouras.sereno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import br.edu.univassouras.sereno.calculo.CertificadoParaCalculo;
import br.edu.univassouras.sereno.calculo.MotorCalculo;
import br.edu.univassouras.sereno.calculo.RegrasCalculo;
import br.edu.univassouras.sereno.regulamento.CatalogoService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class SerenoApplicationTests extends IntegracaoBase {

    @Autowired
    CatalogoService catalogo;

    @Autowired
    MotorCalculo motor;

    @Test
    void catalogoSemeadoTem52ItensEm4Macroatividades() {
        assertThat(mvc.get().uri("/api/v1/regulamentos/vigente"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.macroatividades.length()").isEqualTo(4);
        assertThat(catalogo.vigente()).hasValueSatisfying(c -> assertThat(
                c.macroatividades().stream().mapToInt(m -> m.itens().size()).sum()).isEqualTo(52));
    }

    @Test
    void regulamentoSemeadoReproduzOTotalDaPlanilha() {
        RegrasCalculo regras = catalogo.regrasVigentes().orElseThrow();
        var certificados = Arrays.stream(new int[] {10, 10, 10, 44, 4, 19, 24, 24, 24, 24, 24, 44, 18})
                .mapToObj(item -> new CertificadoParaCalculo(UUID.randomUUID(), item, BigDecimal.ONE, Optional.empty(),
                        CertificadoParaCalculo.Situacao.DEFERIDO))
                .toList();

        assertThat(motor.calcular(regras, certificados).integralizadas()).isEqualByComparingTo("430");
    }

    @Test
    void cookieDeSessaoEhHttpOnlyESameSite() {
        assertThat(mvc.get().uri("/api/v1/me")).hasStatus(HttpStatus.UNAUTHORIZED);

        var login = mvc.post().uri("/api/v1/auth/login").with(csrf())
                .param("email", "aluno@dev.local").param("senha", "sereno-dev")
                .exchange();
        assertThat(login).hasStatus(HttpStatus.NO_CONTENT);

        // A sessão vive no Spring Session JDBC: o que volta ao navegador é o cookie SERENO_SESSION.
        var cookieSessao = login.getMvcResult().getResponse().getCookie("SERENO_SESSION");
        assertThat(cookieSessao).isNotNull();
        assertThat(cookieSessao.isHttpOnly()).isTrue();
        assertThat(login.getMvcResult().getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(h -> assertThat(h).startsWith("SERENO_SESSION=").contains("SameSite=Lax"));
        assertThat(mvc.get().uri("/api/v1/me").cookie(cookieSessao))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.papeis[0]").isEqualTo("ALUNO");
    }

    @Test
    void loginRecusaSenhaErrada() {
        assertThat(mvc.post().uri("/api/v1/auth/login").with(csrf())
                .param("email", "aluno@dev.local").param("senha", "errada"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sessaoComUsuarioDeOutroTipoPedeLoginEmVezDeFalhar() {
        // Simula uma sessão criada por versão anterior da aplicação (principal que não é UsuarioAutenticado).
        assertThat(mvc.get().uri("/api/v1/me")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .user("antigo@dev.local").roles("ALUNO")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void postSemTokenCsrfEhRecusado() {
        assertThat(mvc.post().uri("/api/v1/auth/login")
                .param("email", "aluno@dev.local").param("senha", "sereno-dev"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }
}
