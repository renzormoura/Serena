package br.edu.univassouras.sereno;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Mantém {@code docs/api/openapi.json} igual ao contrato real da API. O frontend gera seus tipos TypeScript a
 * partir desse arquivo ({@code npm run api:tipos}).
 *
 * <p>Mudou a API de propósito? Rode {@code ./mvnw test -Dtest=ContratoOpenApiTest -Dcontrato.atualizar=true} e
 * depois {@code npm run api:tipos} no frontend.
 */
class ContratoOpenApiTest extends IntegracaoBase {

    private static final Path CONTRATO = Path.of("..", "docs", "api", "openapi.json");

    @Test
    void contratoPublicadoEstaAtualizado() throws Exception {
        var resposta = mvc.get().uri("/v3/api-docs").exchange();
        assertThat(resposta).hasStatusOk();
        String atual = resposta.getResponse().getContentAsString(StandardCharsets.UTF_8).replace("\r\n", "\n").strip() + "\n";

        if (Boolean.getBoolean("contrato.atualizar")) {
            Files.createDirectories(CONTRATO.getParent());
            Files.writeString(CONTRATO, atual, StandardCharsets.UTF_8);
        }

        assertThat(CONTRATO).exists();
        assertThat(Files.readString(CONTRATO, StandardCharsets.UTF_8))
                .as("docs/api/openapi.json desatualizado: rode com -Dcontrato.atualizar=true")
                .isEqualTo(atual);
    }
}
