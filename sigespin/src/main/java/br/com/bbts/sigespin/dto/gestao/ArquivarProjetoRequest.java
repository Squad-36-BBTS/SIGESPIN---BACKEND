package br.com.bbts.sigespin.dto.gestao;

import jakarta.validation.constraints.*;

public record ArquivarProjetoRequest(@NotNull Boolean arquivado,
        @NotNull @Min(0) Long versao, @NotBlank String justificativa) {
}
