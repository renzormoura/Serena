package br.edu.univassouras.sereno;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Aceita a URL do banco no formato das plataformas (Render, Heroku etc.):
 * {@code postgresql://usuario:senha@host:porta/banco}, e a converte para o formato JDBC, separando usuário e senha.
 * Assim a "Internal/External Database URL" do Render pode ser colada direto em {@code SERENO_DB_URL}.
 */
final class UrlDoBanco {

    record Conexao(String url, Optional<String> usuario, Optional<String> senha) {
    }

    private UrlDoBanco() {
    }

    /** Vazio se a URL já está no formato JDBC (ou não foi informada): nada a converter. */
    static Optional<Conexao> converter(String url) {
        if (url == null || !(url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
            return Optional.empty();
        }
        URI uri = URI.create(url.replaceFirst("^postgres(ql)?://", "http://"));
        String host = uri.getHost();
        int porta = uri.getPort() == -1 ? 5432 : uri.getPort();
        String banco = uri.getPath() == null ? "" : uri.getPath();
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

    private static String decodificar(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
