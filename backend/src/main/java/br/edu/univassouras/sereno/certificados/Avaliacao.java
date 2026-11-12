package br.edu.univassouras.sereno.certificados;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Uma decisão da coordenação, com o que mudou. Só inserção: o histórico nunca é alterado. */
@Entity
@Table(name = "avaliacao")
class Avaliacao {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID certificadoId;

    @Column(nullable = false)
    private UUID avaliadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAvaliacao decisao;

    private String justificativa;

    private Integer itemAnterior;

    private Integer itemNovo;

    private BigDecimal horasAnteriores;

    private BigDecimal horasNovas;

    @Column(nullable = false)
    private Instant criadoEm;

    protected Avaliacao() {
    }

    Avaliacao(UUID certificadoId, UUID avaliadorId, TipoAvaliacao decisao, String justificativa, Instant quando) {
        this.id = UUID.randomUUID();
        this.certificadoId = certificadoId;
        this.avaliadorId = avaliadorId;
        this.decisao = decisao;
        this.justificativa = justificativa;
        this.criadoEm = quando;
    }

    Avaliacao comMudancas(Integer itemAnterior, Integer itemNovo, BigDecimal horasAnteriores, BigDecimal horasNovas) {
        this.itemAnterior = itemAnterior;
        this.itemNovo = itemNovo;
        this.horasAnteriores = horasAnteriores;
        this.horasNovas = horasNovas;
        return this;
    }

    UUID getCertificadoId() {
        return certificadoId;
    }

    UUID getAvaliadorId() {
        return avaliadorId;
    }

    TipoAvaliacao getDecisao() {
        return decisao;
    }

    Optional<String> getJustificativa() {
        return Optional.ofNullable(justificativa);
    }

    Optional<Integer> getItemAnterior() {
        return Optional.ofNullable(itemAnterior);
    }

    Optional<Integer> getItemNovo() {
        return Optional.ofNullable(itemNovo);
    }

    Optional<BigDecimal> getHorasAnteriores() {
        return Optional.ofNullable(horasAnteriores);
    }

    Optional<BigDecimal> getHorasNovas() {
        return Optional.ofNullable(horasNovas);
    }

    Instant getCriadoEm() {
        return criadoEm;
    }
}
