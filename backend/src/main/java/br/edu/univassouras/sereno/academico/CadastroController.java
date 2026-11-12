package br.edu.univassouras.sereno.academico;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CadastroController {

    private final AlunoService alunos;

    CadastroController(AlunoService alunos) {
        this.alunos = alunos;
    }

    record CadastroForm(
            @NotBlank(message = "Informe seu nome.") @Size(max = 200) String nome,
            @NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.") @Size(max = 254) String email,
            @NotBlank(message = "Crie uma senha.") @Size(max = 200, message = "Senha longa demais.") String senha,
            @NotBlank(message = "Informe sua matrícula.")
            @Pattern(regexp = "[0-9A-Za-z.-]{3,30}", message = "Matrícula inválida.")
            String matricula,
            @NotNull(message = "Escolha o curso.") UUID cursoId) {
    }

    record CadastroCriado(UUID alunoId, String matricula) {
    }

    @PostMapping("/api/v1/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    CadastroCriado cadastrar(@Valid @RequestBody CadastroForm form) {
        AlunoResumo aluno = alunos.cadastrar(new AlunoService.NovoAluno(form.nome(), form.email(), form.senha(),
                form.matricula(), form.cursoId()));
        return new CadastroCriado(aluno.id(), aluno.matricula());
    }

    @GetMapping("/api/v1/cursos")
    List<AlunoService.CursoResumo> cursos() {
        return alunos.cursosAtivos();
    }
}
