package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;

public record UsuarioResumoResponse(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil
) {
    public static UsuarioResumoResponse de(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResumoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil()
        );
    }
}
