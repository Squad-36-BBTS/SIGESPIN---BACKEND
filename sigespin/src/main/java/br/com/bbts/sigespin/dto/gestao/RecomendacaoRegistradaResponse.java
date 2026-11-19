package br.com.bbts.sigespin.dto.gestao;

import br.com.bbts.sigespin.model.enums.StatusRecomendacao;
import java.time.LocalDateTime;

public record RecomendacaoRegistradaResponse(Long id, Long autorId, Long destinatarioId,
        String titulo, String descricao, StatusRecomendacao status, Long projetoId,
        LocalDateTime criadoEm, LocalDateTime encerradoEm, String justificativaEncerramento) {
}
