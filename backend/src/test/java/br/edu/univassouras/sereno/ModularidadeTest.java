package br.edu.univassouras.sereno;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Garante as fronteiras entre módulos (sem ciclos, sem acesso a tipos internos de outro módulo). */
class ModularidadeTest {

    private final ApplicationModules modulos = ApplicationModules.of(SerenoApplication.class);

    @Test
    void fronteirasDosModulosSaoRespeitadas() {
        modulos.verify();
    }

    @Test
    void geraDocumentacaoDosModulos() {
        new Documenter(modulos).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
