package br.com.bbts.sigespin.security;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioAtualService {
    private final UsuarioRepository usuarios;

    public UsuarioAtualService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** A identidade vem do contexto estabelecido pelo Spring Security, nunca do formulário. */
    public Usuario obterAtivo() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("É necessário estar autenticado.");
        }
        Usuario usuario = usuarios.findByEmailIgnoreCase(autenticacao.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuário indisponível."));
        // Reconfere o estado persistido também em chamadas diretas ao serviço.
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AccessDeniedException("Usuário indisponível.");
        }
        return usuario;
    }

    /** Reconfere o perfil no banco, além da authority verificada por @PreAuthorize. */
    public Usuario exigirPerfil(PerfilUsuario perfil) {
        return exigirUmDosPerfis(perfil);
    }

    public Usuario exigirUmDosPerfis(PerfilUsuario... perfis) {
        Usuario usuario = obterAtivo();
        if (java.util.Arrays.stream(perfis).noneMatch(perfil -> usuario.getPerfil() == perfil)) {
            throw new AccessDeniedException("O perfil atual não permite esta operação.");
        }
        return usuario;
    }
}
