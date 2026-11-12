package br.edu.univassouras.sereno.academico;

import br.edu.univassouras.sereno.identidade.IdentificadorDeLogin;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Permite entrar com a matrícula no lugar do e-mail. Sem diferenciar maiúsculas de minúsculas. */
@Component
class MatriculaComoLogin implements IdentificadorDeLogin {

    private final AlunoRepository alunos;

    MatriculaComoLogin(AlunoRepository alunos) {
        this.alunos = alunos;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> usuarioDe(String identificador) {
        return alunos.findByMatriculaIgnoreCase(identificador.strip()).map(Aluno::getUsuarioId);
    }
}
