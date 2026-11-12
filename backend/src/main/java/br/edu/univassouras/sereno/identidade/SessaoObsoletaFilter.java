package br.edu.univassouras.sereno.identidade;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sessões criadas por uma versão anterior da aplicação podem guardar outro tipo de usuário. Em vez de deixar o
 * controller falhar com 500, a sessão é descartada e a requisição segue como anônima (401 nas rotas protegidas).
 */
final class SessaoObsoletaFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.isAuthenticated()
                && !(autenticacao.getPrincipal() instanceof UsuarioAutenticado)) {
            SecurityContextHolder.clearContext();
            HttpSession sessao = request.getSession(false);
            if (sessao != null) {
                sessao.invalidate();
            }
        }
        chain.doFilter(request, response);
    }
}
