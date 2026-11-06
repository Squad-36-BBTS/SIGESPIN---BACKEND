package br.com.bbts.sigespin.dto.kanban;

import br.com.bbts.sigespin.model.enums.TipoTerminal;
import jakarta.validation.constraints.*;

public record CriarColunaRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{0,254}") String codigo,
        @NotBlank @Size(max = 255) String nome,
        @NotNull @Min(0) Integer ordem,
        TipoTerminal terminal) {
}
