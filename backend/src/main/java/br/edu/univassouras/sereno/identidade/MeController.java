package br.edu.univassouras.sereno.identidade;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

    record UsuarioAtual(UUID id, String email, String nome, List<Papel> papeis) {
    }

    @GetMapping("/api/v1/me")
    UsuarioAtual me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return new UsuarioAtual(usuario.id(), usuario.email(), usuario.nome(), List.of(usuario.papel()));
    }
}
