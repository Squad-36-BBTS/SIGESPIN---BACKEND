package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SolicitacaoTransicaoRequest(
        @NotNull(message = "O identificador da transição configurada é obrigatório.")
        Long transicaoId,

        @NotBlank(message = "A justificativa para a transição é obrigatória.")
        String justificativa,

        List<Long> evidenciasIds
) { }
