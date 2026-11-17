package br.com.bbts.sigespin.dto.entrega;

import br.com.bbts.sigespin.model.enums.ResultadoValidacao;
import java.time.LocalDateTime;

public record ValidacaoRegistradaResponse(Long id, Long projetoId, Long entregaId, Long demandanteId,
        ResultadoValidacao resultado, String observacoes, LocalDateTime validadoEm) {
}
