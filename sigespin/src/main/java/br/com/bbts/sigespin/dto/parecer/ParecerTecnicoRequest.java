package br.com.bbts.sigespin.dto.parecer;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ParecerTecnicoRequest(
        Long consultaId,

        @NotBlank(message = "A fundamentação do parecer técnico é obrigatória.")
        String fundamentacao,

        @NotBlank(message = "A conclusão do parecer técnico é obrigatória.")
        String conclusao,

        List<Long> evidenciasIds
) { }
