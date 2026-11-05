package br.com.bbts.sigespin.dto.entrega;

import jakarta.validation.constraints.NotBlank;

public record EntregaProjetoRequest(
        @NotBlank(message = "A descrição da entrega/MVP é obrigatória.")
        String descricao
) { }
