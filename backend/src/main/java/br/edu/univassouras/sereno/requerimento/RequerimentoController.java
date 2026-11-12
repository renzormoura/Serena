package br.edu.univassouras.sereno.requerimento;

import br.edu.univassouras.sereno.identidade.UsuarioAutenticado;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coordenacao")
@PreAuthorize("hasAnyRole('COORDENADOR', 'ADMIN')")
class RequerimentoController {

    static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final RequerimentoService servico;

    RequerimentoController(RequerimentoService servico) {
        this.servico = servico;
    }

    @GetMapping("/alunos/{alunoId}/requerimentos")
    List<RequerimentoDto> listar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID alunoId) {
        return servico.listar(coordenador(usuario), alunoId);
    }

    /** Gera um requerimento novo com as horas deferidas até agora. */
    @PostMapping("/alunos/{alunoId}/requerimentos")
    @ResponseStatus(HttpStatus.CREATED)
    RequerimentoDto gerar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID alunoId) {
        return servico.gerar(coordenador(usuario), alunoId);
    }

    @GetMapping("/requerimentos/{id}/pdf")
    ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return baixar(servico.pdf(coordenador(usuario), id), MediaType.APPLICATION_PDF);
    }

    @GetMapping("/requerimentos/{id}/xlsx")
    ResponseEntity<byte[]> xlsx(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return baixar(servico.xlsx(coordenador(usuario), id), XLSX);
    }

    private static ResponseEntity<byte[]> baixar(RequerimentoService.ArquivoGerado arquivo, MediaType tipo) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nome(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(arquivo.conteudo());
    }

    private static RequerimentoService.Coordenador coordenador(UsuarioAutenticado usuario) {
        return new RequerimentoService.Coordenador(usuario.id(), usuario.nome(), usuario.papel());
    }
}
