package br.edu.univassouras.sereno.academico;

import br.edu.univassouras.sereno.identidade.ContasService;
import br.edu.univassouras.sereno.identidade.Papel;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Contas fixas, pedidas pelo responsável pelo projeto em 11/11/2026: existem em qualquer ambiente e são recriadas
 * ao subir se o banco for perdido (ex.: banco gratuito do Render). Não seguem a regra de senha do cadastro. Se a
 * conta já existe, nada é alterado.
 */
@Component
@Order(0)
class ContasFixas implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContasFixas.class);
    static final UUID ENGENHARIA_CIVIL = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

    private final ContasService contas;
    private final AlunoService alunos;

    ContasFixas(ContasService contas, AlunoService alunos) {
        this.contas = contas;
        this.alunos = alunos;
    }

    @Override
    public void run(ApplicationArguments args) {
        contas.garantirContaFixa(new ContasService.NovaConta("adm@gmail.com", "adm", "12345678", Papel.ADMIN));

        UUID coordenacao = contas.garantirContaFixa(
                new ContasService.NovaConta("coordenacao@dev.local", "Coordenação", "sereno-dev", Papel.COORDENADOR));
        alunos.idsDosCursosAtivos().forEach(curso -> alunos.vincularCoordenador(coordenacao, curso));

        alunos.garantirAlunoFixo("Renzo Moura Rosa", "renzormoura@gmail.com", "24652536", "202410568", ENGENHARIA_CIVIL);
        alunos.garantirAlunoFixo("Pedro Henrique do Nascimento Gama", "pedrohngama@gmail.com", "12345678", "202412748",
                ENGENHARIA_CIVIL);
        log.info("Contas fixas conferidas (adm, coordenação, Renzo e Pedro).");
    }
}
