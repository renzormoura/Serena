package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.academico.AlunoComConta;
import br.edu.univassouras.sereno.academico.AlunoResumo;
import br.edu.univassouras.sereno.academico.AlunoService;
import br.edu.univassouras.sereno.calculo.SaldoHoras;
import br.edu.univassouras.sereno.certificados.armazenamento.ArmazenamentoArquivos;
import br.edu.univassouras.sereno.classificacao.Confianca;
import br.edu.univassouras.sereno.comum.ErroDeNegocio;
import br.edu.univassouras.sereno.extracao.ExtracaoService;
import br.edu.univassouras.sereno.extracao.ResultadoExtracao;
import br.edu.univassouras.sereno.identidade.ContasService;
import br.edu.univassouras.sereno.identidade.Papel;
import br.edu.univassouras.sereno.regulamento.Catalogo;
import br.edu.univassouras.sereno.regulamento.CatalogoService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso da coordenação: fila de análise, decisões, lista e ficha dos alunos. O escopo (quais alunos o
 * coordenador enxerga) vem do módulo acadêmico e é checado em toda operação.
 */
@Service
class CoordenacaoService {

    /** Quem está agindo. */
    record Avaliador(UUID id, Papel papel) {
    }

    enum Decisao {
        DEFERIR,
        INDEFERIR,
        SOLICITAR_AJUSTE,
        REABRIR
    }

    /** Correções opcionais da coordenação ao deferir. */
    record DadosDecisao(Decisao decisao, Optional<String> justificativa, Optional<Integer> itemNumero,
            Optional<BigDecimal> horasCertificado) {
    }

    private final CertificadoRepository certificados;
    private final AvaliacaoRepository avaliacoes;
    private final AlunoService alunos;
    private final ContasService contas;
    private final CatalogoService catalogo;
    private final SaldoService saldo;
    private final ExtracaoService extracao;
    private final ArmazenamentoArquivos armazenamento;
    private final ApplicationEventPublisher eventos;
    private final java.time.Clock relogio;
    private final TriagemDeCertificados triagem;

    CoordenacaoService(CertificadoRepository certificados, AvaliacaoRepository avaliacoes, AlunoService alunos,
            ContasService contas, CatalogoService catalogo, SaldoService saldo, ExtracaoService extracao,
            ArmazenamentoArquivos armazenamento, ApplicationEventPublisher eventos, Optional<java.time.Clock> relogio,
            TriagemDeCertificados triagem) {
        this.triagem = triagem;
        this.relogio = relogio.orElse(java.time.Clock.systemUTC());
        this.certificados = certificados;
        this.avaliacoes = avaliacoes;
        this.alunos = alunos;
        this.contas = contas;
        this.catalogo = catalogo;
        this.saldo = saldo;
        this.extracao = extracao;
        this.armazenamento = armazenamento;
        this.eventos = eventos;
    }

    /**
     * Certificados conferidos pelos alunos, esperando decisão. Primeiro os que pedem mais atenção (sugestão de
     * item fraca ou trocada pelo aluno), depois os mais antigos.
     */
    @Transactional(readOnly = true)
    List<ItemDaFila> fila(Avaliador quem) {
        Map<UUID, AlunoComConta> visiveis = porId(alunos.visiveis(quem.id(), quem.papel()));
        if (visiveis.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<Certificado>> porAluno = certificados.ativosDosAlunos(visiveis.keySet()).stream()
                .collect(Collectors.groupingBy(Certificado::getAlunoId));
        List<Certificado> naFila = porAluno.values().stream()
                .flatMap(List::stream)
                .filter(c -> c.getStatus() == StatusCertificado.CONFIRMADO)
                .toList();
        Map<UUID, CertificadoDto> dtos = dtosPorAluno(naFila, porAluno, visiveis);
        Map<UUID, List<TriagemDeCertificados.Alerta>> alertas = triar(naFila, porAluno, visiveis);
        return naFila.stream()
                .map(c -> new ItemDaFila(dtos.get(c.getId()), AlunoNaFila.de(visiveis.get(c.getAlunoId())),
                        c.getConfirmadoEm().orElse(null), prioridade(alertas.get(c.getId())), trocouSugestao(c),
                        alertas.get(c.getId())))
                .sorted(Comparator.comparingInt(ItemDaFila::prioridade)
                        .thenComparing(i -> i.enviadoParaAnalise() == null ? Instant.MIN : i.enviadoParaAnalise()))
                .toList();
    }

    @Transactional(readOnly = true)
    AnaliseDto analisar(Avaliador quem, UUID certificadoId) {
        Certificado certificado = visivel(quem, certificadoId);
        return analise(certificado, alunos.visivel(quem.id(), quem.papel(), certificado.getAlunoId()));
    }

    @Transactional
    AnaliseDto decidir(Avaliador quem, UUID certificadoId, long versaoEsperada, DadosDecisao dados) {
        Certificado certificado = visivel(quem, certificadoId);
        AlunoComConta aluno = alunos.visivel(quem.id(), quem.papel(), certificado.getAlunoId());
        if (certificado.getVersao() != versaoEsperada) {
            throw ErroDeNegocio.conflito("versao", "Este certificado mudou desde que você abriu. Recarregue a página.");
        }
        Optional<String> justificativa = dados.justificativa().map(String::strip).filter(j -> !j.isEmpty());
        dados.itemNumero().ifPresent(n -> catalogoDo(aluno.aluno()).item(n)
                .orElseThrow(() -> ErroDeNegocio.invalido("item-invalido", "Item " + n + " não existe no regulamento do aluno.")));
        Avaliacao avaliacao = switch (dados.decisao()) {
            case DEFERIR -> certificado.deferir(quem.id(), dados.itemNumero(), dados.horasCertificado(), justificativa);
            case INDEFERIR -> certificado.indeferir(quem.id(), justificativa);
            case SOLICITAR_AJUSTE -> certificado.solicitarAjuste(quem.id(), justificativa);
            case REABRIR -> certificado.reabrir(quem.id(), justificativa);
        };
        certificados.save(certificado);
        avaliacoes.save(avaliacao);
        eventos.publishEvent(new CertificadoAvaliado(certificado.getId(), avaliacao.getDecisao(),
                certificado.getTitulo().orElse(certificado.getArquivo().getNomeOriginal()),
                avaliacao.getJustificativa().orElse(null), aluno.aluno().usuarioId(), aluno.nome(), aluno.email()));
        return analise(certificado, aluno);
    }

    /** Exclui o certificado do aluno, com motivo; o aluno é avisado. */
    @Transactional
    void excluir(Avaliador quem, UUID certificadoId, long versaoEsperada, Optional<String> justificativa) {
        Certificado certificado = visivel(quem, certificadoId);
        AlunoComConta aluno = alunos.visivel(quem.id(), quem.papel(), certificado.getAlunoId());
        if (certificado.getVersao() != versaoEsperada) {
            throw ErroDeNegocio.conflito("versao", "Este certificado mudou desde que você abriu. Recarregue a página.");
        }
        Avaliacao avaliacao = certificado.excluirPelaCoordenacao(quem.id(),
                justificativa.map(String::strip).filter(j -> !j.isEmpty()));
        certificados.save(certificado);
        avaliacoes.save(avaliacao);
        eventos.publishEvent(new CertificadoAvaliado(certificado.getId(), TipoAvaliacao.EXCLUIDO,
                certificado.getTitulo().orElse(certificado.getArquivo().getNomeOriginal()),
                avaliacao.getJustificativa().orElse(null), aluno.aluno().usuarioId(), aluno.nome(), aluno.email()));
    }

    @Transactional(readOnly = true)
    CertificadoService.ConteudoArquivo arquivo(Avaliador quem, UUID certificadoId) {
        Arquivo arquivo = visivel(quem, certificadoId).getArquivo();
        return new CertificadoService.ConteudoArquivo(armazenamento.abrir(arquivo.getChave()), arquivo.getTipoMime(),
                arquivo.getNomeOriginal());
    }

    /** Quanto o certificado contaria com outro item ou outras horas, antes de a coordenação corrigir. */
    @Transactional(readOnly = true)
    SimulacaoDto simular(Avaliador quem, UUID certificadoId, int item, BigDecimal horas) {
        Certificado certificado = visivel(quem, certificadoId);
        AlunoResumo aluno = alunos.visivel(quem.id(), quem.papel(), certificado.getAlunoId()).aluno();
        Catalogo.ItemComMacro itemDoCatalogo = catalogoDo(aluno).item(item)
                .orElseThrow(() -> ErroDeNegocio.invalido("item-invalido", "Item " + item + " não existe no regulamento do aluno."));
        List<Certificado> outros = certificados.ativosDoAluno(aluno.id()).stream()
                .filter(c -> !c.getId().equals(certificadoId))
                .toList();
        SaldoHoras.Detalhe detalhe = saldo.simular(aluno.regulamentoId(), outros, item, horas,
                certificado.getDataInicio().or(certificado::getDataFim));
        return new SimulacaoDto(ItemResumo.de(itemDoCatalogo), detalhe.horasBrutas(), detalhe.horasContabilizadas(), detalhe.cortes());
    }

    @Transactional(readOnly = true)
    List<AlunoNaCoordenacao> alunos(Avaliador quem) {
        List<AlunoComConta> visiveis = alunos.visiveis(quem.id(), quem.papel());
        if (visiveis.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<Certificado>> porAluno = certificados.ativosDosAlunos(visiveis.stream().map(a -> a.aluno().id()).toList())
                .stream()
                .collect(Collectors.groupingBy(Certificado::getAlunoId));
        return visiveis.stream()
                .map(a -> resumo(a, porAluno.getOrDefault(a.aluno().id(), List.of())))
                .sorted(Comparator.comparing(AlunoNaCoordenacao::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    FichaAlunoDto ficha(Avaliador quem, UUID alunoId) {
        AlunoComConta aluno = alunos.visivel(quem.id(), quem.papel(), alunoId);
        List<Certificado> ativos = certificados.ativosDoAluno(alunoId);
        Catalogo regulamento = catalogoDo(aluno.aluno());
        return new FichaAlunoDto(resumo(aluno, ativos), saldoDto(aluno.aluno(), ativos, regulamento),
                dtos(aluno.aluno(), ativos, ativos, regulamento));
    }

    /** A coordenação informa (ou apaga) o ingresso do aluno; devolve a ficha atualizada. */
    @Transactional
    FichaAlunoDto definirIngresso(Avaliador quem, UUID alunoId, Integer ano, Integer semestre) {
        alunos.definirIngresso(quem.id(), quem.papel(), alunoId, ano, semestre);
        return ficha(quem, alunoId);
    }

    // ---- auxiliares ----

    private AnaliseDto analise(Certificado certificado, AlunoComConta aluno) {
        List<Certificado> ativos = certificados.ativosDoAluno(aluno.aluno().id());
        Catalogo regulamento = catalogoDo(aluno.aluno());
        List<Avaliacao> historico = avaliacoes.findByCertificadoIdOrderByCriadoEmDesc(certificado.getId());
        Map<UUID, ContasService.ContaResumo> avaliadores = contas.contas(
                historico.stream().map(Avaliacao::getAvaliadorId).collect(Collectors.toSet()));
        return new AnaliseDto(
                dtos(aluno.aluno(), ativos, List.of(certificado), regulamento).getFirst(),
                AlunoNaFila.de(aluno),
                saldoDto(aluno.aluno(), ativos, regulamento),
                historico.stream()
                        .map(a -> new AvaliacaoDto(a.getDecisao(), a.getJustificativa().orElse(null),
                                Optional.ofNullable(avaliadores.get(a.getAvaliadorId())).map(ContasService.ContaResumo::nome).orElse("—"),
                                a.getItemAnterior().orElse(null), a.getItemNovo().orElse(null),
                                a.getHorasAnteriores().orElse(null), a.getHorasNovas().orElse(null), a.getCriadoEm()))
                        .toList(),
                triar(List.of(certificado), Map.of(aluno.aluno().id(), ativos), Map.of(aluno.aluno().id(), aluno))
                        .get(certificado.getId()));
    }

    private AlunoNaCoordenacao resumo(AlunoComConta a, List<Certificado> ativos) {
        SaldoHoras s = saldo.calcular(a.aluno().regulamentoId(), ativos);
        Map<StatusCertificado, Long> contagem = contagem(ativos);
        return new AlunoNaCoordenacao(a.aluno().id(), a.nome(), a.email(), a.aluno().matricula(), a.aluno().curso(),
                a.aluno().anoIngresso(), a.aluno().semestreIngresso(),
                a.aluno().periodoEm(java.time.LocalDate.now(relogio.withZone(java.time.ZoneId.of("America/Sao_Paulo")))).orElse(null),
                s.integralizadas(), s.pendentes(),
                s.exigidas().orElse(null), s.percentual().orElse(null),
                contagem.getOrDefault(StatusCertificado.CONFIRMADO, 0L),
                contagem.getOrDefault(StatusCertificado.AJUSTE_SOLICITADO, 0L));
    }

    private SaldoDto saldoDto(AlunoResumo aluno, List<Certificado> ativos, Catalogo regulamento) {
        return SaldoDto.de(saldo.calcular(aluno.regulamentoId(), ativos), regulamento, contagem(ativos));
    }

    private static Map<StatusCertificado, Long> contagem(List<Certificado> ativos) {
        Map<StatusCertificado, Long> contagem = new EnumMap<>(StatusCertificado.class);
        ativos.forEach(c -> contagem.merge(c.getStatus(), 1L, Long::sum));
        return contagem;
    }

    /** DTOs de vários alunos de uma vez; o saldo (horas que contam) é calculado aluno a aluno. */
    private Map<UUID, CertificadoDto> dtosPorAluno(List<Certificado> selecionados, Map<UUID, List<Certificado>> ativosPorAluno,
            Map<UUID, AlunoComConta> visiveis) {
        return selecionados.stream()
                .collect(Collectors.groupingBy(Certificado::getAlunoId))
                .entrySet().stream()
                .flatMap(e -> {
                    AlunoResumo aluno = visiveis.get(e.getKey()).aluno();
                    return dtos(aluno, ativosPorAluno.get(e.getKey()), e.getValue(), catalogoDo(aluno)).stream();
                })
                .collect(Collectors.toMap(CertificadoDto::id, Function.identity()));
    }

    private List<CertificadoDto> dtos(AlunoResumo aluno, List<Certificado> ativos, List<Certificado> selecionados,
            Catalogo regulamento) {
        Map<UUID, SaldoHoras.Detalhe> detalhes = saldo.calcular(aluno.regulamentoId(), ativos).detalhes().stream()
                .collect(Collectors.toMap(SaldoHoras.Detalhe::certificadoId, Function.identity()));
        Map<UUID, ResultadoExtracao> leituras = extracao.ultimas(selecionados.stream().map(Certificado::getId).toList());
        return selecionados.stream()
                .map(c -> CertificadoDto.de(c, regulamento, Optional.ofNullable(detalhes.get(c.getId())),
                        Optional.ofNullable(leituras.get(c.getId()))))
                .toList();
    }

    private Certificado visivel(Avaliador quem, UUID certificadoId) {
        Certificado certificado = certificados.findById(certificadoId)
                .filter(c -> c.pertenceA(c.getAlunoId()))
                .orElseThrow(() -> ErroDeNegocio.naoEncontrado("Certificado não encontrado."));
        // Lança 404 se o aluno estiver fora do escopo do coordenador.
        alunos.visivel(quem.id(), quem.papel(), certificado.getAlunoId());
        return certificado;
    }

    /** 0 com alerta forte, 1 com alerta de atenção, 2 sem alertas (só informação ou nada). */
    private static int prioridade(List<TriagemDeCertificados.Alerta> alertas) {
        return alertas.stream()
                .mapToInt(a -> switch (a.gravidade()) {
                    case FORTE -> 0;
                    case ATENCAO -> 1;
                    case INFORMACAO -> 2;
                })
                .min()
                .orElse(2);
    }

    /** Triagem automática dos certificados selecionados, com o contexto de cada aluno. */
    private Map<UUID, List<TriagemDeCertificados.Alerta>> triar(List<Certificado> selecionados,
            Map<UUID, List<Certificado>> ativosPorAluno, Map<UUID, AlunoComConta> alunosPorId) {
        if (selecionados.isEmpty()) {
            return Map.of();
        }
        java.util.Set<String> compartilhados = new java.util.HashSet<>(certificados.arquivosEmMaisDeUmaConta(
                selecionados.stream().map(c -> c.getArquivo().getSha256()).toList()));
        Map<UUID, ResultadoExtracao> leituras = extracao.ultimas(selecionados.stream().map(Certificado::getId).toList());
        java.time.LocalDate hoje = java.time.LocalDate.now(relogio.withZone(java.time.ZoneId.of("America/Sao_Paulo")));
        Map<UUID, List<TriagemDeCertificados.Alerta>> resultado = new java.util.HashMap<>();
        selecionados.stream()
                .collect(Collectors.groupingBy(Certificado::getAlunoId))
                .forEach((alunoId, doAluno) -> {
                    AlunoResumo aluno = alunosPorId.get(alunoId).aluno();
                    List<Certificado> ativos = ativosPorAluno.getOrDefault(alunoId, List.of());
                    Map<UUID, SaldoHoras.Detalhe> detalhes = saldo.calcular(aluno.regulamentoId(), ativos).detalhes().stream()
                            .collect(Collectors.toMap(SaldoHoras.Detalhe::certificadoId, Function.identity()));
                    Optional<java.time.LocalDate> ingresso = Optional.ofNullable(aluno.anoIngresso())
                            .map(ano -> java.time.LocalDate.of(ano, aluno.semestreIngresso() == 2 ? 7 : 1, 1));
                    for (Certificado c : doAluno) {
                        resultado.put(c.getId(), triagem.avaliar(new TriagemDeCertificados.Contexto(c,
                                Optional.ofNullable(leituras.get(c.getId())), Optional.ofNullable(detalhes.get(c.getId())),
                                ativos, compartilhados.contains(c.getArquivo().getSha256()), ingresso), hoje));
                    }
                });
        return resultado;
    }

    private static boolean trocouSugestao(Certificado c) {
        return c.getItemSugerido().isPresent() && !Objects.equals(c.getItemSugerido().get(), c.getItemNumero().orElse(null));
    }

    private Catalogo catalogoDo(AlunoResumo aluno) {
        return catalogo.catalogo(aluno.regulamentoId())
                .orElseThrow(() -> new IllegalStateException("Regulamento do aluno não existe: " + aluno.regulamentoId()));
    }

    private static Map<UUID, AlunoComConta> porId(List<AlunoComConta> lista) {
        return lista.stream().collect(Collectors.toMap(a -> a.aluno().id(), Function.identity()));
    }
}
