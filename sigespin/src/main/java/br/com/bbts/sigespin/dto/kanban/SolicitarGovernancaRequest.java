package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.constraints.*;
import java.util.List;

public record SolicitarGovernancaRequest(
        @NotNull @Positive Long transicaoId,
        @NotBlank String justificativa,
        @NotEmpty List<@NotNull @Positive Long> evidenciaIds) {
}
