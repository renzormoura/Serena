package br.edu.univassouras.sereno.extracao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Registro do que foi lido. Imutável: cada nova leitura gera um registro novo. */
@Entity
@Table(name = "extracao")
class Extracao {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID certificadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoExtracao metodo;

    @Column(nullable = false)
    private String versaoExtrator;

    private String textoBruto;

    private String titulo;

    private String instituicao;

    private LocalDate dataInicio;

    private LocalDate dataFim;

    private BigDecimal horas;

    private Boolean nomeConfere;

    @Column(nullable = false)
    private String[] trechos;

    @Column(nullable = false)
    private String[] avisos;

    private String pdfProdutor;

    private String pdfCriador;

    private Instant pdfCriadoEm;

    private Instant pdfModificadoEm;

    @Column(nullable = false)
    private long duracaoMs;

    @Column(nullable = false)
    private Instant criadoEm;

    protected Extracao() {
    }

    Extracao(UUID certificadoId, String textoBruto, ResultadoExtracao r, long duracaoMs) {
        this.id = UUID.randomUUID();
        this.certificadoId = certificadoId;
        this.metodo = r.metodo();
        this.versaoExtrator = ExtratorDeCampos.VERSAO;
        this.textoBruto = textoBruto == null || textoBruto.isBlank() ? null : textoBruto;
        this.titulo = r.titulo().orElse(null);
        this.instituicao = r.instituicao().orElse(null);
        this.dataInicio = r.dataInicio().orElse(null);
        this.dataFim = r.dataFim().orElse(null);
        this.horas = r.horas().orElse(null);
        this.nomeConfere = r.nomeConfere().orElse(null);
        this.trechos = r.trechos().toArray(String[]::new);
        this.avisos = r.avisos().toArray(String[]::new);
        this.pdfProdutor = r.pdf().produtor().orElse(null);
        this.pdfCriador = r.pdf().criador().orElse(null);
        this.pdfCriadoEm = r.pdf().criadoEm().orElse(null);
        this.pdfModificadoEm = r.pdf().modificadoEm().orElse(null);
        this.duracaoMs = duracaoMs;
        this.criadoEm = Instant.now();
    }

    UUID getCertificadoId() {
        return certificadoId;
    }

    ResultadoExtracao resultado() {
        return new ResultadoExtracao(metodo, Optional.ofNullable(titulo), Optional.ofNullable(instituicao),
                Optional.ofNullable(dataInicio), Optional.ofNullable(dataFim), Optional.ofNullable(horas),
                Optional.ofNullable(nomeConfere), List.of(trechos), List.of(avisos), textoBruto,
                new MetadadosPdf(Optional.ofNullable(pdfProdutor), Optional.ofNullable(pdfCriador),
                        Optional.ofNullable(pdfCriadoEm), Optional.ofNullable(pdfModificadoEm)));
    }
}
