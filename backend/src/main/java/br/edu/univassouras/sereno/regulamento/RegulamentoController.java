package br.edu.univassouras.sereno.regulamento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/regulamentos")
class RegulamentoController {

    private final CatalogoService catalogo;

    RegulamentoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/vigente")
    Catalogo vigente() {
        return catalogo.vigente()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum regulamento publicado"));
    }
}
