package br.com.bbts.sigespin.dto.kanban;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

import java.util.List;

public record SolicitacaoTransicaoRequest(
        @NotNull(message = "O identificador da transição configurada é obrigatório.")
        Long transicaoId,

        @NotBlank(message = "A justificativa para a transição é obrigatória.")
        String justificativa,

        @JsonAlias("evidenciaIds") @NotEmpty List<@NotNull @Positive Long> evidenciasIds
) { }
