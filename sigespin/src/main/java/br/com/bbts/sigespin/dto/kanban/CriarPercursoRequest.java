package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.constraints.*;

public record CriarPercursoRequest(@NotNull @Positive Long origemId,
        @NotNull @Positive Long destinoId, @NotNull Boolean exigeGovernanca) {
}
