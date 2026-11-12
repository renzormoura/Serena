package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.classificacao.Confianca;
import br.edu.univassouras.sereno.classificacao.SugestaoDeItem;
import br.edu.univassouras.sereno.comum.ErroDeNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Agregado certificado. Toda mudança de status passa por métodos daqui, nunca por setters. */
@Entity
@Table(name = "certificado")
class Certificado {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID alunoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arquivo_id")
    private Arquivo arquivo;

    @Column(nullable = false)
    private UUID regulamentoId;

    private Integer itemNumero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCertificado status;

    private String titulo;

    private String instituicao;

    private LocalDate dataInicio;

    private LocalDate dataFim;

    private BigDecimal horasCertificado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemClassificacao origemClassificacao;

    private Integer itemSugerido;

    @Enumerated(EnumType.STRING)
    private Confianca confiancaSugestao;

    @Column(nullable = false)
    private Integer[] alternativasSugeridas = new Integer[0];

    private String motivoSugestao;

    private Instant confirmadoEm;

    private Instant excluidoEm;

    private Instant avaliadoEm;

    private UUID avaliadoPor;

    private String observacaoCoordenacao;

    private String observacaoAluno;

    @Version
    private long versao;

    @Column(nullable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    protected Certificado() {
    }

    private static Certificado novo(UUID alunoId, Arquivo arquivo, UUID regulamentoId) {
        Certificado c = new Certificado();
        c.id = UUID.randomUUID();
        c.alunoId = alunoId;
        c.arquivo = arquivo;
        c.regulamentoId = regulamentoId;
        c.origemClassificacao = OrigemClassificacao.MANUAL;
        c.criadoEm = Instant.now();
        c.atualizadoEm = c.criadoEm;
        return c;
    }

    /** Lançamento com os dados já preenchidos pelo aluno: nasce confirmado. */
    static Certificado lancadoPeloAluno(UUID alunoId, Arquivo arquivo, UUID regulamentoId, DadosCertificado dados) {
        Certificado c = novo(alunoId, arquivo, regulamentoId);
        c.aplicar(dados);
        c.status = StatusCertificado.CONFIRMADO;
        c.confirmadoEm = c.criadoEm;
        return c;
    }

    /** Só o arquivo: os dados virão da leitura automática e da conferência do aluno. */
    static Certificado enviado(UUID alunoId, Arquivo arquivo, UUID regulamentoId) {
        Certificado c = novo(alunoId, arquivo, regulamentoId);
        c.status = StatusCertificado.PROCESSANDO;
        return c;
    }

    record Sugestao(Optional<String> titulo, Optional<String> instituicao, Optional<LocalDate> dataInicio,
            Optional<LocalDate> dataFim, Optional<BigDecimal> horas, SugestaoDeItem item) {
        static final Sugestao VAZIA = new Sugestao(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), SugestaoDeItem.NENHUMA);
    }

    /**
     * Resultado da leitura automática. Ignorado se o certificado não está mais esperando a leitura (excluído,
     * ou leitura repetida depois de uma queda).
     *
     * @return se a sugestão foi aplicada
     */
    boolean receberLeitura(Sugestao sugestao) {
        if (status != StatusCertificado.PROCESSANDO || excluidoEm != null) {
            return false;
        }
        titulo = sugestao.titulo().map(t -> t.length() > 300 ? t.substring(0, 300) : t).orElse(null);
        instituicao = sugestao.instituicao().orElse(null);
        dataInicio = sugestao.dataInicio().orElse(null);
        dataFim = sugestao.dataFim().orElse(null);
        horasCertificado = sugestao.horas().orElse(null);
        SugestaoDeItem item = sugestao.item();
        itemSugerido = item.principal().map(SugestaoDeItem.Candidato::numero).orElse(null);
        confiancaSugestao = item.principal().isPresent() ? item.confianca() : null;
        alternativasSugeridas = item.alternativas().toArray(Integer[]::new);
        motivoSugestao = item.principal().isPresent() ? limitar(item.motivo(), 500) : null;
        itemNumero = item.itemParaPreencher().orElse(null);
        if (itemNumero != null) {
            origemClassificacao = OrigemClassificacao.REGRA;
        }
        status = StatusCertificado.AGUARDANDO_CONFIRMACAO;
        atualizadoEm = Instant.now();
        return true;
    }

    /** Conferência ou edição pelo aluno. Depois disso o certificado vai (ou volta) para a fila da coordenação. */
    void editarPeloAluno(DadosCertificado dados) {
        if (!status.editavelPeloAluno()) {
            throw status == StatusCertificado.PROCESSANDO
                    ? ErroDeNegocio.conflito("em-leitura", "O certificado ainda está sendo lido. Aguarde alguns segundos.")
                    : ErroDeNegocio.conflito("nao-editavel", "Certificados já analisados pela coordenação não podem ser alterados.");
        }
        aplicar(dados);
        // Se o aluno ficou com o item sugerido, a classificação foi da regra; se trocou, foi manual.
        origemClassificacao = Objects.equals(itemSugerido, dados.itemNumero()) ? OrigemClassificacao.REGRA : OrigemClassificacao.MANUAL;
        status = StatusCertificado.CONFIRMADO;
        confirmadoEm = Instant.now();
    }

    void excluirPeloAluno() {
        if (!status.excluivelPeloAluno()) {
            throw ErroDeNegocio.conflito("nao-editavel", "Certificados já analisados pela coordenação não podem ser excluídos.");
        }
        excluidoEm = Instant.now();
        atualizadoEm = excluidoEm;
    }

    // ---- Decisões da coordenação (PLANO-DO-PROJETO.md §5.1). Cada uma devolve o registro para o histórico. ----

    /** Defere, opcionalmente corrigindo o item ou as horas. Corrigir exige justificativa, que o aluno vê. */
    Avaliacao deferir(UUID avaliador, Optional<Integer> novoItem, Optional<BigDecimal> novasHoras, Optional<String> justificativa) {
        exigirStatus(StatusCertificado.CONFIRMADO, "Só certificados em análise podem ser validados.");
        Integer itemAntes = itemNumero;
        BigDecimal horasAntes = horasCertificado;
        boolean mudouItem = novoItem.isPresent() && !novoItem.get().equals(itemNumero);
        boolean mudouHoras = novasHoras.isPresent() && (horasCertificado == null || novasHoras.get().compareTo(horasCertificado) != 0);
        if ((mudouItem || mudouHoras) && justificativa.isEmpty()) {
            throw ErroDeNegocio.invalido("justificativa-obrigatoria", "Explique ao aluno por que o item ou as horas mudaram.");
        }
        if (mudouItem) {
            itemNumero = novoItem.get();
        }
        if (mudouHoras) {
            horasCertificado = novasHoras.get();
        }
        Avaliacao avaliacao = decidir(avaliador, StatusCertificado.DEFERIDO, TipoAvaliacao.DEFERIDO, justificativa.orElse(null));
        return avaliacao.comMudancas(mudouItem ? itemAntes : null, mudouItem ? itemNumero : null,
                mudouHoras ? horasAntes : null, mudouHoras ? horasCertificado : null);
    }

    Avaliacao indeferir(UUID avaliador, Optional<String> justificativa) {
        exigirStatus(StatusCertificado.CONFIRMADO, "Só certificados em análise podem ser reprovados.");
        return decidir(avaliador, StatusCertificado.INDEFERIDO, TipoAvaliacao.INDEFERIDO,
                exigirJustificativa(justificativa, "Explique ao aluno por que o certificado foi reprovado e o que ficou faltando."));
    }

    /** Devolve ao aluno para corrigir; quando ele salvar, volta para a fila. */
    Avaliacao solicitarAjuste(UUID avaliador, Optional<String> justificativa) {
        exigirStatus(StatusCertificado.CONFIRMADO, "Só certificados em análise podem voltar para ajuste.");
        return decidir(avaliador, StatusCertificado.AJUSTE_SOLICITADO, TipoAvaliacao.AJUSTE_SOLICITADO,
                exigirJustificativa(justificativa, "Diga ao aluno o que precisa ser corrigido."));
    }

    /** Desfaz um deferimento ou indeferimento: o certificado volta para a fila. */
    Avaliacao reabrir(UUID avaliador, Optional<String> justificativa) {
        if (excluidoEm != null || (status != StatusCertificado.DEFERIDO && status != StatusCertificado.INDEFERIDO)) {
            throw ErroDeNegocio.conflito("status-invalido", "Só certificados validados ou reprovados podem ser reabertos.");
        }
        return decidir(avaliador, StatusCertificado.CONFIRMADO, TipoAvaliacao.REABERTO,
                exigirJustificativa(justificativa, "Registre por que o certificado foi reaberto."));
    }

    /**
     * A coordenação tira o certificado da conta do aluno, em qualquer situação (inclusive validado: as horas saem
     * do saldo). Exclusão lógica: o arquivo e o histórico ficam guardados.
     */
    Avaliacao excluirPelaCoordenacao(UUID avaliador, Optional<String> justificativa) {
        if (excluidoEm != null) {
            throw ErroDeNegocio.conflito("status-invalido", "Este certificado já foi excluído.");
        }
        String motivo = exigirJustificativa(justificativa, "Explique ao aluno por que o certificado foi excluído.");
        excluidoEm = Instant.now();
        avaliadoEm = excluidoEm;
        avaliadoPor = avaliador;
        observacaoCoordenacao = motivo;
        atualizadoEm = excluidoEm;
        return new Avaliacao(id, avaliador, TipoAvaliacao.EXCLUIDO, motivo, excluidoEm);
    }

    private Avaliacao decidir(UUID avaliador, StatusCertificado novo, TipoAvaliacao tipo, String justificativa) {
        status = novo;
        avaliadoEm = Instant.now();
        avaliadoPor = avaliador;
        observacaoCoordenacao = justificativa;
        atualizadoEm = avaliadoEm;
        return new Avaliacao(id, avaliador, tipo, justificativa, avaliadoEm);
    }

    private void exigirStatus(StatusCertificado esperado, String mensagem) {
        if (excluidoEm != null || status != esperado) {
            throw ErroDeNegocio.conflito("status-invalido", mensagem);
        }
    }

    private static String exigirJustificativa(Optional<String> justificativa, String mensagem) {
        return justificativa.orElseThrow(() -> ErroDeNegocio.invalido("justificativa-obrigatoria", mensagem));
    }

    private void aplicar(DadosCertificado dados) {
        titulo = dados.titulo().strip();
        instituicao = dados.instituicao() == null || dados.instituicao().isBlank() ? null : dados.instituicao().strip();
        dataInicio = dados.dataInicio();
        dataFim = dados.dataFim();
        horasCertificado = dados.horasCertificado();
        itemNumero = dados.itemNumero();
        observacaoAluno = dados.observacao() == null || dados.observacao().isBlank() ? null : dados.observacao().strip();
        atualizadoEm = Instant.now();
    }

    private static String limitar(String texto, int tamanho) {
        return texto.length() > tamanho ? texto.substring(0, tamanho) : texto;
    }

    Optional<Integer> getItemSugerido() {
        return Optional.ofNullable(itemSugerido);
    }

    Optional<Confianca> getConfiancaSugestao() {
        return Optional.ofNullable(confiancaSugestao);
    }

    java.util.List<Integer> getAlternativasSugeridas() {
        return java.util.List.of(alternativasSugeridas);
    }

    Optional<String> getMotivoSugestao() {
        return Optional.ofNullable(motivoSugestao);
    }

    boolean pertenceA(UUID aluno) {
        return alunoId.equals(aluno) && excluidoEm == null;
    }

    UUID getId() {
        return id;
    }

    Arquivo getArquivo() {
        return arquivo;
    }

    UUID getRegulamentoId() {
        return regulamentoId;
    }

    Optional<Integer> getItemNumero() {
        return Optional.ofNullable(itemNumero);
    }

    StatusCertificado getStatus() {
        return status;
    }

    Optional<String> getTitulo() {
        return Optional.ofNullable(titulo);
    }

    Optional<String> getInstituicao() {
        return Optional.ofNullable(instituicao);
    }

    Optional<LocalDate> getDataInicio() {
        return Optional.ofNullable(dataInicio);
    }

    Optional<LocalDate> getDataFim() {
        return Optional.ofNullable(dataFim);
    }

    Optional<BigDecimal> getHorasCertificado() {
        return Optional.ofNullable(horasCertificado);
    }

    OrigemClassificacao getOrigemClassificacao() {
        return origemClassificacao;
    }

    long getVersao() {
        return versao;
    }

    Instant getCriadoEm() {
        return criadoEm;
    }

    UUID getAlunoId() {
        return alunoId;
    }

    Optional<Instant> getConfirmadoEm() {
        return Optional.ofNullable(confirmadoEm);
    }

    Optional<Instant> getAvaliadoEm() {
        return Optional.ofNullable(avaliadoEm);
    }

    Optional<String> getObservacaoAluno() {
        return Optional.ofNullable(observacaoAluno);
    }

    Optional<String> getObservacaoCoordenacao() {
        return Optional.ofNullable(observacaoCoordenacao);
    }
}
