package br.com.bbts.sigespin.dto.interacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Só o texto: autor e data são definidos pelo servidor. */
public record RegistrarInteracaoRequest(
        @NotBlank @Size(max = RegistrarInteracaoRequest.TAMANHO_MAXIMO_TEXTO) String texto) {
    public static final int TAMANHO_MAXIMO_TEXTO = 2000;
}
