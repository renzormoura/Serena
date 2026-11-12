package br.edu.univassouras.sereno.comum;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI fiel ao JSON real: respostas sempre trazem todos os campos (records serializam tudo), e só os
 * marcados com {@link Anulavel} podem vir nulos. Formulários de entrada mantêm o "required" das anotações de
 * validação.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI contratoSereno() {
        return new OpenAPI()
                .info(new Info().title("Sereno API").version("v1")
                        .description("Horas complementares das Engenharias — Universidade de Vassouras"))
                // Servidor relativo: o contrato não muda conforme a porta ou host em que foi gerado.
                .servers(List.of(new Server().url("/")));
    }

    @Bean
    PropertyCustomizer camposAnulaveis() {
        return (propriedade, tipo) -> {
            boolean anulavel = tipo.getCtxAnnotations() != null
                    && Arrays.stream(tipo.getCtxAnnotations()).anyMatch(a -> a.annotationType() == Anulavel.class);
            if (anulavel) {
                if (propriedade.get$ref() != null) {
                    // OpenAPI 3.1: um $ref não aceita "type"; o nulo vai como alternativa.
                    Schema<Object> referencia = new Schema<>();
                    referencia.set$ref(propriedade.get$ref());
                    Schema<Object> nulo = new Schema<>();
                    nulo.addType("null");
                    Schema<Object> alternativas = new Schema<>();
                    alternativas.setAnyOf(List.<Schema>of(referencia, nulo));
                    return alternativas;
                }
                if (propriedade.getTypes() != null) {
                    propriedade.addType("null");
                } else {
                    propriedade.setNullable(true);
                }
            }
            return propriedade;
        };
    }

    @Bean
    OpenApiCustomizer respostasComCamposObrigatorios() {
        return openApi -> {
            Map<String, Schema> esquemas = openApi.getComponents() == null ? Map.of() : openApi.getComponents().getSchemas();
            if (esquemas == null) {
                return;
            }
            Set<String> deEntrada = esquemasDeEntrada(openApi, esquemas);
            esquemas.forEach((nome, esquema) -> {
                if (!deEntrada.contains(nome) && esquema.getProperties() != null) {
                    esquema.setRequired(esquema.getProperties().keySet().stream().map(Object::toString).sorted().toList());
                }
            });
        };
    }

    /** Esquemas alcançáveis a partir de corpos de requisição (inclusive aninhados). */
    private static Set<String> esquemasDeEntrada(OpenAPI openApi, Map<String, Schema> esquemas) {
        Deque<Schema<?>> pendentes = new ArrayDeque<>();
        openApi.getPaths().values().stream()
                .flatMap(p -> p.readOperations().stream())
                .map(Operation::getRequestBody)
                .filter(Objects::nonNull)
                .flatMap(corpo -> corpo.getContent().values().stream())
                .map(MediaType::getSchema)
                .filter(Objects::nonNull)
                .forEach(pendentes::add);

        Set<String> nomes = new HashSet<>();
        while (!pendentes.isEmpty()) {
            Schema<?> atual = pendentes.pop();
            if (atual.get$ref() != null) {
                String nome = atual.get$ref().substring(atual.get$ref().lastIndexOf('/') + 1);
                if (nomes.add(nome) && esquemas.containsKey(nome)) {
                    pendentes.push(esquemas.get(nome));
                }
                continue;
            }
            Stream.of(atual.getProperties() == null ? List.<Schema>of() : (Collection<Schema>) atual.getProperties().values(),
                            atual.getItems() == null ? List.<Schema>of() : List.<Schema>of(atual.getItems()))
                    .flatMap(Collection::stream)
                    .forEach(pendentes::push);
        }
        return nomes;
    }
}
