package br.com.bbts.sigespin.dto.gestao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record AtualizarProjetoRequest(@NotNull @Valid DadosProjetoRequest dados,
        @NotNull @Min(0) Long versao, @NotBlank String justificativa) {
}
