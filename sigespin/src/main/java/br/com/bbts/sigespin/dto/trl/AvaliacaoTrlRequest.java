package br.com.bbts.sigespin.dto.trl;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

import java.util.List;

public record AvaliacaoTrlRequest(
        @NotNull(message = "O nível TRL é obrigatório.")
        @Min(value = 1, message = "O nível TRL mínimo é 1.")
        @Max(value = 9, message = "O nível TRL máximo é 9.")
        Integer nivel,

        @NotBlank(message = "A justificativa da avaliação de TRL é obrigatória.")
        String justificativa,

        @JsonAlias("evidenciaIds") @NotEmpty List<@NotNull @Positive Long> evidenciasIds
) { }
