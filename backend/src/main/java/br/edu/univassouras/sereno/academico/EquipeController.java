package br.edu.univassouras.sereno.academico;

import br.edu.univassouras.sereno.identidade.Papel;
import br.edu.univassouras.sereno.identidade.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Equipe da coordenação: listar e criar contas de coordenadores e administradores. */
@RestController
@RequestMapping("/api/v1/coordenacao/equipe")
@PreAuthorize("hasAnyRole('COORDENADOR', 'ADMIN')")
class EquipeController {

    private final EquipeService servico;

    EquipeController(EquipeService servico) {
        this.servico = servico;
    }

    /**
     * @param senha    senha inicial (mesma regra do cadastro); passe-a à pessoa por um canal seguro
     * @param cursoIds obrigatório para coordenador; ignorado para administrador (vê todos os cursos)
     */
    record NovoMembroForm(
            @NotBlank(message = "Informe o nome.") @Size(max = 200) String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 254) String email,
            @NotBlank(message = "Crie uma senha inicial.") @Size(max = 200) String senha,
            @NotNull(message = "Escolha o perfil.") Papel papel,
            List<UUID> cursoIds) {
    }

    @GetMapping
    EquipeService.EquipeDto equipe(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.equipe(usuario.id(), usuario.papel());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EquipeService.MembroDto criar(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody NovoMembroForm form) {
        return servico.criar(usuario.id(), usuario.papel(),
                new EquipeService.NovoMembro(form.nome(), form.email(), form.senha(), form.papel(), form.cursoIds()));
    }
}
