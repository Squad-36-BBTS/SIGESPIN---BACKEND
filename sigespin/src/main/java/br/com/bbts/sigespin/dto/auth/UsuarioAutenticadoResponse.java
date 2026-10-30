package br.com.bbts.sigespin.dto.auth;

import br.com.bbts.sigespin.model.enums.PerfilUsuario;

public record UsuarioAutenticadoResponse(Long id, String nome, String email, PerfilUsuario perfil, boolean ativo) { }
