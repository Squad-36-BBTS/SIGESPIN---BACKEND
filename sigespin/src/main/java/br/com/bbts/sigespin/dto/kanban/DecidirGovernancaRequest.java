package br.com.bbts.sigespin.dto.kanban;

import jakarta.validation.constraints.*;

public record DecidirGovernancaRequest(@NotNull Decisao decisao, @NotBlank String justificativa) {
    public enum Decisao { APROVAR, REJEITAR }
}
