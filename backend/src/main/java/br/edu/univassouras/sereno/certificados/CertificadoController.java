package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.comum.ErroDeNegocio;
import br.edu.univassouras.sereno.identidade.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ALUNO')")
class CertificadoController {

    static final int ENVIOS_POR_VEZ = 10;

    private final CertificadoService servico;

    CertificadoController(CertificadoService servico) {
        this.servico = servico;
    }

    record EdicaoForm(@NotNull Long versao, @NotNull @Valid DadosCertificado dados) {
    }

    record SimulacaoForm(
            @NotNull(message = "Escolha o item.") Integer itemNumero,
            @NotNull(message = "Informe a carga horária.") @DecimalMin("0") BigDecimal horasCertificado,
            LocalDate data,
            UUID ignorarCertificado) {
    }

    @PostMapping(path = "/certificados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    CertificadoDto lancar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestPart("dados") DadosCertificado dados,
            @RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        if (arquivo.getSize() > CertificadoService.TAMANHO_MAXIMO) {
            throw ErroDeNegocio.invalido("arquivo-grande", "O arquivo passa de 10 MB.");
        }
        return servico.lancar(usuario.id(), dados,
                new CertificadoService.ArquivoEnviado(arquivo.getOriginalFilename(), arquivo.getBytes()));
    }

    /**
     * Envio de um ou mais arquivos. Cada arquivo é tratado à parte: um arquivo recusado (duplicado, tipo errado)
     * não impede os outros. A leitura automática roda em segundo plano; acompanhe pelo status.
     */
    @PostMapping(path = "/certificados/envios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    List<ResultadoEnvio> enviar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestPart("arquivos") List<MultipartFile> arquivos) throws IOException {
        if (arquivos.isEmpty() || arquivos.size() > ENVIOS_POR_VEZ) {
            throw ErroDeNegocio.invalido("quantidade-arquivos", "Envie de 1 a " + ENVIOS_POR_VEZ + " arquivos por vez.");
        }
        List<ResultadoEnvio> resultados = new ArrayList<>();
        for (MultipartFile arquivo : arquivos) {
            String nome = arquivo.getOriginalFilename() == null ? "arquivo" : arquivo.getOriginalFilename();
            try {
                if (arquivo.getSize() > CertificadoService.TAMANHO_MAXIMO) {
                    throw ErroDeNegocio.invalido("arquivo-grande", "O arquivo passa de 10 MB.");
                }
                CertificadoDto criado = servico.enviar(usuario.id(), usuario.nome(),
                        new CertificadoService.ArquivoEnviado(nome, arquivo.getBytes()));
                resultados.add(new ResultadoEnvio(nome, criado, null, null));
            } catch (ErroDeNegocio erro) {
                if (erro.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
                    throw erro;
                }
                resultados.add(new ResultadoEnvio(nome, null, erro.codigo(), erro.getBody().getDetail()));
            }
        }
        return resultados;
    }

    @GetMapping("/certificados")
    List<CertificadoDto> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.listar(usuario.id());
    }

    @GetMapping("/certificados/{id}")
    CertificadoDto detalhar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return servico.detalhar(usuario.id(), id);
    }

    @PutMapping("/certificados/{id}")
    CertificadoDto atualizar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id,
            @Valid @RequestBody EdicaoForm form) {
        return servico.atualizar(usuario.id(), id, form.versao(), form.dados());
    }

    @DeleteMapping("/certificados/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void excluir(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        servico.excluir(usuario.id(), id);
    }

    @GetMapping("/certificados/{id}/arquivo")
    ResponseEntity<Resource> arquivo(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        CertificadoService.ConteudoArquivo conteudo = servico.arquivo(usuario.id(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(conteudo.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(conteudo.nomeOriginal(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(conteudo.recurso());
    }

    @PostMapping("/certificados/simular")
    SimulacaoDto simular(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody SimulacaoForm form) {
        return servico.simular(usuario.id(), form.itemNumero(), form.horasCertificado(),
                Optional.ofNullable(form.data()), Optional.ofNullable(form.ignorarCertificado()));
    }

    @GetMapping("/alunos/me/saldo")
    SaldoDto saldo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.saldo(usuario.id());
    }
}
