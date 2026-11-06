package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.constraints.NotBlank;

public record CancelarGovernancaRequest(@NotBlank String justificativa) {
}
