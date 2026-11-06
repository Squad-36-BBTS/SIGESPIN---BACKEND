package br.com.bbts.sigespin.dto.entrega;

import java.time.LocalDateTime;

public record EntregaRegistradaResponse(Long id, Long projetoId, Long autorId, String descricao, LocalDateTime submetidoEm) {
}
