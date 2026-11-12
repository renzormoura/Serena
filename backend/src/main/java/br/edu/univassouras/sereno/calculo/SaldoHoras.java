package br.edu.univassouras.sereno.calculo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Resultado do cálculo para um aluno.
 *
 * @param integralizadas horas deferidas que contam, já com limites aplicados
 * @param pendentes      quanto a projeção soma além das integralizadas
 * @param projecao       integralizadas + pendentes, se tudo for deferido
 * @param faltantes      exigidas − integralizadas (nunca negativo); vazio sem total exigido
 * @param percentual     integralizadas / exigidas, 0–100; vazio sem total exigido
 * @param porItem        horas integralizadas por item
 * @param porMacro       horas integralizadas por macroatividade
 * @param porMacroProjecao horas por macroatividade se tudo for deferido
 * @param detalhes       por certificado, na projeção: quanto contou e por que foi cortado
 */
public record SaldoHoras(
        Optional<BigDecimal> exigidas,
        BigDecimal integralizadas,
        BigDecimal pendentes,
        BigDecimal projecao,
        Optional<BigDecimal> faltantes,
        Optional<Integer> percentual,
        Map<Integer, BigDecimal> porItem,
        Map<String, BigDecimal> porMacro,
        Map<String, BigDecimal> porMacroProjecao,
        List<Detalhe> detalhes) {

    public record Detalhe(UUID certificadoId, BigDecimal horasBrutas, BigDecimal horasContabilizadas, List<String> cortes) {
    }
}
