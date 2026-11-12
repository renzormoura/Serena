package br.edu.univassouras.sereno.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Um aviso no site para o usuário (ex.: "Certificado validado"). */
@Entity
@Table(name = "aviso")
class Aviso {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID usuarioId;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String mensagem;

    private String link;

    private Instant lidoEm;

    @Column(nullable = false)
    private Instant criadoEm;

    protected Aviso() {
    }

    Aviso(UUID usuarioId, String titulo, String mensagem, String link) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.titulo = titulo;
        this.mensagem = mensagem.length() > 1500 ? mensagem.substring(0, 1500) : mensagem;
        this.link = link;
        this.criadoEm = Instant.now();
    }

    void marcarLido(Instant quando) {
        if (lidoEm == null) {
            lidoEm = quando;
        }
    }

    UUID getId() {
        return id;
    }

    String getTitulo() {
        return titulo;
    }

    String getMensagem() {
        return mensagem;
    }

    Optional<String> getLink() {
        return Optional.ofNullable(link);
    }

    Optional<Instant> getLidoEm() {
        return Optional.ofNullable(lidoEm);
    }

    Instant getCriadoEm() {
        return criadoEm;
    }
}
