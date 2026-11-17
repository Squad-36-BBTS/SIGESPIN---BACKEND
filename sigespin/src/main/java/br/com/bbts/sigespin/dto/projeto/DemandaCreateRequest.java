package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@OrigemDemandaValida
public record DemandaCreateRequest(
        @NotBlank(message = "O título da demanda é obrigatório.")
        @Size(max = 255, message = "O título deve possuir no máximo 255 caracteres.")
        String titulo,

        @NotBlank(message = "A descrição da dor/problema é obrigatória.")
        String descricaoDor,

        @NotNull(message = "O identificador da área demandante é obrigatório.")
        @Positive Long areaDemandanteId,

        ArenaTipo arena,

        @NotNull(message = "A origem da demanda é obrigatória.")
        OrigemDemanda origem,

        @Positive Long acaoMobilizacaoId,

        String hipoteseSolucao,

        Boolean potencialLeiDoBem,

        @PositiveOrZero(message = "O dispêndio estimado não pode ser negativo.")
        BigDecimal dispendioEstimado,

        @NotNull @Min(1) @Max(9) Integer trlInicialDeclarado
) { }
