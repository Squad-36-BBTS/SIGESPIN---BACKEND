package br.com.bbts.sigespin.dto.entrega;

import br.com.bbts.sigespin.model.enums.ResultadoValidacao;
import jakarta.validation.constraints.*;

public record RegistrarValidacaoRequest(@NotNull ResultadoValidacao resultado, @NotBlank String observacoes) {
}
