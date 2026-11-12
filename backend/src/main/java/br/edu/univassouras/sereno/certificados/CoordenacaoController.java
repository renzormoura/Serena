package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.comum.ErroDeNegocio;
import br.edu.univassouras.sereno.identidade.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Área da coordenação: fila de análise, decisões e alunos. */
@RestController
@RequestMapping("/api/v1/coordenacao")
@PreAuthorize("hasAnyRole('COORDENADOR', 'ADMIN')")
class CoordenacaoController {

    static final int LOTE_MAXIMO = 100;

    private final CoordenacaoService servico;

    CoordenacaoController(CoordenacaoService servico) {
        this.servico = servico;
    }

    /**
     * @param justificativa obrigatória para indeferir, pedir ajuste, reabrir e para corrigir item ou horas
     * @param itemNumero    só ao deferir: corrige o item escolhido pelo aluno
     * @param horasCertificado só ao deferir: corrige a carga horária
     */
    record DecisaoForm(
            @NotNull Long versao,
            @NotNull(message = "Escolha a decisão.") CoordenacaoService.Decisao decisao,
            @Size(max = 1000, message = "Use no máximo 1000 caracteres.") String justificativa,
            Integer itemNumero,
            @DecimalMin(value = "0", message = "A carga horária não pode ser negativa.") BigDecimal horasCertificado) {
    }

    record ExclusaoForm(@NotNull Long versao,
            @Size(max = 1000, message = "Use no máximo 1000 caracteres.") String justificativa) {
    }

    /** A coordenação exclui um certificado do aluno. O motivo é obrigatório e vai para o aluno. */
    @PostMapping("/certificados/{id}/exclusao")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void excluir(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id,
            @Valid @RequestBody ExclusaoForm form) {
        servico.excluir(avaliador(usuario), id, form.versao(), Optional.ofNullable(form.justificativa()));
    }

    record AlvoLote(@NotNull UUID id, @NotNull Long versao) {
    }

    record LoteForm(@NotEmpty @Size(max = LOTE_MAXIMO) List<@Valid AlvoLote> certificados) {
    }

    record SimulacaoCoordenacaoForm(
            @NotNull(message = "Escolha o item.") Integer itemNumero,
            @NotNull(message = "Informe a carga horária.") @DecimalMin("0") BigDecimal horasCertificado) {
    }

    @GetMapping("/fila")
    List<ItemDaFila> fila(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.fila(avaliador(usuario));
    }

    @GetMapping("/certificados/{id}")
    AnaliseDto analisar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return servico.analisar(avaliador(usuario), id);
    }

    @PostMapping("/certificados/{id}/decisao")
    AnaliseDto decidir(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id,
            @Valid @RequestBody DecisaoForm form) {
        if (form.decisao() != CoordenacaoService.Decisao.DEFERIR && (form.itemNumero() != null || form.horasCertificado() != null)) {
            throw ErroDeNegocio.invalido("correcao-so-ao-deferir", "Item e horas só podem ser corrigidos ao deferir.");
        }
        return servico.decidir(avaliador(usuario), id, form.versao(), new CoordenacaoService.DadosDecisao(form.decisao(),
                Optional.ofNullable(form.justificativa()), Optional.ofNullable(form.itemNumero()),
                Optional.ofNullable(form.horasCertificado())));
    }

    /**
     * Defere vários certificados de uma vez, sem correções. Cada um é decidido à parte: um que mudou ou já foi
     * decidido não impede os outros.
     */
    @PostMapping("/certificados/deferimentos")
    List<ResultadoLote> deferirEmLote(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody LoteForm form) {
        CoordenacaoService.Avaliador quem = avaliador(usuario);
        List<ResultadoLote> resultados = new ArrayList<>();
        for (AlvoLote alvo : form.certificados()) {
            try {
                servico.decidir(quem, alvo.id(), alvo.versao(), new CoordenacaoService.DadosDecisao(
                        CoordenacaoService.Decisao.DEFERIR, Optional.empty(), Optional.empty(), Optional.empty()));
                resultados.add(new ResultadoLote(alvo.id(), true, null, null));
            } catch (ErroDeNegocio erro) {
                resultados.add(new ResultadoLote(alvo.id(), false, erro.codigo(), erro.getBody().getDetail()));
            } catch (OptimisticLockingFailureException erro) {
                resultados.add(new ResultadoLote(alvo.id(), false, "versao", "Este certificado mudou enquanto você decidia."));
            }
        }
        return resultados;
    }

    @PostMapping("/certificados/{id}/simular")
    SimulacaoDto simular(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id,
            @Valid @RequestBody SimulacaoCoordenacaoForm form) {
        return servico.simular(avaliador(usuario), id, form.itemNumero(), form.horasCertificado());
    }

    @GetMapping("/certificados/{id}/arquivo")
    ResponseEntity<Resource> arquivo(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        CertificadoService.ConteudoArquivo conteudo = servico.arquivo(avaliador(usuario), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(conteudo.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(conteudo.nomeOriginal(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(conteudo.recurso());
    }

    @GetMapping("/alunos")
    List<AlunoNaCoordenacao> alunos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.alunos(avaliador(usuario));
    }

    @GetMapping("/alunos/{id}")
    FichaAlunoDto ficha(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return servico.ficha(avaliador(usuario), id);
    }

    /** Ano e semestre de ingresso: os dois, ou nenhum (apaga). */
    record IngressoForm(
            @jakarta.validation.constraints.Min(value = 1990, message = "Ano inválido.")
            @jakarta.validation.constraints.Max(value = 2100, message = "Ano inválido.") Integer anoIngresso,
            @jakarta.validation.constraints.Min(value = 1, message = "Semestre deve ser 1 ou 2.")
            @jakarta.validation.constraints.Max(value = 2, message = "Semestre deve ser 1 ou 2.") Integer semestreIngresso) {
    }

    @org.springframework.web.bind.annotation.PutMapping("/alunos/{id}/ingresso")
    FichaAlunoDto definirIngresso(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id,
            @Valid @RequestBody IngressoForm form) {
        return servico.definirIngresso(avaliador(usuario), id, form.anoIngresso(), form.semestreIngresso());
    }

    private static CoordenacaoService.Avaliador avaliador(UsuarioAutenticado usuario) {
        return new CoordenacaoService.Avaliador(usuario.id(), usuario.papel());
    }
}
