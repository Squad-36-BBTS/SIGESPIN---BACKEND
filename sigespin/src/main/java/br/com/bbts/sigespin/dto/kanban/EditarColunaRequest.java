package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.constraints.*;

/** Versão é a que o cliente leu. Detecta edição feita sobre configuração antiga. */
public record EditarColunaRequest(@NotBlank @Size(max = 255) String nome,
        @NotNull @Min(0) Integer ordem, @NotNull Boolean ativa,
        @NotNull @Min(0) Long versao) {
}
