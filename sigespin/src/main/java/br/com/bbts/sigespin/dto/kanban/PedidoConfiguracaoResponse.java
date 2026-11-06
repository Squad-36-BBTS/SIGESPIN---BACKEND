package br.com.bbts.sigespin.dto.kanban;

import br.com.bbts.sigespin.model.enums.*;
import java.time.LocalDateTime;

public record PedidoConfiguracaoResponse(Long id, TipoConfiguracaoKanban tipo, Long solicitanteId,
        CriarColunaRequest coluna, CriarPercursoRequest percurso, String justificativa,
        StatusSolicitacao status, LocalDateTime criadoEm, Long responsavelEncerramentoId,
        String justificativaEncerramento, LocalDateTime encerradoEm, Long colunaCriadaId,
        Long transicaoCriadaId, Long versao) {
}
