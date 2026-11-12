package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.academico.AlunoResumo;
import br.edu.univassouras.sereno.academico.AlunoService;
import br.edu.univassouras.sereno.calculo.SaldoHoras;
import br.edu.univassouras.sereno.certificados.armazenamento.ArmazenamentoArquivos;
import br.edu.univassouras.sereno.certificados.armazenamento.TipoArquivo;
import br.edu.univassouras.sereno.comum.ErroDeNegocio;
import br.edu.univassouras.sereno.extracao.ExtracaoService;
import br.edu.univassouras.sereno.extracao.ResultadoExtracao;
import br.edu.univassouras.sereno.regulamento.Catalogo;
import br.edu.univassouras.sereno.regulamento.CatalogoService;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso do aluno sobre os próprios certificados. */
@Service
class CertificadoService {

    static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;

    private final CertificadoRepository certificados;
    private final ArquivoRepository arquivos;
    private final ArmazenamentoArquivos armazenamento;
    private final AlunoService alunos;
    private final CatalogoService catalogo;
    private final SaldoService saldo;
    private final ExtracaoService extracao;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    CertificadoService(CertificadoRepository certificados, ArquivoRepository arquivos, ArmazenamentoArquivos armazenamento,
            AlunoService alunos, CatalogoService catalogo, SaldoService saldo, ExtracaoService extracao,
            ApplicationEventPublisher eventos, Optional<Clock> relogio) {
        this.certificados = certificados;
        this.arquivos = arquivos;
        this.armazenamento = armazenamento;
        this.alunos = alunos;
        this.catalogo = catalogo;
        this.saldo = saldo;
        this.extracao = extracao;
        this.eventos = eventos;
        this.relogio = relogio.orElse(Clock.systemDefaultZone());
    }

    record ArquivoEnviado(String nomeOriginal, byte[] conteudo) {
    }

    record ConteudoArquivo(Resource recurso, String tipoMime, String nomeOriginal) {
    }

    @Transactional
    CertificadoDto lancar(UUID usuarioId, DadosCertificado dados, ArquivoEnviado enviado) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        Catalogo regulamento = catalogoDo(aluno);
        validar(dados, regulamento);
        Arquivo arquivo = receberArquivo(aluno, usuarioId, enviado);
        Certificado certificado = certificados.save(Certificado.lancadoPeloAluno(aluno.id(), arquivo, aluno.regulamentoId(), dados));
        return paraDtos(aluno, List.of(certificado), regulamento).getFirst();
    }

    /**
     * Recebe só o arquivo; a leitura automática roda depois do commit, em segundo plano, e o certificado fica
     * esperando a conferência do aluno.
     */
    @Transactional
    CertificadoDto enviar(UUID usuarioId, String nomeAluno, ArquivoEnviado enviado) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        Arquivo arquivo = receberArquivo(aluno, usuarioId, enviado);
        Certificado certificado = certificados.save(Certificado.enviado(aluno.id(), arquivo, aluno.regulamentoId()));
        eventos.publishEvent(new CertificadoEnviado(certificado.getId(), nomeAluno));
        return paraDtos(aluno, List.of(certificado), catalogoDo(aluno)).getFirst();
    }

    private Arquivo receberArquivo(AlunoResumo aluno, UUID usuarioId, ArquivoEnviado enviado) {
        if (enviado.conteudo().length == 0) {
            throw ErroDeNegocio.invalido("arquivo-vazio", "O arquivo está vazio.");
        }
        if (enviado.conteudo().length > TAMANHO_MAXIMO) {
            throw ErroDeNegocio.invalido("arquivo-grande", "O arquivo passa de 10 MB.");
        }
        TipoArquivo tipo = TipoArquivo.detectar(enviado.conteudo())
                .orElseThrow(() -> ErroDeNegocio.invalido("arquivo-tipo", "Envie um PDF, JPG ou PNG."));
        String sha256 = sha256(enviado.conteudo());
        if (certificados.existeArquivoAtivo(aluno.id(), sha256)) {
            throw ErroDeNegocio.conflito("arquivo-duplicado", "Este arquivo já foi enviado em outro certificado.");
        }

        String chave = armazenamento.guardar(enviado.conteudo(), tipo);
        return arquivos.save(new Arquivo(chave, nomeSeguro(enviado.nomeOriginal(), tipo), tipo.mime(),
                enviado.conteudo().length, sha256, usuarioId));
    }

    @Transactional(readOnly = true)
    List<CertificadoDto> listar(UUID usuarioId) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        return paraDtos(aluno, certificados.ativosDoAluno(aluno.id()), catalogoDo(aluno));
    }

    @Transactional(readOnly = true)
    CertificadoDto detalhar(UUID usuarioId, UUID id) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        return paraDtos(aluno, List.of(doAluno(aluno, id)), catalogoDo(aluno)).getFirst();
    }

    @Transactional
    CertificadoDto atualizar(UUID usuarioId, UUID id, long versaoEsperada, DadosCertificado dados) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        Catalogo regulamento = catalogoDo(aluno);
        Certificado certificado = doAluno(aluno, id);
        if (certificado.getVersao() != versaoEsperada) {
            throw ErroDeNegocio.conflito("versao", "Este certificado foi alterado em outra janela. Recarregue a página.");
        }
        validar(dados, regulamento);
        certificado.editarPeloAluno(dados);
        certificados.save(certificado);
        return paraDtos(aluno, List.of(certificado), regulamento).getFirst();
    }

    @Transactional
    void excluir(UUID usuarioId, UUID id) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        Certificado certificado = doAluno(aluno, id);
        certificado.excluirPeloAluno();
        certificados.save(certificado);
    }

    @Transactional(readOnly = true)
    ConteudoArquivo arquivo(UUID usuarioId, UUID id) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        Arquivo arquivo = doAluno(aluno, id).getArquivo();
        return new ConteudoArquivo(armazenamento.abrir(arquivo.getChave()), arquivo.getTipoMime(), arquivo.getNomeOriginal());
    }

    @Transactional(readOnly = true)
    SimulacaoDto simular(UUID usuarioId, int item, BigDecimal horas, Optional<LocalDate> data, Optional<UUID> ignorar) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        Catalogo regulamento = catalogoDo(aluno);
        Catalogo.ItemComMacro itemDoCatalogo = regulamento.item(item)
                .orElseThrow(() -> ErroDeNegocio.invalido("item-invalido", "Item " + item + " não existe no seu regulamento."));
        List<Certificado> existentes = certificados.ativosDoAluno(aluno.id()).stream()
                .filter(c -> ignorar.map(id -> !id.equals(c.getId())).orElse(true))
                .toList();
        SaldoHoras.Detalhe detalhe = saldo.simular(aluno.regulamentoId(), existentes, item, horas, data);
        return new SimulacaoDto(ItemResumo.de(itemDoCatalogo), detalhe.horasBrutas(), detalhe.horasContabilizadas(), detalhe.cortes());
    }

    @Transactional(readOnly = true)
    SaldoDto saldo(UUID usuarioId) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        Catalogo regulamento = catalogoDo(aluno);
        List<Certificado> ativos = certificados.ativosDoAluno(aluno.id());
        return SaldoDto.de(saldo.calcular(aluno.regulamentoId(), ativos), regulamento, ativos.stream()
                .collect(Collectors.groupingBy(Certificado::getStatus, Collectors.counting())));
    }

    private List<CertificadoDto> paraDtos(AlunoResumo aluno, List<Certificado> lista, Catalogo regulamento) {
        Map<UUID, SaldoHoras.Detalhe> detalhes = saldo.calcular(aluno.regulamentoId(), certificados.ativosDoAluno(aluno.id()))
                .detalhes().stream()
                .collect(Collectors.toMap(SaldoHoras.Detalhe::certificadoId, Function.identity()));
        Map<UUID, ResultadoExtracao> leituras = extracao.ultimas(lista.stream().map(Certificado::getId).toList());
        return lista.stream()
                .map(c -> CertificadoDto.de(c, regulamento,
                        Optional.ofNullable(detalhes.get(c.getId())), Optional.ofNullable(leituras.get(c.getId()))))
                .toList();
    }

    private Certificado doAluno(AlunoResumo aluno, UUID id) {
        // Certificado de outro aluno responde 404, igual a um inexistente: não revela que o id existe.
        return certificados.findById(id)
                .filter(c -> c.pertenceA(aluno.id()))
                .orElseThrow(() -> ErroDeNegocio.naoEncontrado("Certificado não encontrado."));
    }

    private Catalogo catalogoDo(AlunoResumo aluno) {
        return catalogo.catalogo(aluno.regulamentoId())
                .orElseThrow(() -> new IllegalStateException("Regulamento do aluno não existe: " + aluno.regulamentoId()));
    }

    private void validar(DadosCertificado dados, Catalogo regulamento) {
        if (regulamento.item(dados.itemNumero()).isEmpty()) {
            throw ErroDeNegocio.invalido("item-invalido", "Item " + dados.itemNumero() + " não existe no seu regulamento.");
        }
        LocalDate hoje = LocalDate.now(relogio);
        if ((dados.dataInicio() != null && dados.dataInicio().isAfter(hoje)) || (dados.dataFim() != null && dados.dataFim().isAfter(hoje))) {
            throw ErroDeNegocio.invalido("data-futura", "A atividade não pode ter data no futuro.");
        }
        if (dados.dataInicio() != null && dados.dataFim() != null && dados.dataFim().isBefore(dados.dataInicio())) {
            throw ErroDeNegocio.invalido("datas-invertidas", "A data de término é anterior à de início.");
        }
    }

    /** Nome só para exibição e download: sem caminho, sem caracteres de controle, com a extensão real. */
    static String nomeSeguro(String original, TipoArquivo tipo) {
        String base = original == null ? "" : original.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "").strip();
        int ponto = base.lastIndexOf('.');
        if (ponto > 0) {
            base = base.substring(0, ponto);
        }
        if (base.isBlank()) {
            base = "certificado";
        }
        if (base.length() > 200) {
            base = base.substring(0, 200);
        }
        return base + "." + tipo.extensao();
    }

    private static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
