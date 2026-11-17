package br.com.bbts.sigespin.dto.trl;

import java.time.LocalDateTime;
import java.util.List;

public record AvaliacaoTrlRegistradaResponse(
        Long id, Long projetoId, Long autorId, Integer nivelAnterior, Integer nivel,
        String justificativa, List<Long> evidenciaIds, LocalDateTime avaliadoEm) {
    public AvaliacaoTrlRegistradaResponse {
        evidenciaIds = List.copyOf(evidenciaIds);
    }
}
