package br.com.bbts.sigespin.dto.parecer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConsultaEspecialistaRequest(
        @NotNull(message = "O especialista destinatário é obrigatório.")
        Long especialistaId,

        @NotBlank(message = "A questão da consulta é obrigatória.")
        String questao
) { }
