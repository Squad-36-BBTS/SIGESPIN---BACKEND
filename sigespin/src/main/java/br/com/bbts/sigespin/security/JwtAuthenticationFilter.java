package br.com.bbts.sigespin.security;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.repository.seguranca.TokenJwtRevogadoRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenJwtRevogadoRepository tokenRevogadoRepository;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            TokenJwtRevogadoRepository tokenRevogadoRepository,
            UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.tokenRevogadoRepository = tokenRevogadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        try {
            Claims claims = jwtService.validarEObterClaims(token);
            String jti = claims.getId();
            if (jti == null || tokenRevogadoRepository.existsById(jti)) {
                SecurityContextHolder.clearContext();
            } else {
                Number identificador = claims.get("uid", Number.class);
                if (identificador == null) {
                    throw new IllegalArgumentException("JWT sem identificador do usuário");
                }
                Long usuarioId = identificador.longValue();
                Usuario usuario = usuarioRepository.findByEmailIgnoreCase(claims.getSubject())
                        .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
                if (Boolean.TRUE.equals(usuario.getAtivo()) && usuario.getId().equals(usuarioId)) {
                    UsuarioPrincipal principal = UsuarioPrincipal.paraToken(
                            usuario, jti, claims.getExpiration().toInstant());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
