package br.edu.univassouras.sereno.identidade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Sessão no servidor (Spring Session JDBC) com cookie HttpOnly e CSRF no padrão SPA (cookie XSRF-TOKEN +
 * cabeçalho X-XSRF-TOKEN). Sem JWT no navegador — ver docs/adr/0003-sessao-em-vez-de-jwt.md.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfig {

    static final String LOGIN = "/api/v1/auth/login";
    static final String LOGOUT = "/api/v1/auth/logout";

    private final boolean documentacaoAberta;

    SecurityConfig(@Value("${sereno.api.documentacao-aberta:false}") boolean documentacaoAberta) {
        this.documentacaoAberta = documentacaoAberta;
    }

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.spa())
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .addFilterBefore(new SessaoObsoletaFilter(), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, LOGIN, "/api/v1/cadastro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/regulamentos/**", "/api/v1/cursos").permitAll()
                        .requestMatchers("/v3/api-docs/**").access((autenticacao, contexto) -> new AuthorizationDecision(documentacaoAberta))
                        .requestMatchers("/api/**", "/actuator/**").authenticated()
                        // O resto são as telas (o build do frontend servido pelo próprio backend): públicas, porque
                        // os dados vêm sempre da API, que exige login.
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginProcessingUrl(LOGIN)
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .successHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler((request, response, exception) -> response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                .logout(logout -> logout
                        .logoutUrl(LOGOUT)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // SAMEORIGIN para o SPA poder pré-visualizar o PDF do certificado num iframe.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
