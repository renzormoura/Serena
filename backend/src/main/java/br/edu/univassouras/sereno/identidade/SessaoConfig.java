package br.edu.univassouras.sereno.identidade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Cookie de sessão definido explicitamente. Com Spring Session JDBC, as propriedades
 * {@code server.servlet.session.cookie.*} não chegam ao cookie: sem este bean ele sai como {@code SESSION}, sem
 * {@code HttpOnly} e sem {@code SameSite}. O teste {@code SerenoApplicationTests} protege essa configuração.
 */
@Configuration(proxyBeanMethods = false)
class SessaoConfig {

    static final String COOKIE = "SERENO_SESSION";

    @Bean
    CookieSerializer cookieDeSessao(@Value("${sereno.sessao.cookie-seguro:true}") boolean seguro) {
        DefaultCookieSerializer cookie = new DefaultCookieSerializer();
        cookie.setCookieName(COOKIE);
        cookie.setCookiePath("/");
        cookie.setUseHttpOnlyCookie(true);
        cookie.setSameSite("Lax");
        cookie.setUseSecureCookie(seguro);
        return cookie;
    }
}
