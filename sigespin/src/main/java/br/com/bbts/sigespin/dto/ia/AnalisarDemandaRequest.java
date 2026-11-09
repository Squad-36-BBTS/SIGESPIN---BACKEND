package br.com.bbts.sigespin.dto.ia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalisarDemandaRequest(
        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres.")
        String titulo,

        @NotBlank(message = "A descrição da dor é obrigatória.")
        @Size(max = 4000, message = "A descrição deve ter no máximo 4000 caracteres.")
        String descricao
) {
}
