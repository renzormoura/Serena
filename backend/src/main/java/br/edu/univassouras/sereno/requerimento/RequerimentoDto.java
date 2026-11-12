package br.edu.univassouras.sereno.requerimento;

import br.edu.univassouras.sereno.comum.Anulavel;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um requerimento já gerado, para listar e baixar.
 *
 * @param dataDeferimento nulo quando o aluno gerou (a coordenação preenche ao deferir)
 * @param coordenador     nulo quando o aluno gerou
 */
record RequerimentoDto(UUID id, int numero, Instant geradoEm, @Anulavel LocalDate dataDeferimento, @Anulavel String coordenador,
        BigDecimal totalHoras, boolean geradoPeloAluno) {

    static RequerimentoDto de(Requerimento r) {
        return new RequerimentoDto(r.getId(), r.getNumero(), r.getGeradoEm(), r.getDataDeferimento().orElse(null),
                r.getCoordenadorNome().orElse(null), r.getTotalHoras(), r.isGeradoPeloAluno());
    }
}
