package br.com.bbts.sigespin.dto.demanda;

import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import jakarta.validation.constraints.*;

/** Dados de entrada do caso de uso; autoria e etapa são definidas pelo servidor. */
public record RegistrarDemandaRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotBlank String descricaoDor,
        @NotNull @Positive Long areaDemandanteId,
        @NotNull OrigemDemanda origem,
        @Positive Long acaoMobilizacaoId,
        @NotNull @Min(1) @Max(9) Integer trlInicialDeclarado,
        String hipoteseSolucao) {
}
