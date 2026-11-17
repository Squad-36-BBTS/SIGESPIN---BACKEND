package br.com.bbts.sigespin.dto.gestao;

import jakarta.validation.constraints.*;

public record RecomendarDemandaRequest(@NotNull @Positive Long destinatarioId,
        @NotBlank @Size(max = 255) String titulo, @NotBlank String descricao) {
}
