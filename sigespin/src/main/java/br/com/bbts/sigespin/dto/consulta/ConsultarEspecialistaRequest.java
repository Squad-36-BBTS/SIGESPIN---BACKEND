package br.com.bbts.sigespin.dto.consulta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** O solicitante vem da autenticação; o formulário escolhe somente o destinatário. */
public record ConsultarEspecialistaRequest(
        @NotNull @Positive Long especialistaId,
        @NotBlank String questao) {
}
