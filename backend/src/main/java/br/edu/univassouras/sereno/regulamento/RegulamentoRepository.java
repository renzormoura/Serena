package br.edu.univassouras.sereno.regulamento;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface RegulamentoRepository extends Repository<Regulamento, UUID> {

    Optional<Regulamento> findById(UUID id);

    /** O publicado de maior versão. Quando houver vínculo aluno↔regulamento (F1), o aluno usa o seu. */
    @Query("""
            select r from Regulamento r
            where r.publicado = true
            order by r.versao desc
            limit 1""")
    Optional<Regulamento> findVigente();
}
