package br.com.bbts.sigespin.dto.trl;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AvaliacaoTrlRequest(
        @NotNull(message = "O nível TRL é obrigatório.")
        @Min(value = 1, message = "O nível TRL mínimo é 1.")
        @Max(value = 9, message = "O nível TRL máximo é 9.")
        Integer nivel,

        @NotBlank(message = "A justificativa da avaliação de TRL é obrigatória.")
        String justificativa,

        List<Long> evidenciasIds
) { }
