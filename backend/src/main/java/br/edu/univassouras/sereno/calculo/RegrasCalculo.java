package br.edu.univassouras.sereno.calculo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Regras de um regulamento, já no formato que o motor entende.
 *
 * @param horasExigidas total exigido; vazio enquanto o regulamento não o define
 * @param itens         regras por número de item (1..52 no regulamento atual)
 * @param macros        regras por código de macroatividade
 */
public record RegrasCalculo(Optional<BigDecimal> horasExigidas, Map<Integer, Item> itens, Map<String, Macro> macros) {

    public RegrasCalculo {
        Objects.requireNonNull(horasExigidas, "horasExigidas");
        itens = Map.copyOf(itens);
        macros = Map.copyOf(macros);
        for (Item item : itens.values()) {
            if (!macros.containsKey(item.macro())) {
                throw new IllegalArgumentException("Item " + item.numero() + " aponta para macroatividade inexistente: " + item.macro());
            }
        }
    }

    public Item item(int numero) {
        Item item = itens.get(numero);
        if (item == null) {
            throw new IllegalArgumentException("Item " + numero + " não existe neste regulamento");
        }
        return item;
    }

    /**
     * @param horasPorOcorrencia valor fixo (usado por {@link EstrategiaCalculo#FIXO_POR_OCORRENCIA})
     * @param tetoPorOcorrencia  obrigatório para {@link EstrategiaCalculo#HORAS_CERTIFICADO_COM_TETO}
     * @param limiteOcorrencias  máximo de certificados que contam no item (vazio = sem limite)
     * @param limiteHoras        máximo de horas no item (vazio = sem limite)
     */
    public record Item(
            int numero,
            String macro,
            BigDecimal horasPorOcorrencia,
            EstrategiaCalculo estrategia,
            Optional<BigDecimal> tetoPorOcorrencia,
            Optional<Integer> limiteOcorrencias,
            Optional<BigDecimal> limiteHoras) {

        public Item {
            Objects.requireNonNull(macro, "macro");
            Objects.requireNonNull(horasPorOcorrencia, "horasPorOcorrencia");
            Objects.requireNonNull(estrategia, "estrategia");
            if (estrategia == EstrategiaCalculo.HORAS_CERTIFICADO_COM_TETO && tetoPorOcorrencia.isEmpty()) {
                throw new IllegalArgumentException("Item " + numero + " usa teto mas não define teto_por_ocorrencia");
            }
        }

        /** Item simples, com valor fixo por certificado e sem limites. */
        public static Item fixo(int numero, String macro, int horas) {
            return new Item(numero, macro, BigDecimal.valueOf(horas), EstrategiaCalculo.FIXO_POR_OCORRENCIA,
                    Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    /** @param limiteHoras máximo de horas na macroatividade (vazio = sem limite) */
    public record Macro(String codigo, Optional<BigDecimal> limiteHoras) {

        public static Macro semLimite(String codigo) {
            return new Macro(codigo, Optional.empty());
        }
    }
}
