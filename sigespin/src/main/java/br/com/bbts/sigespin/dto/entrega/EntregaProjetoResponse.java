package br.com.bbts.sigespin.dto.entrega;

import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.entity.EntregaProjeto;

import java.time.LocalDateTime;

public record EntregaProjetoResponse(
        Long id,
        Long projetoId,
        UsuarioResumoResponse autor,
        String descricao,
        LocalDateTime submetidoEm
) {
    public static EntregaProjetoResponse de(EntregaProjeto entrega) {
        if (entrega == null) {
            return null;
        }
        return new EntregaProjetoResponse(
                entrega.getId(),
                entrega.getProjeto() != null ? entrega.getProjeto().getId() : null,
                UsuarioResumoResponse.de(entrega.getAutor()),
                entrega.getDescricao(),
                entrega.getSubmetidoEm()
        );
    }
}
