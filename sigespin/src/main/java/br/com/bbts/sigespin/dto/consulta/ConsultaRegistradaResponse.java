package br.com.bbts.sigespin.dto.consulta;

import java.time.LocalDateTime;

public record ConsultaRegistradaResponse(
        Long id, Long projetoId, Long solicitanteId, Long especialistaId,
        String questao, LocalDateTime criadoEm) {
}
