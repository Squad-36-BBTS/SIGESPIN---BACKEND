package br.com.bbts.sigespin.dto.gestao;

import jakarta.validation.constraints.NotBlank;

public record RecusarRecomendacaoRequest(@NotBlank String justificativa) {
}
