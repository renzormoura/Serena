package br.edu.univassouras.sereno.identidade;

import java.util.Optional;
import java.util.UUID;

/**
 * Ponto de extensão do login: outros módulos dizem a quem pertence um identificador que não é e-mail (ex.: a
 * matrícula, no módulo acadêmico). Assim a identidade não depende de quem guarda esses dados.
 */
public interface IdentificadorDeLogin {

    /** O usuário dono do identificador, se este módulo o reconhece. */
    Optional<UUID> usuarioDe(String identificador);
}
