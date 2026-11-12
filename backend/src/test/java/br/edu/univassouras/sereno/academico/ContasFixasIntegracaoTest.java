package br.edu.univassouras.sereno.academico;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.univassouras.sereno.IntegracaoBase;
import org.junit.jupiter.api.Test;

/** As contas fixas existem ao subir, mesmo com o banco vazio, e entram com as senhas definidas no código. */
class ContasFixasIntegracaoTest extends IntegracaoBase {

    @Test
    void administradorEAlunosFixosEntram() {
        assertThat(mvc.get().uri("/api/v1/me").cookie(entrar("adm@gmail.com", "12345678")))
                .hasStatusOk().bodyJson().extractingPath("$.papeis[0]").isEqualTo("ADMIN");

        assertThat(mvc.get().uri("/api/v1/me").cookie(entrar("202410568", "24652536")))
                .hasStatusOk().bodyJson().extractingPath("$.nome").isEqualTo("Renzo Moura Rosa");
        assertThat(mvc.get().uri("/api/v1/me").cookie(entrar("renzormoura@gmail.com", "24652536"))).hasStatusOk();

        assertThat(mvc.get().uri("/api/v1/me").cookie(entrar("202412748", "12345678")))
                .hasStatusOk().bodyJson().extractingPath("$.nome").isEqualTo("Pedro Henrique do Nascimento Gama");

        assertThat(mvc.get().uri("/api/v1/coordenacao/fila").cookie(entrar("coordenacao@dev.local", "sereno-dev"))).hasStatusOk();
    }
}
