package br.edu.univassouras.sereno.regulamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "macroatividade")
class Macroatividade {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regulamento_id")
    private Regulamento regulamento;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false)
    private String nome;

    private BigDecimal limiteHoras;

    @OneToMany(mappedBy = "macroatividade")
    @OrderBy("numero")
    private List<ItemAtividade> itens = new ArrayList<>();

    protected Macroatividade() {
    }

    String getCodigo() {
        return codigo;
    }

    int getOrdem() {
        return ordem;
    }

    String getNome() {
        return nome;
    }

    Optional<BigDecimal> getLimiteHoras() {
        return Optional.ofNullable(limiteHoras);
    }

    List<ItemAtividade> getItens() {
        return List.copyOf(itens);
    }
}
