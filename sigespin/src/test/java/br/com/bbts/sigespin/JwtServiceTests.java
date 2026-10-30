package br.com.bbts.sigespin;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.security.JwtService;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTests {

    private static final String SEGREDO = "local-test-secret-key-that-is-long-enough-32-bytes";

    @Test
    void tokenAssinadoContemIdentidadeExpiracaoEIdentificadorUnico() {
        JwtService service = new JwtService(SEGREDO, "sigespin-test", 900);
        Usuario usuario = usuario();

        JwtService.TokenEmitido emitido = service.emitir(usuario);
        var claims = service.validarEObterClaims(emitido.valor());

        assertEquals("sigespin-test", claims.getIssuer());
        assertEquals(usuario.getEmail(), claims.getSubject());
        assertEquals(usuario.getId(), claims.get("uid", Number.class).longValue());
        assertEquals(emitido.jti(), claims.getId());
        assertEquals(emitido.expiraEm(), claims.getExpiration().toInstant());
        assertTrue(emitido.expiraEm().isAfter(claims.getIssuedAt().toInstant()));
    }

    @Test
    void tokenComOutraChaveOuEmissorERejeitado() {
        JwtService service = new JwtService(SEGREDO, "sigespin-test", 900);
        String token = service.emitir(usuario()).valor();

        assertThrows(JwtException.class,
                () -> new JwtService("another-local-test-secret-key-32-bytes", "sigespin-test", 900)
                        .validarEObterClaims(token));
        assertThrows(JwtException.class,
                () -> new JwtService(SEGREDO, "outro-emissor", 900).validarEObterClaims(token));
    }

    @Test
    void principalUsaPerfilComoAuthorityERefleteContaInativa() {
        Usuario usuario = usuario();
        usuario.setAtivo(false);

        UsuarioPrincipal principal = UsuarioPrincipal.paraLogin(usuario);

        assertEquals("ROLE_ESPECIALISTA", principal.getAuthorities().iterator().next().getAuthority());
        assertFalse(principal.isEnabled());
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setNome("Especialista de Teste");
        usuario.setEmail("especialista@teste.local");
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(PerfilUsuario.ESPECIALISTA);
        usuario.setAtivo(true);
        return usuario;
    }
}
