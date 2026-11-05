package br.com.bbts.sigespin.dto.entrega;

import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.entity.ValidacaoEntrega;
import br.com.bbts.sigespin.model.enums.ResultadoValidacao;

import java.time.LocalDateTime;

public record ValidacaoEntregaResponse(
        Long id,
        Long projetoId,
        Long entregaId,
        UsuarioResumoResponse demandante,
        ResultadoValidacao resultado,
        String observacoes,
        LocalDateTime validadoEm
) {
    public static ValidacaoEntregaResponse de(ValidacaoEntrega validacao) {
        if (validacao == null) {
            return null;
        }
        return new ValidacaoEntregaResponse(
                validacao.getId(),
                validacao.getProjeto() != null ? validacao.getProjeto().getId() : null,
                validacao.getEntrega() != null ? validacao.getEntrega().getId() : null,
                UsuarioResumoResponse.de(validacao.getDemandante()),
                validacao.getResultado(),
                validacao.getObservacoes(),
                validacao.getValidadoEm()
        );
    }
}
