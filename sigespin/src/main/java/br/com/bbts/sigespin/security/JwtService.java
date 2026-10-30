package br.com.bbts.sigespin.security;

import br.com.bbts.sigespin.model.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey chave;
    private final String emissor;
    private final long duracaoSegundos;

    public JwtService(
            @Value("${app.security.jwt.secret}") String segredo,
            @Value("${app.security.jwt.issuer:sigespin}") String emissor,
            @Value("${app.security.jwt.expiration-seconds:900}") long duracaoSegundos) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa conter ao menos 32 bytes.");
        }
        if (duracaoSegundos < 60) {
            throw new IllegalStateException("A duração do JWT deve ser de pelo menos 60 segundos.");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.emissor = emissor;
        this.duracaoSegundos = duracaoSegundos;
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant emitidoEm = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEm = emitidoEm.plusSeconds(duracaoSegundos);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer(emissor)
                .subject(usuario.getEmail())
                .id(jti)
                .claim("uid", usuario.getId())
                .issuedAt(Date.from(emitidoEm))
                .expiration(Date.from(expiraEm))
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
        return new TokenEmitido(token, jti, expiraEm);
    }

    public Claims validarEObterClaims(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(chave)
                .requireIssuer(emissor)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    public record TokenEmitido(String valor, String jti, Instant expiraEm) { }
}
