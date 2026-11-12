package br.edu.univassouras.sereno.regulamento;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "regulamento")
class Regulamento {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private int versao;

    private LocalDate vigenteDesde;

    private LocalDate vigenteAte;

    private BigDecimal horasExigidas;

    @Column(nullable = false)
    private boolean publicado;

    @OneToMany(mappedBy = "regulamento", cascade = CascadeType.ALL)
    @OrderBy("ordem")
    private List<Macroatividade> macroatividades = new ArrayList<>();

    protected Regulamento() {
    }

    UUID getId() {
        return id;
    }

    String getNome() {
        return nome;
    }

    int getVersao() {
        return versao;
    }

    Optional<LocalDate> getVigenteDesde() {
        return Optional.ofNullable(vigenteDesde);
    }

    Optional<BigDecimal> getHorasExigidas() {
        return Optional.ofNullable(horasExigidas);
    }

    /** Total de horas complementares exigido (decisão D1). Nulo enquanto não definido. */
    void definirHorasExigidas(BigDecimal horas) {
        this.horasExigidas = horas;
    }

    boolean isPublicado() {
        return publicado;
    }

    List<Macroatividade> getMacroatividades() {
        return List.copyOf(macroatividades);
    }
}
