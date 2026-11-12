package br.edu.univassouras.sereno.academico;

import br.edu.univassouras.sereno.comum.ErroDeNegocio;
import br.edu.univassouras.sereno.identidade.ContasService;
import br.edu.univassouras.sereno.identidade.Papel;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Equipe da coordenação: quem analisa certificados e de quais cursos. Regras de quem pode criar quem:
 * <ul>
 *   <li>o administrador cria coordenadores (de qualquer curso) e outros administradores;</li>
 *   <li>o coordenador cria coordenadores, só para os cursos que ele mesmo coordena.</li>
 * </ul>
 */
@Service
class EquipeService {

    record MembroDto(UUID id, String nome, String email, Papel papel, List<AlunoService.CursoResumo> cursos, Instant criadoEm) {
    }

    /**
     * @param cursosQuePodeAtribuir cursos que quem está logado pode dar a um novo coordenador
     */
    record EquipeDto(List<MembroDto> membros, boolean podeCriarAdmin, List<AlunoService.CursoResumo> cursosQuePodeAtribuir) {
    }

    record NovoMembro(String nome, String email, String senha, Papel papel, Collection<UUID> cursoIds) {
    }

    private final ContasService contas;
    private final AlunoService alunos;
    private final JdbcClient jdbc;
    private final jakarta.persistence.EntityManager entityManager;

    EquipeService(ContasService contas, AlunoService alunos, JdbcClient jdbc, jakarta.persistence.EntityManager entityManager) {
        this.contas = contas;
        this.alunos = alunos;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    EquipeDto equipe(UUID quem, Papel papel) {
        Map<UUID, List<AlunoService.CursoResumo>> cursosPorUsuario = cursosPorUsuario();
        List<MembroDto> membros = contas.equipe().stream()
                .map(m -> new MembroDto(m.id(), m.nome(), m.email(), m.papel(),
                        m.papel() == Papel.ADMIN ? List.of() : cursosPorUsuario.getOrDefault(m.id(), List.of()), m.criadoEm()))
                .toList();
        return new EquipeDto(membros, papel == Papel.ADMIN, cursosQuePodeAtribuir(quem, papel, cursosPorUsuario));
    }

    @Transactional
    MembroDto criar(UUID quem, Papel papelDeQuem, NovoMembro novo) {
        if (novo.papel() == Papel.ALUNO) {
            throw ErroDeNegocio.invalido("papel-invalido", "Alunos criam a própria conta pela tela de cadastro.");
        }
        if (novo.papel() == Papel.ADMIN && papelDeQuem != Papel.ADMIN) {
            throw ErroDeNegocio.proibido("Só a administração pode criar outro administrador.");
        }
        Set<UUID> cursos = new LinkedHashSet<>(novo.cursoIds() == null ? List.of() : novo.cursoIds());
        if (novo.papel() == Papel.COORDENADOR) {
            if (cursos.isEmpty()) {
                throw ErroDeNegocio.invalido("sem-curso", "Escolha pelo menos um curso para o coordenador.");
            }
            Set<UUID> permitidos = new java.util.HashSet<>(cursosQuePodeAtribuir(quem, papelDeQuem, cursosPorUsuario()).stream()
                    .map(AlunoService.CursoResumo::id).toList());
            if (!permitidos.containsAll(cursos)) {
                throw ErroDeNegocio.proibido("Você só pode dar acesso aos cursos que coordena.");
            }
        }
        UUID id = contas.criarConta(new ContasService.NovaConta(novo.email(), novo.nome(), novo.senha(), novo.papel()));
        // A conta vem do JPA e o vínculo é gravado por SQL: grava a conta antes, por causa da chave estrangeira.
        entityManager.flush();
        if (novo.papel() == Papel.COORDENADOR) {
            cursos.forEach(curso -> alunos.vincularCoordenador(id, curso));
        }
        return equipe(quem, papelDeQuem).membros().stream()
                .filter(m -> m.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private List<AlunoService.CursoResumo> cursosQuePodeAtribuir(UUID quem, Papel papel,
            Map<UUID, List<AlunoService.CursoResumo>> cursosPorUsuario) {
        return papel == Papel.ADMIN ? alunos.cursosAtivos() : cursosPorUsuario.getOrDefault(quem, List.of());
    }

    private Map<UUID, List<AlunoService.CursoResumo>> cursosPorUsuario() {
        Map<UUID, List<AlunoService.CursoResumo>> mapa = new HashMap<>();
        jdbc.sql("""
                SELECT co.usuario_id, c.id, c.nome, c.sigla
                FROM coordenacao co JOIN curso c ON c.id = co.curso_id
                WHERE c.ativo
                ORDER BY c.nome""")
                .query((rs, n) -> {
                    mapa.computeIfAbsent(rs.getObject("usuario_id", UUID.class), k -> new java.util.ArrayList<>())
                            .add(new AlunoService.CursoResumo(rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("sigla")));
                    return null;
                })
                .list();
        return mapa;
    }
}
