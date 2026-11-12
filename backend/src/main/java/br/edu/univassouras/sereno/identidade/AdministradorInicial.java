package br.edu.univassouras.sereno.identidade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Em produção não existem as contas de teste: a primeira conta de administração vem de {@code SERENO_ADMIN_EMAIL} e
 * {@code SERENO_ADMIN_SENHA}. Só cria se o e-mail ainda não existir; depois disso as variáveis podem ser apagadas.
 * Com a conta criada, a administração cadastra a coordenação pela tela "Equipe".
 */
@Component
class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final ContasService contas;
    private final String email;
    private final String senha;
    private final String nome;

    AdministradorInicial(ContasService contas, @Value("${sereno.admin-inicial.email:}") String email,
            @Value("${sereno.admin-inicial.senha:}") String senha, @Value("${sereno.admin-inicial.nome:Administração}") String nome) {
        this.contas = contas;
        this.email = email.strip();
        this.senha = senha;
        this.nome = nome;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isEmpty() || senha.isEmpty() || contas.existe(email)) {
            return;
        }
        contas.criarConta(new ContasService.NovaConta(email, nome, senha, Papel.ADMIN));
        log.info("Conta de administração inicial criada: {}", email);
    }
}
