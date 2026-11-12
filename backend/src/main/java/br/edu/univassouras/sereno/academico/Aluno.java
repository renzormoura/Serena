package br.edu.univassouras.sereno.academico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "aluno")
class Aluno {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID usuarioId;

    @Column(nullable = false)
    private String matricula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(nullable = false)
    private UUID regulamentoId;

    /** Opcional: o cadastro não pede; a coordenação pode informar depois. */
    private Integer anoIngresso;

    private Integer semestreIngresso;

    @Column(nullable = false)
    private Instant criadoEm;

    protected Aluno() {
    }

    Aluno(UUID usuarioId, String matricula, Curso curso, UUID regulamentoId) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.matricula = matricula.strip();
        this.curso = curso;
        this.regulamentoId = regulamentoId;
        this.criadoEm = Instant.now();
    }

    AlunoResumo resumo() {
        return new AlunoResumo(id, usuarioId, matricula, curso.getId(), curso.getNome(), regulamentoId, anoIngresso, semestreIngresso);
    }

    void definirIngresso(Integer ano, Integer semestre) {
        this.anoIngresso = ano;
        this.semestreIngresso = semestre;
    }

    UUID getId() {
        return id;
    }

    UUID getUsuarioId() {
        return usuarioId;
    }

    Curso getCurso() {
        return curso;
    }
}
