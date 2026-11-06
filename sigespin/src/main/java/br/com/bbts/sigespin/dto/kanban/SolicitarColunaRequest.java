package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record SolicitarColunaRequest(@NotNull @Valid CriarColunaRequest proposta,
        @NotBlank String justificativa) {
}
