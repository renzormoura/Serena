package br.edu.univassouras.sereno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes de integração: Postgres real (Testcontainers), Flyway, segurança e MockMvc. Todas as classes
 * filhas compartilham o mesmo contexto Spring. Pulados quando o Docker não está disponível.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"sereno.arquivos.diretorio=target/arquivos-de-teste", "sereno.notificacao.email-ativo=false"})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public abstract class IntegracaoBase {

    protected static final String ENGENHARIA_CIVIL = "00000000-0000-0000-0000-0000000000c2";
    protected static final String SENHA = "senha-de-teste-123";
    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    protected MockMvcTester mvc;

    protected record Conta(String email, String matricula, Cookie sessao) {
    }

    /** Cadastra um aluno novo (e-mail e matrícula únicos) e devolve a sessão já autenticada. */
    protected Conta novoAluno() {
        int n = SEQUENCIA.incrementAndGet();
        String sufixo = n + "-" + UUID.randomUUID().toString().substring(0, 8);
        String email = "aluno" + sufixo + "@teste.local";
        String matricula = "T" + Math.abs(sufixo.hashCode());
        assertThat(mvc.post().uri("/api/v1/cadastro").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Aluno %d","email":"%s","senha":"%s","matricula":"%s",
                         "cursoId":"%s"}"""
                        .formatted(n, email, SENHA, matricula, ENGENHARIA_CIVIL)))
                .hasStatus(HttpStatus.CREATED);
        return new Conta(email, matricula, entrar(email, SENHA));
    }

    protected Cookie entrar(String email, String senha) {
        var resposta = mvc.post().uri("/api/v1/auth/login").with(csrf())
                .param("email", email).param("senha", senha)
                .exchange();
        assertThat(resposta).hasStatus(HttpStatus.NO_CONTENT);
        Cookie sessao = resposta.getMvcResult().getResponse().getCookie("SERENO_SESSION");
        assertThat(sessao).isNotNull();
        return sessao;
    }
}
