package br.edu.univassouras.sereno.requerimento;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Um requerimento gerado. Imutável: gerar de novo cria outro, com o número seguinte. */
@Entity
@Table(name = "requerimento")
class Requerimento {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID alunoId;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private String nomeAluno;

    @Column(nullable = false)
    private String matricula;

    @Column(nullable = false)
    private String curso;

    /** Nulo quando foi o aluno que gerou (ainda sem deferimento). */
    private UUID coordenadorId;

    private String coordenadorNome;

    @Column(nullable = false)
    private BigDecimal totalHoras;

    @Column(nullable = false)
    private Instant geradoEm;

    private LocalDate dataDeferimento;

    @Column(nullable = false)
    private boolean geradoPeloAluno;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "requerimento_linha", joinColumns = @JoinColumn(name = "requerimento_id"))
    @OrderColumn(name = "ordem")
    private List<Linha> linhas = new ArrayList<>();

    protected Requerimento() {
    }

    Requerimento(UUID alunoId, int numero, String nomeAluno, String matricula, String curso, UUID coordenadorId,
            String coordenadorNome, Instant geradoEm, LocalDate dataDeferimento, List<Linha> linhas, boolean geradoPeloAluno) {
        this.geradoPeloAluno = geradoPeloAluno;
        this.id = UUID.randomUUID();
        this.alunoId = alunoId;
        this.numero = numero;
        this.nomeAluno = nomeAluno;
        this.matricula = matricula;
        this.curso = curso;
        this.coordenadorId = coordenadorId;
        this.coordenadorNome = coordenadorNome;
        this.geradoEm = geradoEm;
        this.dataDeferimento = dataDeferimento;
        this.linhas = new ArrayList<>(linhas);
        this.totalHoras = linhas.stream().map(Linha::getHorasIntegralizadas).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Uma linha da tabela: um item do regulamento. */
    @Embeddable
    static class Linha {

        @Column(nullable = false)
        private int item;

        @Column(nullable = false)
        private String macroatividade;

        @Column(nullable = false)
        private String nome;

        private String descricao;

        @Column(nullable = false)
        private BigDecimal horasPorOcorrencia;

        @Column(nullable = false)
        private int quantidade;

        @Column(nullable = false)
        private BigDecimal horasIntegralizadas;

        protected Linha() {
        }

        Linha(int item, String macroatividade, String nome, String descricao, BigDecimal horasPorOcorrencia, int quantidade,
                BigDecimal horasIntegralizadas) {
            this.item = item;
            this.macroatividade = macroatividade;
            this.nome = nome;
            this.descricao = descricao;
            this.horasPorOcorrencia = horasPorOcorrencia;
            this.quantidade = quantidade;
            this.horasIntegralizadas = horasIntegralizadas;
        }

        int getItem() {
            return item;
        }

        String getMacroatividade() {
            return macroatividade;
        }

        String getNome() {
            return nome;
        }

        String getDescricao() {
            return descricao;
        }

        BigDecimal getHorasPorOcorrencia() {
            return horasPorOcorrencia;
        }

        int getQuantidade() {
            return quantidade;
        }

        BigDecimal getHorasIntegralizadas() {
            return horasIntegralizadas;
        }
    }

    UUID getId() {
        return id;
    }

    UUID getAlunoId() {
        return alunoId;
    }

    int getNumero() {
        return numero;
    }

    String getNomeAluno() {
        return nomeAluno;
    }

    String getMatricula() {
        return matricula;
    }

    String getCurso() {
        return curso;
    }

    java.util.Optional<String> getCoordenadorNome() {
        return java.util.Optional.ofNullable(coordenadorNome);
    }

    BigDecimal getTotalHoras() {
        return totalHoras;
    }

    Instant getGeradoEm() {
        return geradoEm;
    }

    boolean isGeradoPeloAluno() {
        return geradoPeloAluno;
    }

    java.util.Optional<LocalDate> getDataDeferimento() {
        return java.util.Optional.ofNullable(dataDeferimento);
    }

    List<Linha> getLinhas() {
        return List.copyOf(linhas);
    }
}
