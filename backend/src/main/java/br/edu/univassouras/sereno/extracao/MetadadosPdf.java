package br.edu.univassouras.sereno.extracao;

import java.time.Instant;
import java.util.Optional;

/**
 * O que o próprio PDF declara sobre como foi gerado. Serve de pista na triagem (um certificado emitido por uma
 * plataforma raramente passa por um editor de PDF), nunca de prova.
 */
public record MetadadosPdf(Optional<String> produtor, Optional<String> criador, Optional<Instant> criadoEm,
        Optional<Instant> modificadoEm) {

    public static final MetadadosPdf VAZIO = new MetadadosPdf(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
}
