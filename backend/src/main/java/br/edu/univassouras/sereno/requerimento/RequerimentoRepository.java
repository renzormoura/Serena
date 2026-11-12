package br.edu.univassouras.sereno.requerimento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface RequerimentoRepository extends Repository<Requerimento, UUID> {

    Requerimento save(Requerimento requerimento);

    Optional<Requerimento> findById(UUID id);

    List<Requerimento> findByAlunoIdOrderByNumeroDesc(UUID alunoId);

    @Query("select coalesce(max(r.numero), 0) from Requerimento r where r.alunoId = :alunoId")
    int ultimoNumero(UUID alunoId);
}
