package br.com.bbts.sigespin.dto.kanban;

import br.com.bbts.sigespin.dto.projeto.EtapaResumoResponse;
import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.entity.MovimentacaoKanban;

import java.time.LocalDateTime;

public record MovimentacaoKanbanResponse(
        Long id,
        Long projetoId,
        UsuarioResumoResponse autor,
        EtapaResumoResponse origem,
        EtapaResumoResponse destino,
        Long solicitacaoId,
        String justificativa,
        LocalDateTime movimentadoEm
) {
    public static MovimentacaoKanbanResponse de(MovimentacaoKanban movimentacao) {
        if (movimentacao == null) {
            return null;
        }
        return new MovimentacaoKanbanResponse(
                movimentacao.getId(),
                movimentacao.getProjeto() != null ? movimentacao.getProjeto().getId() : null,
                UsuarioResumoResponse.de(movimentacao.getAutor()),
                EtapaResumoResponse.de(movimentacao.getOrigem()),
                EtapaResumoResponse.de(movimentacao.getDestino()),
                movimentacao.getSolicitacao() != null ? movimentacao.getSolicitacao().getId() : null,
                movimentacao.getJustificativa(),
                movimentacao.getMovimentadoEm()
        );
    }
}
