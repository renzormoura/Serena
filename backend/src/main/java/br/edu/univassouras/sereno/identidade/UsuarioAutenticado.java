package br.edu.univassouras.sereno.identidade;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * O usuário da sessão. Os outros módulos o recebem com {@code @AuthenticationPrincipal UsuarioAutenticado}.
 * Vai serializado para o Spring Session; o hash da senha é apagado logo após a autenticação.
 */
public final class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String nome;
    private final Papel papel;
    private final boolean ativo;
    private String senhaHash;

    UsuarioAutenticado(UUID id, String email, String nome, Papel papel, String senhaHash, boolean ativo) {
        this.id = id;
        this.email = email;
        this.nome = nome;
        this.papel = papel;
        this.senhaHash = senhaHash;
        this.ativo = ativo;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String nome() {
        return nome;
    }

    public Papel papel() {
        return papel;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }

    @Override
    public void eraseCredentials() {
        senhaHash = null;
    }
}
