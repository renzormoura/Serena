package br.edu.univassouras.sereno.identidade;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface UsuarioRepository extends Repository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findById(UUID id);

    boolean existsByEmail(String email);

    List<Usuario> findByIdIn(Collection<UUID> ids);

    List<Usuario> findByPapelInOrderByNome(Collection<Papel> papeis);

    Usuario save(Usuario usuario);
}
