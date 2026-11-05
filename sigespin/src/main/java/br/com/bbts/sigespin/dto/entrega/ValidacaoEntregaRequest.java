package br.com.bbts.sigespin.dto.entrega;

import br.com.bbts.sigespin.model.enums.ResultadoValidacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ValidacaoEntregaRequest(
        @NotNull(message = "O resultado da validação é obrigatório.")
        ResultadoValidacao resultado,

        @NotBlank(message = "As observações da validação são obrigatórias.")
        String observacoes
) { }
