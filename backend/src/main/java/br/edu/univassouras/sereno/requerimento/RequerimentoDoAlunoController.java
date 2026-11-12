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

/** O aluno gera e baixa o próprio requerimento oficial (mesmo modelo da coordenação, sem a parte do deferimento). */
@RestController
@RequestMapping("/api/v1/alunos/me/requerimentos")
@PreAuthorize("hasRole('ALUNO')")
class RequerimentoDoAlunoController {

    private final RequerimentoService servico;

    RequerimentoDoAlunoController(RequerimentoService servico) {
        this.servico = servico;
    }

    @GetMapping
    List<RequerimentoDto> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listarDoAluno(usuario.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RequerimentoDto gerar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.gerarPeloAluno(usuario.id(), usuario.nome());
    }

    @GetMapping("/{id}/pdf")
    ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return baixar(servico.arquivoDoAluno(usuario.id(), id, true), MediaType.APPLICATION_PDF);
    }

    @GetMapping("/{id}/xlsx")
    ResponseEntity<byte[]> xlsx(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return baixar(servico.arquivoDoAluno(usuario.id(), id, false), RequerimentoController.XLSX);
    }

    private static ResponseEntity<byte[]> baixar(RequerimentoService.ArquivoGerado arquivo, MediaType tipo) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nome(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(arquivo.conteudo());
    }
}
