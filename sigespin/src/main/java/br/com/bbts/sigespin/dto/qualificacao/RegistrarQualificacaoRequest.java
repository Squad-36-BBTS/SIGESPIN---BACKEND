package br.com.bbts.sigespin.dto.qualificacao;

import jakarta.validation.constraints.NotBlank;

/** Avaliação textual do Analista; maturidade aqui não altera o indicador TRL. */
public record RegistrarQualificacaoRequest(
        @NotBlank String aderenciaTese,
        @NotBlank String relevancia,
        @NotBlank String grauInovacao,
        @NotBlank String viabilidade,
        @NotBlank String maturidadeTecnologica,
        @NotBlank String fundamentacao,
        @NotBlank String conclusao) {
}
