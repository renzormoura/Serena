package br.edu.univassouras.sereno.certificados;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O que o aluno informa (ou confirma) sobre um certificado.
 *
 * @param observacao recado opcional para quem analisa (até 250 caracteres)
 */
public record DadosCertificado(
        @NotBlank(message = "Informe o nome da atividade.") @Size(max = 300, message = "Nome muito longo.") String titulo,
        @Size(max = 200, message = "Nome muito longo.") String instituicao,
        LocalDate dataInicio,
        LocalDate dataFim,
        @NotNull(message = "Informe a carga horária do certificado.")
        @DecimalMin(value = "0", message = "A carga horária não pode ser negativa.")
        @DecimalMax(value = "2000", message = "Carga horária acima do esperado.")
        @Digits(integer = 4, fraction = 2, message = "Use no máximo duas casas decimais.")
        BigDecimal horasCertificado,
        @NotNull(message = "Escolha o item do regulamento.") Integer itemNumero,
        @Size(max = 250, message = "Use no máximo 250 caracteres.") String observacao) {
}
