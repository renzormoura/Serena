package br.edu.univassouras.sereno.regulamento;

import br.edu.univassouras.sereno.calculo.EstrategiaCalculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "item_atividade")
class ItemAtividade {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "macroatividade_id")
    private Macroatividade macroatividade;

    @Column(name = "regulamento_id", nullable = false)
    private UUID regulamentoId;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(nullable = false)
    private BigDecimal horasPorOcorrencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstrategiaCalculo estrategia;

    private BigDecimal tetoPorOcorrencia;

    private Integer limiteOcorrencias;

    private BigDecimal limiteHoras;

    @Column(nullable = false)
    private String[] palavrasChave = new String[0];

    @Column(nullable = false)
    private boolean ativo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemEsperada origemEsperada;

    protected ItemAtividade() {
    }

    int getNumero() {
        return numero;
    }

    String getNome() {
        return nome;
    }

    Optional<String> getDescricao() {
        return Optional.ofNullable(descricao);
    }

    BigDecimal getHorasPorOcorrencia() {
        return horasPorOcorrencia;
    }

    EstrategiaCalculo getEstrategia() {
        return estrategia;
    }

    Optional<BigDecimal> getTetoPorOcorrencia() {
        return Optional.ofNullable(tetoPorOcorrencia);
    }

    Optional<Integer> getLimiteOcorrencias() {
        return Optional.ofNullable(limiteOcorrencias);
    }

    Optional<BigDecimal> getLimiteHoras() {
        return Optional.ofNullable(limiteHoras);
    }

    List<String> getPalavrasChave() {
        return List.of(palavrasChave);
    }

    boolean isAtivo() {
        return ativo;
    }

    OrigemEsperada getOrigemEsperada() {
        return origemEsperada;
    }

    UUID getRegulamentoId() {
        return regulamentoId;
    }

    /** Ajuste das regras de sugestão automática, feito pela administração. */
    void ajustarClassificacao(java.util.List<String> palavras, OrigemEsperada origem) {
        this.palavrasChave = palavras.stream().map(String::strip).filter(p -> !p.isEmpty()).distinct().toArray(String[]::new);
        this.origemEsperada = origem;
    }
}
