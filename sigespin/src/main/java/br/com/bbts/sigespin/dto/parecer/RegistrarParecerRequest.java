package br.com.bbts.sigespin.dto.parecer;

import jakarta.validation.constraints.*;
import java.util.List;

/** Consulta é opcional; parecer textual usa lista de evidências vazia, nunca nula. */
public record RegistrarParecerRequest(
        @Positive Long consultaId,
        @NotBlank String fundamentacao,
        @NotBlank String conclusao,
        @NotNull List<@NotNull @Positive Long> evidenciaIds) {
}
