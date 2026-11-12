package br.edu.univassouras.sereno.extracao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface ExtracaoRepository extends Repository<Extracao, UUID> {

    Extracao save(Extracao extracao);

    Optional<Extracao> findFirstByCertificadoIdOrderByCriadoEmDesc(UUID certificadoId);

    /** A leitura mais recente de cada certificado da lista. */
    @Query("""
            select e from Extracao e
            where e.certificadoId in :ids
              and e.criadoEm = (select max(x.criadoEm) from Extracao x where x.certificadoId = e.certificadoId)""")
    List<Extracao> maisRecentes(Collection<UUID> ids);
}
