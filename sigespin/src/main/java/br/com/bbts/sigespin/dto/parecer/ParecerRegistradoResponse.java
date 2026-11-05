package br.com.bbts.sigespin.dto.parecer;

import java.time.LocalDateTime;
import java.util.List;

public record ParecerRegistradoResponse(
        Long id, Long projetoId, Long autorId, Long consultaId, String fundamentacao,
        String conclusao, List<Long> evidenciaIds, LocalDateTime criadoEm) {
    public ParecerRegistradoResponse {
        evidenciaIds = List.copyOf(evidenciaIds);
    }
}
