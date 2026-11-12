package br.edu.univassouras.sereno.calculo;

import br.edu.univassouras.sereno.calculo.CertificadoParaCalculo.Situacao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Calcula o saldo de horas complementares. Função pura: sem banco, sem Spring, determinística.
 *
 * <p>Passos por certificado (PLANO-DO-PROJETO.md §9): horas brutas pela estratégia do item → limite de
 * ocorrências do item → limite de horas do item → limite de horas da macroatividade.
 *
 * <p>Os limites são consumidos primeiro pelos deferidos e, dentro de cada grupo, pelos mais antigos. Por isso
 * o cálculo roda duas vezes: só com deferidos (integralizadas) e com todos (projeção). Assim um pendente
 * nunca "rouba" espaço de um certificado já deferido.
 */
public final class MotorCalculo {

    private static final Comparator<CertificadoParaCalculo> ORDEM_DE_CONSUMO = Comparator
            .comparing((CertificadoParaCalculo c) -> c.situacao() == Situacao.DEFERIDO ? 0 : 1)
            .thenComparing(c -> c.data().orElse(LocalDate.MAX))
            .thenComparing(CertificadoParaCalculo::id);

    public SaldoHoras calcular(RegrasCalculo regras, Collection<CertificadoParaCalculo> certificados) {
        List<CertificadoParaCalculo> deferidos = certificados.stream()
                .filter(c -> c.situacao() == Situacao.DEFERIDO)
                .toList();

        Passada oficial = executar(regras, deferidos);
        Passada projecao = executar(regras, certificados);

        BigDecimal integralizadas = oficial.total();
        BigDecimal projetado = projecao.total();

        Optional<BigDecimal> faltantes = regras.horasExigidas()
                .map(exigidas -> exigidas.subtract(integralizadas).max(BigDecimal.ZERO));
        Optional<Integer> percentual = regras.horasExigidas()
                .map(exigidas -> integralizadas.min(exigidas)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(exigidas, 0, RoundingMode.DOWN)
                        .intValueExact());

        return new SaldoHoras(
                regras.horasExigidas(),
                integralizadas,
                projetado.subtract(integralizadas),
                projetado,
                faltantes,
                percentual,
                oficial.porItem(),
                oficial.porMacro(),
                projecao.porMacro(),
                projecao.detalhes());
    }

    private Passada executar(RegrasCalculo regras, Collection<CertificadoParaCalculo> certificados) {
        Map<Integer, Integer> ocorrenciasPorItem = new HashMap<>();
        Map<Integer, BigDecimal> horasPorItem = new TreeMap<>();
        Map<String, BigDecimal> horasPorMacro = new TreeMap<>();
        regras.macros().keySet().forEach(codigo -> horasPorMacro.put(codigo, BigDecimal.ZERO));
        List<SaldoHoras.Detalhe> detalhes = new ArrayList<>();

        for (CertificadoParaCalculo certificado : certificados.stream().sorted(ORDEM_DE_CONSUMO).toList()) {
            RegrasCalculo.Item item = regras.item(certificado.item());
            RegrasCalculo.Macro macro = regras.macros().get(item.macro());
            List<String> cortes = new ArrayList<>();

            BigDecimal brutas = horasBrutas(item, certificado);
            BigDecimal contam = brutas;

            int ocorrencia = ocorrenciasPorItem.merge(item.numero(), 1, Integer::sum);
            if (item.limiteOcorrencias().isPresent() && ocorrencia > item.limiteOcorrencias().get()) {
                contam = BigDecimal.ZERO;
                cortes.add("Limite de " + item.limiteOcorrencias().get() + " certificado(s) no item " + item.numero() + " atingido");
            }

            BigDecimal jaNoItem = horasPorItem.getOrDefault(item.numero(), BigDecimal.ZERO);
            contam = aplicarLimite(contam, jaNoItem, item.limiteHoras(),
                    "Limite de %s h no item " + item.numero() + " atingido", cortes);

            BigDecimal jaNaMacro = horasPorMacro.get(macro.codigo());
            contam = aplicarLimite(contam, jaNaMacro, macro.limiteHoras(),
                    "Limite de %s h na macroatividade " + macro.codigo() + " atingido", cortes);

            horasPorItem.put(item.numero(), jaNoItem.add(contam));
            horasPorMacro.put(macro.codigo(), jaNaMacro.add(contam));
            detalhes.add(new SaldoHoras.Detalhe(certificado.id(), brutas, contam, List.copyOf(cortes)));
        }
        return new Passada(Map.copyOf(horasPorItem), Map.copyOf(horasPorMacro), List.copyOf(detalhes));
    }

    private static BigDecimal horasBrutas(RegrasCalculo.Item item, CertificadoParaCalculo certificado) {
        return switch (item.estrategia()) {
            case FIXO_POR_OCORRENCIA -> item.horasPorOcorrencia();
            case HORAS_CERTIFICADO -> certificado.horasCertificado();
            case HORAS_CERTIFICADO_COM_TETO -> certificado.horasCertificado().min(item.tetoPorOcorrencia().orElseThrow());
        };
    }

    private static BigDecimal aplicarLimite(
            BigDecimal horas, BigDecimal jaContabilizado, Optional<BigDecimal> limite, String motivo, List<String> cortes) {
        if (limite.isEmpty() || horas.signum() == 0) {
            return horas;
        }
        BigDecimal espaco = limite.get().subtract(jaContabilizado).max(BigDecimal.ZERO);
        if (horas.compareTo(espaco) > 0) {
            cortes.add(motivo.formatted(limite.get().stripTrailingZeros().toPlainString()));
            return espaco;
        }
        return horas;
    }

    private record Passada(Map<Integer, BigDecimal> porItem, Map<String, BigDecimal> porMacro, List<SaldoHoras.Detalhe> detalhes) {

        BigDecimal total() {
            return porMacro.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
