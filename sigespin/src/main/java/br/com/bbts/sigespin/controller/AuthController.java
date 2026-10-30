package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.auth.LoginRequest;
import br.com.bbts.sigespin.dto.auth.LoginResponse;
import br.com.bbts.sigespin.dto.auth.UsuarioAutenticadoResponse;
import br.com.bbts.sigespin.security.ApiErroResponse;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import br.com.bbts.sigespin.service.AutenticacaoService;
import br.com.bbts.sigespin.service.CredenciaisInvalidasException;
import br.com.bbts.sigespin.service.TokenJwtRevogacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacaoService autenticacaoService;
    private final TokenJwtRevogacaoService revogacaoService;

    public AuthController(AutenticacaoService autenticacaoService, TokenJwtRevogacaoService revogacaoService) {
        this.autenticacaoService = autenticacaoService;
        this.revogacaoService = revogacaoService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacaoService.autenticar(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UsuarioPrincipal principal) {
        revogacaoService.revogar(principal.getJti(), principal.getExpiraEm());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioAutenticadoResponse usuarioAtual(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return autenticacaoService.paraResposta(principal.getUsuario());
    }
}

@RestControllerAdvice(assignableTypes = AuthController.class)
class AuthExceptionHandler {

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ResponseEntity<ApiErroResponse> credenciaisInvalidas(CredenciaisInvalidasException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErroResponse(
                Instant.now(), HttpStatus.UNAUTHORIZED.value(), "CREDENCIAIS_INVALIDAS",
                exception.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErroResponse> requisicaoInvalida(MethodArgumentNotValidException exception) {
        List<String> detalhes = exception.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(new ApiErroResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDACAO_FALHOU",
                "Revise os campos informados.", detalhes));
    }
}
