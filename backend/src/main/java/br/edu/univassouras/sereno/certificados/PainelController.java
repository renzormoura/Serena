package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.identidade.UsuarioAutenticado;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Painel da coordenação e a exportação dele. Os filtros valem para os dois. */
@RestController
@RequestMapping("/api/v1/coordenacao/painel")
@PreAuthorize("hasAnyRole('COORDENADOR', 'ADMIN')")
class PainelController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PainelService servico;

    PainelController(PainelService servico) {
        this.servico = servico;
    }

    /**
     * @param curso     só os alunos deste curso
     * @param ingresso  só os alunos que ingressaram neste ano
     */
    @GetMapping
    PainelDto painel(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) UUID curso, @RequestParam(required = false) Integer ingresso) {
        return servico.painel(avaliador(usuario), new PainelService.Filtro(Optional.ofNullable(curso), Optional.ofNullable(ingresso)));
    }

    @GetMapping("/xlsx")
    ResponseEntity<byte[]> exportar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) UUID curso, @RequestParam(required = false) Integer ingresso) {
        byte[] conteudo = ExportacaoPainel.xlsx(servico.dados(avaliador(usuario),
                new PainelService.Filtro(Optional.ofNullable(curso), Optional.ofNullable(ingresso))));
        String nome = "painel-coordenacao-" + LocalDate.now(ZoneId.of("America/Sao_Paulo")) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(conteudo);
    }

    private static CoordenacaoService.Avaliador avaliador(UsuarioAutenticado usuario) {
        return new CoordenacaoService.Avaliador(usuario.id(), usuario.papel());
    }
}
