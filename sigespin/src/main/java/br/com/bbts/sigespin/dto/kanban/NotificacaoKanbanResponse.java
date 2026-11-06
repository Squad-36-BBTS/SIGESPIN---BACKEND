package br.com.bbts.sigespin.dto.kanban;

import java.time.LocalDateTime;

public record NotificacaoKanbanResponse(Long id, Long movimentacaoId, Long projetoId,
        String mensagem, LocalDateTime criadaEm) {
}
