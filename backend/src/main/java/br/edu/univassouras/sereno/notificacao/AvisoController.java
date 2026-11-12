package br.edu.univassouras.sereno.notificacao;

import br.edu.univassouras.sereno.comum.Anulavel;
import br.edu.univassouras.sereno.identidade.UsuarioAutenticado;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Avisos do usuário logado. */
@RestController
class AvisoController {

    static final int MOSTRADOS = 50;

    private final AvisoRepository avisos;

    AvisoController(AvisoRepository avisos) {
        this.avisos = avisos;
    }

    record AvisoDto(UUID id, String titulo, String mensagem, @Anulavel String link, boolean lido, Instant criadoEm) {
    }

    record CaixaDeAvisos(long naoLidos, List<AvisoDto> avisos) {
    }

    @GetMapping("/api/v1/avisos")
    @Transactional(readOnly = true)
    CaixaDeAvisos listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return new CaixaDeAvisos(avisos.countByUsuarioIdAndLidoEmIsNull(usuario.id()),
                avisos.findByUsuarioIdOrderByCriadoEmDesc(usuario.id(), Limit.of(MOSTRADOS)).stream()
                        .map(a -> new AvisoDto(a.getId(), a.getTitulo(), a.getMensagem(), a.getLink().orElse(null),
                                a.getLidoEm().isPresent(), a.getCriadoEm()))
                        .toList());
    }

    /** Marca todos como lidos (ao abrir a página de avisos). */
    @PostMapping("/api/v1/avisos/lidos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void marcarLidos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        Instant agora = Instant.now();
        avisos.findByUsuarioIdAndLidoEmIsNull(usuario.id()).forEach(a -> {
            a.marcarLido(agora);
            avisos.save(a);
        });
    }
}
