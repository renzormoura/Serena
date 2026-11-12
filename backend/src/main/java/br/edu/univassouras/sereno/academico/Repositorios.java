package br.edu.univassouras.sereno.academico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface AlunoRepository extends Repository<Aluno, UUID> {

    Optional<Aluno> findByUsuarioId(UUID usuarioId);

    boolean existsByMatricula(String matricula);

    Optional<Aluno> findByMatriculaIgnoreCase(String matricula);

    Optional<Aluno> findById(UUID id);

    @Query("select a from Aluno a join fetch a.curso order by a.matricula")
    List<Aluno> todos();

    @Query("select a from Aluno a join fetch a.curso c where c.id in :cursos order by a.matricula")
    List<Aluno> dosCursos(java.util.Collection<UUID> cursos);

    Aluno save(Aluno aluno);
}

interface CursoRepository extends Repository<Curso, UUID> {

    Optional<Curso> findById(UUID id);

    @Query("select c from Curso c where c.ativo = true order by c.nome")
    List<Curso> ativos();
}
