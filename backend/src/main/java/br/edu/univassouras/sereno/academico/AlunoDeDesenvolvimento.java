package br.edu.univassouras.sereno.academico;

import br.edu.univassouras.sereno.identidade.ContasService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Só no perfil {@code dev}: cria a conta local aluno@dev.local e vincula a coordenação local ao curso. */
@Component
@Profile("dev")
@Order(2)
class AlunoDeDesenvolvimento implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AlunoDeDesenvolvimento.class);
    private static final UUID ENGENHARIA_CIVIL = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

    private final AlunoService alunos;
    private final ContasService contas;
    private final String senha;

    AlunoDeDesenvolvimento(AlunoService alunos, ContasService contas, @Value("${sereno.dev.senha}") String senha) {
        this.alunos = alunos;
        this.contas = contas;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!contas.existe("aluno@dev.local")) {
            alunos.cadastrar(new AlunoService.NovoAluno("Aluno de Teste", "aluno@dev.local", senha, "DEV0001",
                    ENGENHARIA_CIVIL));
            log.info("Conta de desenvolvimento criada: aluno@dev.local");
        }
        // A conta coordenacao@dev.local (criada pelo módulo de identidade) coordena todas as engenharias.
        contas.idPorEmail("coordenacao@dev.local")
                .ifPresent(id -> alunos.idsDosCursosAtivos().forEach(curso -> alunos.vincularCoordenador(id, curso)));
    }
}
