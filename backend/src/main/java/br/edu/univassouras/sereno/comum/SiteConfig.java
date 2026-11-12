package br.edu.univassouras.sereno.comum;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serve o frontend (build do Vite copiado para {@code classpath:/static} no Dockerfile) no mesmo endereço da API,
 * para o cookie de sessão funcionar. Rotas do React como {@code /coordenacao/fila} devolvem o {@code index.html};
 * {@code /api/**} nunca cai aqui. Em desenvolvimento a pasta está vazia e o frontend roda no Vite.
 */
@Configuration(proxyBeanMethods = false)
class SiteConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String caminho, Resource local) throws IOException {
                        Resource arquivo = local.createRelative(caminho);
                        boolean pasta = caminho.isEmpty() || caminho.endsWith("/");
                        if (!pasta && arquivo.exists() && arquivo.isReadable()) {
                            return arquivo;
                        }
                        // Arquivo que não existe (com extensão) ou chamada de API: 404 de verdade.
                        if (caminho.startsWith("api/") || caminho.startsWith("actuator") || caminho.contains(".")) {
                            return null;
                        }
                        Resource indice = local.createRelative("index.html");
                        return indice.exists() ? indice : null;
                    }
                });
    }
}
