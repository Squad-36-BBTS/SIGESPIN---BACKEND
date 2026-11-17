package br.com.bbts.sigespin.dto.parecer;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

import java.util.List;

public record ParecerTecnicoRequest(
        @Positive Long consultaId,

        @NotBlank(message = "A fundamentação do parecer técnico é obrigatória.")
        String fundamentacao,

        @NotBlank(message = "A conclusão do parecer técnico é obrigatória.")
        String conclusao,

        @JsonAlias("evidenciaIds") List<@NotNull @Positive Long> evidenciasIds
) {
    public ParecerTecnicoRequest {
        evidenciasIds = evidenciasIds == null ? List.of() : List.copyOf(evidenciasIds);
    }
}
