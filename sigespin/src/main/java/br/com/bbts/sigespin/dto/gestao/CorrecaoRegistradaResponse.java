package br.com.bbts.sigespin.dto.gestao;

import java.time.LocalDateTime;

public record CorrecaoRegistradaResponse(Long id, Long projetoId, Long autorId,
        String descricao, String referencia, Long evidenciaId, LocalDateTime criadoEm) {
}
