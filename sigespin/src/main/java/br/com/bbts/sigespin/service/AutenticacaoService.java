package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.auth.LoginRequest;
import br.com.bbts.sigespin.dto.auth.LoginResponse;
import br.com.bbts.sigespin.dto.auth.UsuarioAutenticadoResponse;
import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.security.JwtService;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AutenticacaoService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse autenticar(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().trim().toLowerCase(Locale.ROOT), request.senha()));
            Usuario usuario = ((UsuarioPrincipal) authentication.getPrincipal()).getUsuario();
            JwtService.TokenEmitido emitido = jwtService.emitir(usuario);
            return new LoginResponse(emitido.valor(), "Bearer", emitido.expiraEm(), paraResposta(usuario));
        } catch (AuthenticationException exception) {
            throw new CredenciaisInvalidasException();
        }
    }

    public UsuarioAutenticadoResponse paraResposta(Usuario usuario) {
        return new UsuarioAutenticadoResponse(
                usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
                Boolean.TRUE.equals(usuario.getAtivo()));
    }
}
