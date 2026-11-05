package br.com.bbts.sigespin.dto.trl;

import jakarta.validation.constraints.*;
import java.util.List;

/** A lista contém IDs de arquivos já cadastrados; o upload é um caso de uso separado. */
public record RegistrarAvaliacaoTrlRequest(
        @NotNull @Min(1) @Max(9) Integer nivel,
        @NotBlank String justificativa,
        @NotEmpty List<@NotNull @Positive Long> evidenciaIds) {
}
