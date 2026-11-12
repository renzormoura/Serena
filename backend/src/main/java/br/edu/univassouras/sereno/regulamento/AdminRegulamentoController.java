package br.edu.univassouras.sereno.regulamento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/regulamentos/vigente")
@PreAuthorize("hasRole('ADMIN')")
class AdminRegulamentoController {

    private final CatalogoService catalogo;

    AdminRegulamentoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    record ClassificacaoForm(
            @NotNull(message = "Informe as palavras-chave.")
            @Size(max = 40, message = "No máximo 40 palavras-chave.")
            List<@NotBlank @Size(max = 80) String> palavrasChave,
            @NotNull(message = "Informe a origem esperada.") OrigemEsperada origemEsperada) {
    }

    /** @param horasExigidas nulo apaga o total (volta a "a definir") */
    record HorasExigidasForm(
            @jakarta.validation.constraints.DecimalMin(value = "1", message = "Informe um total positivo.")
            @jakarta.validation.constraints.DecimalMax(value = "2000", message = "Total alto demais.")
            java.math.BigDecimal horasExigidas) {
    }

    /**
     * Total de horas complementares exigido (decisão D1). Liga os contadores "faltam", o percentual e a lista de
     * alunos em atenção do painel.
     */
    @PutMapping("/horas-exigidas")
    Catalogo definirHorasExigidas(@Valid @RequestBody HorasExigidasForm form) {
        return catalogo.definirHorasExigidas(form.horasExigidas());
    }

    /** Ajusta como o sistema reconhece um item ao sugerir a classificação de certificados novos. */
    @PutMapping("/itens/{numero}/classificacao")
    Catalogo.ItemDto ajustar(@PathVariable int numero, @Valid @RequestBody ClassificacaoForm form) {
        return catalogo.ajustarClassificacao(numero, form.palavrasChave(), form.origemEsperada());
    }
}
