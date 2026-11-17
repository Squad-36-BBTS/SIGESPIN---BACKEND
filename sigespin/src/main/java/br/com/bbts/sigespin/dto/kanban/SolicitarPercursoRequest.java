package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record SolicitarPercursoRequest(@NotNull @Valid CriarPercursoRequest proposta,
        @NotBlank String justificativa) {
}
