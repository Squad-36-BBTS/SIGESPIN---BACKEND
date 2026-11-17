package br.com.bbts.sigespin.dto.kanban;

import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import java.time.LocalDateTime;

public record SolicitacaoGovernancaResponse(Long id, Long projetoId, Long solicitanteId,
        Long transicaoId, Long origemId, Long destinoId, StatusSolicitacao status,
        String justificativa, LocalDateTime criadoEm, Long responsavelEncerramentoId,
        String justificativaEncerramento, LocalDateTime encerradoEm) {
}
