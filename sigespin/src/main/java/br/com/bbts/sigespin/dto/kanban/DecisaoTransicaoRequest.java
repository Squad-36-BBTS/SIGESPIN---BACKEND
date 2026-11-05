package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DecisaoTransicaoRequest(
        @NotNull(message = "A decisão de aprovação ou rejeição é obrigatória.")
        Boolean aprovada,

        @NotBlank(message = "A justificativa da decisão é obrigatória.")
        String justificativa
) { }
