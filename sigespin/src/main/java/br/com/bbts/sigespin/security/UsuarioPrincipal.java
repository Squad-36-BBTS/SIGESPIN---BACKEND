package br.com.bbts.sigespin.security;

import br.com.bbts.sigespin.model.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;
    private final String jti;
    private final Instant expiraEm;

    private UsuarioPrincipal(Usuario usuario, String jti, Instant expiraEm) {
        this.usuario = usuario;
        this.jti = jti;
        this.expiraEm = expiraEm;
    }

    public static UsuarioPrincipal paraLogin(Usuario usuario) {
        return new UsuarioPrincipal(usuario, null, null);
    }

    public static UsuarioPrincipal paraToken(Usuario usuario, String jti, Instant expiraEm) {
        return new UsuarioPrincipal(usuario, jti, expiraEm);
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getJti() {
        return jti;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getSenhaHash();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(usuario.getAtivo());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
