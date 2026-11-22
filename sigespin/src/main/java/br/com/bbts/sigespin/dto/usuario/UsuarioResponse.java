package br.com.bbts.sigespin.dto.usuario;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;

/**
 * DTO de resposta para consulta de usuários.
 * Nunca expõe senhaHash nem tokens.
 */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        Boolean ativo
) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.getAtivo());
    }
}
