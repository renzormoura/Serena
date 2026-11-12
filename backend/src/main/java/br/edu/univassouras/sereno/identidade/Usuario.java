package br.edu.univassouras.sereno.identidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "usuario")
class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @Column(nullable = false)
    private boolean ativo = true;

    private Instant ultimoLoginEm;

    @Column(nullable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    protected Usuario() {
    }

    Usuario(String email, String nome, String senhaHash, Papel papel) {
        this.id = UUID.randomUUID();
        this.email = normalizarEmail(email);
        this.nome = nome.strip();
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    void registrarLogin(Instant quando) {
        this.ultimoLoginEm = quando;
    }

    UsuarioAutenticado paraAutenticado() {
        return new UsuarioAutenticado(id, email, nome, papel, senhaHash, ativo);
    }

    UUID getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    String getNome() {
        return nome;
    }

    Papel getPapel() {
        return papel;
    }

    Instant getCriadoEm() {
        return criadoEm;
    }
}
