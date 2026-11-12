package br.edu.univassouras.sereno.academico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "curso")
class Curso {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    private String sigla;

    @Column(nullable = false)
    private boolean ativo;

    protected Curso() {
    }

    UUID getId() {
        return id;
    }

    String getNome() {
        return nome;
    }

    String getSigla() {
        return sigla;
    }

    boolean isAtivo() {
        return ativo;
    }
}
