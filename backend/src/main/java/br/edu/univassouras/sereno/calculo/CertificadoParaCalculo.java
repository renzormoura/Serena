package br.edu.univassouras.sereno.calculo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * O mínimo de um certificado que o motor precisa saber.
 *
 * @param data usada para ordenar quem consome os limites primeiro; vazia vai para o fim
 */
public record CertificadoParaCalculo(
        UUID id, int item, BigDecimal horasCertificado, Optional<LocalDate> data, Situacao situacao) {

    public CertificadoParaCalculo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(horasCertificado, "horasCertificado");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(situacao, "situacao");
        if (horasCertificado.signum() < 0) {
            throw new IllegalArgumentException("Horas do certificado não podem ser negativas");
        }
    }

    public enum Situacao {
        /** Deferido pela coordenação: conta como integralizado. */
        DEFERIDO,
        /** Confirmado pelo aluno, aguardando análise: conta só na projeção. */
        PENDENTE
    }
}
