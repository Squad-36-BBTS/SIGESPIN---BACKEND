package br.com.bbts.sigespin.dto.qualificacao;

import java.time.LocalDateTime;

public record QualificacaoRegistradaResponse(
        Long id, Long projetoId, Long autorId,
        String aderenciaTese, String relevancia, String grauInovacao, String viabilidade,
        String maturidadeTecnologica, String fundamentacao, String conclusao,
        LocalDateTime criadoEm) {
}
