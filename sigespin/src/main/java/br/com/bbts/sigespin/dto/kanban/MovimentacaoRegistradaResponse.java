package br.com.bbts.sigespin.dto.kanban;

import java.time.LocalDateTime;

public record MovimentacaoRegistradaResponse(Long id, Long projetoId, Long autorId,
        Long origemId, Long destinoId, Long solicitacaoId, String justificativa, LocalDateTime movimentadoEm) {
}
