package br.com.bbts.sigespin.dto.gestao;

import jakarta.validation.constraints.*;

public record RegistrarCorrecaoRequest(@NotBlank String descricao,
        @Size(max = 500) String referencia, @Positive Long evidenciaId) {
}
