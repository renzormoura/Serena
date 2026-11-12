package br.edu.univassouras.sereno.certificados;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "arquivo")
class Arquivo {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String chave;

    @Column(nullable = false)
    private String nomeOriginal;

    @Column(nullable = false)
    private String tipoMime;

    @Column(nullable = false)
    private long tamanhoBytes;

    @Column(nullable = false)
    private String sha256;

    @Column(nullable = false)
    private UUID enviadoPor;

    @Column(nullable = false)
    private Instant criadoEm;

    protected Arquivo() {
    }

    Arquivo(String chave, String nomeOriginal, String tipoMime, long tamanhoBytes, String sha256, UUID enviadoPor) {
        this.id = UUID.randomUUID();
        this.chave = chave;
        this.nomeOriginal = nomeOriginal;
        this.tipoMime = tipoMime;
        this.tamanhoBytes = tamanhoBytes;
        this.sha256 = sha256;
        this.enviadoPor = enviadoPor;
        this.criadoEm = Instant.now();
    }

    String getSha256() {
        return sha256;
    }

    String getChave() {
        return chave;
    }

    String getNomeOriginal() {
        return nomeOriginal;
    }

    String getTipoMime() {
        return tipoMime;
    }

    long getTamanhoBytes() {
        return tamanhoBytes;
    }
}
