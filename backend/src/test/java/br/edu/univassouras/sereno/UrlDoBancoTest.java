package br.edu.univassouras.sereno;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlDoBancoTest {

    @Test
    void urlInternaDoRenderViraJdbcComUsuarioESenha() {
        var c = UrlDoBanco.converter("postgresql://sereno:abc123@dpg-xyz-a/sereno_db").orElseThrow();
        assertThat(c.url()).isEqualTo("jdbc:postgresql://dpg-xyz-a:5432/sereno_db");
        assertThat(c.usuario()).contains("sereno");
        assertThat(c.senha()).contains("abc123");
    }

    @Test
    void urlExternaDoRenderGanhaSsl() {
        var c = UrlDoBanco.converter("postgres://u:p%40ss@dpg-xyz-a.oregon-postgres.render.com:5433/db").orElseThrow();
        assertThat(c.url()).isEqualTo("jdbc:postgresql://dpg-xyz-a.oregon-postgres.render.com:5433/db?sslmode=require");
        assertThat(c.senha()).contains("p@ss");
    }

    @Test
    void urlJaEmFormatoJdbcFicaComoEsta() {
        assertThat(UrlDoBanco.converter("jdbc:postgresql://localhost:5432/sereno")).isEmpty();
        assertThat(UrlDoBanco.converter(null)).isEmpty();
    }
}
