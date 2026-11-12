package br.edu.univassouras.sereno.identidade;

import br.edu.univassouras.sereno.comum.ErroDeNegocio;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API pública do módulo: criação de contas. Também é a fonte de usuários do Spring Security. */
@Service
public class ContasService implements UserDetailsService {

    /** Regra de senha (pedido de 11/11/2026): pelo menos 8 caracteres, uma letra e um caractere especial. */
    public static final int SENHA_MINIMA = 8;
    public static final String REGRA_SENHA = "A senha precisa ter pelo menos 8 caracteres, com pelo menos uma letra e um caractere especial (ex.: ! @ # -).";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final List<String> dominiosPermitidos;
    private final org.springframework.beans.factory.ObjectProvider<IdentificadorDeLogin> outrosIdentificadores;

    ContasService(UsuarioRepository usuarios, PasswordEncoder encoder,
            @Value("${sereno.cadastro.dominios-permitidos:}") List<String> dominiosPermitidos,
            org.springframework.beans.factory.ObjectProvider<IdentificadorDeLogin> outrosIdentificadores) {
        this.outrosIdentificadores = outrosIdentificadores;
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.dominiosPermitidos = dominiosPermitidos.stream().map(String::strip).filter(d -> !d.isEmpty()).toList();
    }

    public record NovaConta(String email, String nome, String senha, Papel papel) {
    }

    @Transactional
    public UUID criarConta(NovaConta conta) {
        String email = Usuario.normalizarEmail(conta.email());
        if (!dominiosPermitidos.isEmpty() && dominiosPermitidos.stream().noneMatch(d -> email.endsWith("@" + d))) {
            throw ErroDeNegocio.invalido("email-dominio", "Use seu e-mail institucional (" + String.join(", ", dominiosPermitidos) + ").");
        }
        if (!senhaAceita(conta.senha())) {
            throw ErroDeNegocio.invalido("senha-fraca", REGRA_SENHA);
        }
        if (usuarios.existsByEmail(email)) {
            throw ErroDeNegocio.conflito("email-em-uso", "Já existe uma conta com este e-mail.");
        }
        return usuarios.save(new Usuario(email, conta.nome(), encoder.encode(conta.senha()), conta.papel())).getId();
    }

    /** Pelo menos 8 caracteres, uma letra e um caractere que não seja letra, número nem espaço. */
    public static boolean senhaAceita(String senha) {
        return senha != null && senha.length() >= SENHA_MINIMA
                && senha.codePoints().anyMatch(Character::isLetter)
                && senha.codePoints().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
    }

    @Transactional(readOnly = true)
    public boolean existe(String email) {
        return usuarios.existsByEmail(Usuario.normalizarEmail(email));
    }

    /** Nome e e-mail de uma conta, para mostrar quem é quem (alunos na fila, quem decidiu). */
    public record ContaResumo(UUID id, String nome, String email) {
    }

    @Transactional(readOnly = true)
    public Map<UUID, ContaResumo> contas(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return usuarios.findByIdIn(ids).stream()
                .map(u -> new ContaResumo(u.getId(), u.getNome(), u.getEmail()))
                .collect(Collectors.toMap(ContaResumo::id, Function.identity()));
    }

    /** Uma conta da equipe (coordenação ou administração). */
    public record Membro(UUID id, String nome, String email, Papel papel, Instant criadoEm) {
    }

    @Transactional(readOnly = true)
    public List<Membro> equipe() {
        return usuarios.findByPapelInOrderByNome(List.of(Papel.COORDENADOR, Papel.ADMIN)).stream()
                .map(u -> new Membro(u.getId(), u.getNome(), u.getEmail(), u.getPapel(), u.getCriadoEm()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UUID> idPorEmail(String email) {
        return usuarios.findByEmail(Usuario.normalizarEmail(email)).map(Usuario::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identificador) {
        return buscarParaLogin(identificador == null ? "" : identificador.strip())
                .map(Usuario::paraAutenticado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    /** O mesmo campo aceita e-mail ou, sem "@", outro identificador (a matrícula). */
    private Optional<Usuario> buscarParaLogin(String identificador) {
        if (identificador.isEmpty()) {
            return Optional.empty();
        }
        if (identificador.contains("@")) {
            return usuarios.findByEmail(Usuario.normalizarEmail(identificador));
        }
        return outrosIdentificadores.orderedStream()
                .map(i -> i.usuarioDe(identificador))
                .flatMap(Optional::stream)
                .findFirst()
                .flatMap(usuarios::findById);
    }

    @EventListener
    @Transactional
    void aoAutenticar(AuthenticationSuccessEvent evento) {
        if (evento.getAuthentication().getPrincipal() instanceof UsuarioAutenticado u) {
            usuarios.findByEmail(u.email()).ifPresent(usuario -> usuario.registrarLogin(Instant.now()));
        }
    }
}
