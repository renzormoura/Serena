package br.edu.univassouras.sereno.identidade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Só no perfil {@code dev}: cria as contas locais de coordenação e admin. A do aluno é criada pelo módulo acadêmico. */
@Component
@Profile("dev")
@Order(1)
class ContasDeDesenvolvimento implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContasDeDesenvolvimento.class);

    private final ContasService contas;
    private final String senha;

    ContasDeDesenvolvimento(ContasService contas, @Value("${sereno.dev.senha}") String senha) {
        this.contas = contas;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        criarSeFaltar("coordenacao@dev.local", "Coordenação (dev)", Papel.COORDENADOR);
        criarSeFaltar("admin@dev.local", "Administração (dev)", Papel.ADMIN);
    }

    private void criarSeFaltar(String email, String nome, Papel papel) {
        if (!contas.existe(email)) {
            contas.criarConta(new ContasService.NovaConta(email, nome, senha, papel));
            log.info("Conta de desenvolvimento criada: {}", email);
        }
    }
}
