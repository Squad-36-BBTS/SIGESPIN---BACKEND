package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.constraints.*;

/** O percurso mantém origem e destino. Uma rota diferente recebe outro registro. */
public record EditarPercursoRequest(@NotNull Boolean exigeGovernanca,
        @NotNull Boolean ativa, @NotNull @Min(0) Long versao) {
}
