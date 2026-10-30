package br.com.bbts.sigespin.dto.auth;

import java.time.Instant;

public record LoginResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioAutenticadoResponse usuario) { }
