package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.usuario.UsuarioResponse;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * BE-07 — GET /api/usuarios?perfil=&ativo=
 * Lista usuários com filtros opcionais de perfil e situação.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<UsuarioResponse> listar(
            @RequestParam(required = false) PerfilUsuario perfil,
            @RequestParam(required = false) Boolean ativo) {
        return usuarioService.listar(perfil, ativo);
    }
}
