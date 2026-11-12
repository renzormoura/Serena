package br.edu.univassouras.sereno;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Aceita a URL do banco no formato das plataformas (Render, Heroku etc.):
 * {@code postgresql://usuario:senha@host:porta/banco}, e a converte para o formato JDBC, separando usuário e senha.
 * Assim a "Internal/External Database URL" do Render pode ser colada direto em {@code SERENO_DB_URL}. Tolera espaços,
 * aspas e a falta do prefixo; quando não reconhece o valor, para com uma mensagem clara (sem mostrar a senha).
 */
final class UrlDoBanco {

    record Conexao(String url, Optional<String> usuario, Optional<String> senha) {
    }

    private UrlDoBanco() {
    }

    /** Vazio se não foi informada ou já está no formato JDBC: nada a converter. */
    static Optional<Conexao> converter(String bruta) {
        if (bruta == null) {
            return Optional.empty();
        }
        String url = bruta.strip().replaceAll("^[\"']+|[\"']+$", "").strip();
        if (url.isEmpty()) {
            return Optional.empty();
        }
        String minuscula = url.toLowerCase(Locale.ROOT);
        if (minuscula.startsWith("jdbc:")) {
            return url.equals(bruta) ? Optional.empty() : Optional.of(new Conexao(url, Optional.empty(), Optional.empty()));
        }
        if (!minuscula.startsWith("postgres://") && !minuscula.startsWith("postgresql://")) {
            if (minuscula.contains("://")) {
                throw new IllegalStateException("SERENO_DB_URL em formato desconhecido: começa com \""
                        + url.substring(0, url.indexOf("://") + 3) + "\". Use a Internal Database URL do Render "
                        + "(postgresql://...) ou jdbc:postgresql://host:5432/banco.");
            }
            // Sem prefixo (ex.: usuario:senha@host/banco ou host:5432/banco): assume PostgreSQL.
            url = "postgresql://" + url;
        }
        URI uri;
        try {
            uri = URI.create(url.replaceFirst("(?i)^postgres(ql)?://", "http://"));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("SERENO_DB_URL não é uma URL válida. Copie de novo a Internal Database URL do Render.", e);
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("SERENO_DB_URL sem o endereço do servidor (host). Copie de novo a Internal "
                    + "Database URL do Render.");
        }
        int porta = uri.getPort() == -1 ? 5432 : uri.getPort();
        String banco = uri.getPath() == null ? "" : uri.getPath();
        if (banco.isBlank() || banco.equals("/")) {
            throw new IllegalStateException("SERENO_DB_URL sem o nome do banco (host " + host + "). Cole a Internal Database "
                    + "URL inteira do Render, que termina com /nome_do_banco.");
        }
        String parametros = uri.getRawQuery() == null ? "" : uri.getRawQuery();
        // URL externa do Render (host *.render.com) só aceita conexão com SSL.
        if (host.endsWith(".render.com") && !parametros.contains("sslmode")) {
            parametros = parametros.isEmpty() ? "sslmode=require" : parametros + "&sslmode=require";
        }
        String jdbc = "jdbc:postgresql://" + host + ":" + porta + banco + (parametros.isEmpty() ? "" : "?" + parametros);

        Optional<String> usuario = Optional.empty();
        Optional<String> senha = Optional.empty();
        if (uri.getRawUserInfo() != null) {
            String[] partes = uri.getRawUserInfo().split(":", 2);
            usuario = Optional.of(decodificar(partes[0]));
            if (partes.length > 1) {
                senha = Optional.of(decodificar(partes[1]));
            }
        }
        return Optional.of(new Conexao(jdbc, usuario, senha));
    }

    /** Propriedades do Spring a definir antes de subir (usuário e senha só se vieram na URL). */
    static Map<String, String> propriedades(Conexao c) {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", c.url());
        c.usuario().ifPresent(u -> props.put("spring.datasource.username", u));
        c.senha().ifPresent(s -> props.put("spring.datasource.password", s));
        return props;
    }

    /** Para o log: o endereço sem usuário nem senha. */
    static String semSenha(Conexao c) {
        return c.url() + c.usuario().map(u -> " (usuário " + u + ")").orElse("");
    }

    private static String decodificar(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
