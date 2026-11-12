package br.edu.univassouras.sereno.notificacao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.Repository;

interface AvisoRepository extends Repository<Aviso, UUID> {

    Aviso save(Aviso aviso);

    List<Aviso> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId, Limit limite);

    List<Aviso> findByUsuarioIdAndLidoEmIsNull(UUID usuarioId);

    long countByUsuarioIdAndLidoEmIsNull(UUID usuarioId);
}
