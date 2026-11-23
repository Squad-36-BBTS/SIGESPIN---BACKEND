package br.com.bbts.sigespin.dto.interacao;

import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.entity.InteracaoProjeto;

import java.time.LocalDateTime;

public record InteracaoProjetoResponse(
        Long id,
        Long projetoId,
        UsuarioResumoResponse autor,
        String texto,
        LocalDateTime criadoEm
) {
    public static InteracaoProjetoResponse de(InteracaoProjeto interacao) {
        return new InteracaoProjetoResponse(
                interacao.getId(),
                interacao.getProjeto().getId(),
                UsuarioResumoResponse.de(interacao.getAutor()),
                interacao.getTexto(),
                interacao.getCriadoEm()
        );
    }
}
