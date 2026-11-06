package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.constraints.*;

/** A transição configurada determina origem e destino, sem aceitar etapas arbitrárias. */
public record MovimentarProjetoRequest(@NotNull @Positive Long transicaoId, @NotBlank String justificativa) {
}
