package br.com.bbts.sigespin.dto.entrega;

import jakarta.validation.constraints.NotBlank;

public record SubmeterEntregaRequest(@NotBlank String descricao) {
}
