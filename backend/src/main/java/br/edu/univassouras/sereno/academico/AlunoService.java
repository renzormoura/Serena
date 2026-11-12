package br.edu.univassouras.sereno.academico;

import br.edu.univassouras.sereno.comum.Anulavel;
import br.edu.univassouras.sereno.comum.ErroDeNegocio;
import br.edu.univassouras.sereno.identidade.ContasService;
import br.edu.univassouras.sereno.identidade.Papel;
import br.edu.univassouras.sereno.regulamento.CatalogoService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API pública do módulo acadêmico. */
@Service
public class AlunoService {

    private final AlunoRepository alunos;
    private final CursoRepository cursos;
    private final ContasService contas;
    private final CatalogoService catalogo;
    private final JdbcClient jdbc;

    AlunoService(AlunoRepository alunos, CursoRepository cursos, ContasService contas, CatalogoService catalogo,
            JdbcClient jdbc) {
        this.alunos = alunos;
        this.cursos = cursos;
        this.contas = contas;
        this.catalogo = catalogo;
        this.jdbc = jdbc;
    }

    public record NovoAluno(String nome, String email, String senha, String matricula, UUID cursoId) {
    }

    public record CursoResumo(UUID id, String nome, @Anulavel String sigla) {
    }

    /**
     * Cria a conta e o aluno na mesma transação. O aluno fica vinculado ao regulamento vigente no momento do
     * cadastro (PLANO-DO-PROJETO.md, RF05).
     */
    @Transactional
    public AlunoResumo cadastrar(NovoAluno novo) {
        if (alunos.existsByMatricula(novo.matricula().strip())) {
            throw ErroDeNegocio.conflito("matricula-em-uso", "Esta matrícula já está cadastrada.");
        }
        Curso curso = cursos.findById(novo.cursoId())
                .filter(Curso::isAtivo)
                .orElseThrow(() -> ErroDeNegocio.invalido("curso-invalido", "Curso não encontrado."));
        UUID regulamentoId = catalogo.idVigente()
                .orElseThrow(() -> ErroDeNegocio.conflito("sem-regulamento", "Nenhum regulamento publicado."));
        UUID usuarioId = contas.criarConta(new ContasService.NovaConta(novo.email(), novo.nome(), novo.senha(), Papel.ALUNO));
        return alunos.save(new Aluno(usuarioId, novo.matricula(), curso, regulamentoId))
                .resumo();
    }

    @Transactional(readOnly = true)
    public Optional<AlunoResumo> doUsuario(UUID usuarioId) {
        return alunos.findByUsuarioId(usuarioId).map(Aluno::resumo);
    }

    /** O aluno da sessão; 403 se a conta não for de aluno. */
    @Transactional(readOnly = true)
    public AlunoResumo exigirAluno(UUID usuarioId) {
        return doUsuario(usuarioId).orElseThrow(() -> ErroDeNegocio.proibido("Esta conta não é de aluno."));
    }

    @Transactional(readOnly = true)
    public List<CursoResumo> cursosAtivos() {
        return cursos.ativos().stream().map(c -> new CursoResumo(c.getId(), c.getNome(), c.getSigla())).toList();
    }

    /** Dá ao coordenador acesso aos alunos do curso. Repetir não tem efeito. */
    @Transactional
    public void vincularCoordenador(UUID usuarioId, UUID cursoId) {
        jdbc.sql("INSERT INTO coordenacao (usuario_id, curso_id) VALUES (?, ?) ON CONFLICT DO NOTHING")
                .params(usuarioId, cursoId)
                .update();
    }

    /**
     * Alunos que este usuário pode ver: o coordenador, os dos cursos a que está vinculado; o admin, todos.
     * A verificação fica aqui, no servidor, e não só no frontend (PLANO-DO-PROJETO.md §4).
     */
    @Transactional(readOnly = true)
    public List<AlunoComConta> visiveis(UUID usuarioId, Papel papel) {
        List<Aluno> lista = switch (papel) {
            case ADMIN -> alunos.todos();
            case COORDENADOR -> {
                List<UUID> cursosDoCoordenador = cursosDe(usuarioId);
                yield cursosDoCoordenador.isEmpty() ? List.of() : alunos.dosCursos(cursosDoCoordenador);
            }
            case ALUNO -> throw ErroDeNegocio.proibido("Só a coordenação vê outros alunos.");
        };
        return comConta(lista);
    }

    /** Um aluno, se este usuário pode vê-lo; senão 404 (não revela que o aluno existe). */
    @Transactional(readOnly = true)
    public AlunoComConta visivel(UUID usuarioId, Papel papel, UUID alunoId) {
        Aluno aluno = alunos.findById(alunoId)
                .filter(a -> switch (papel) {
                    case ADMIN -> true;
                    case COORDENADOR -> cursosDe(usuarioId).contains(a.getCurso().getId());
                    case ALUNO -> false;
                })
                .orElseThrow(() -> ErroDeNegocio.naoEncontrado("Aluno não encontrado."));
        return comConta(List.of(aluno)).getFirst();
    }

    /** A coordenação informa (ou apaga, com nulos) o ingresso do aluno, usado para calcular o período. */
    @Transactional
    public AlunoComConta definirIngresso(UUID usuarioId, Papel papel, UUID alunoId, Integer ano, Integer semestre) {
        visivel(usuarioId, papel, alunoId);
        if ((ano == null) != (semestre == null)) {
            throw ErroDeNegocio.invalido("ingresso-incompleto", "Informe o ano e o semestre de ingresso, ou nenhum dos dois.");
        }
        Aluno aluno = alunos.findById(alunoId).orElseThrow();
        aluno.definirIngresso(ano, semestre);
        return comConta(List.of(alunos.save(aluno))).getFirst();
    }

    /** Todos os cursos ativos: usado para dar acesso à coordenação local em desenvolvimento. */
    @Transactional(readOnly = true)
    public List<UUID> idsDosCursosAtivos() {
        return cursos.ativos().stream().map(Curso::getId).toList();
    }

    private List<UUID> cursosDe(UUID usuarioId) {
        return jdbc.sql("SELECT curso_id FROM coordenacao WHERE usuario_id = ?").param(usuarioId).query(UUID.class).list();
    }

    private List<AlunoComConta> comConta(Collection<Aluno> lista) {
        Map<UUID, ContasService.ContaResumo> porUsuario = contas.contas(lista.stream().map(Aluno::getUsuarioId).toList());
        return lista.stream()
                .map(a -> {
                    ContasService.ContaResumo conta = porUsuario.get(a.getUsuarioId());
                    return new AlunoComConta(a.resumo(), conta == null ? "—" : conta.nome(), conta == null ? "" : conta.email());
                })
                .toList();
    }
}
