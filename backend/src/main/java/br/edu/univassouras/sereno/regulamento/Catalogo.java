package br.edu.univassouras.sereno.regulamento;

import br.edu.univassouras.sereno.calculo.EstrategiaCalculo;
import br.edu.univassouras.sereno.comum.Anulavel;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Visão somente leitura de um regulamento, exposta aos outros módulos e à API.
 * Campos sem valor definido vêm como {@code null} (contrato JSON); o domínio interno usa {@code Optional}.
 */
public record Catalogo(UUID id, String nome, int versao, @Anulavel BigDecimal horasExigidas, List<MacroDto> macroatividades) {

    /** Item pelo número, com a macroatividade a que pertence. */
    public java.util.Optional<ItemComMacro> item(int numero) {
        return macroatividades.stream()
                .flatMap(m -> m.itens().stream().filter(i -> i.numero() == numero).map(i -> new ItemComMacro(i, m)))
                .findFirst();
    }

    public record ItemComMacro(ItemDto item, MacroDto macro) {
    }

    public record MacroDto(String codigo, int ordem, String nome, @Anulavel BigDecimal limiteHoras, List<ItemDto> itens) {
    }

    public record ItemDto(
            int numero,
            String nome,
            @Anulavel String descricao,
            BigDecimal horasPorOcorrencia,
            EstrategiaCalculo estrategia,
            @Anulavel BigDecimal tetoPorOcorrencia,
            @Anulavel Integer limiteOcorrencias,
            @Anulavel BigDecimal limiteHoras,
            List<String> palavrasChave,
            OrigemEsperada origemEsperada) {
    }
}
