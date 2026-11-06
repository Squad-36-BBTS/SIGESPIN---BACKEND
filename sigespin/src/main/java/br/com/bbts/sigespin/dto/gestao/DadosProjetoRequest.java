package br.com.bbts.sigespin.dto.gestao;

import br.com.bbts.sigespin.model.enums.ArenaTipo;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Edição completa dos dados cadastrais e responsáveis. */
public record DadosProjetoRequest(@NotBlank @Size(max = 255) String titulo,
        @NotBlank String descricaoDor, @NotNull @Positive Long areaDemandanteId,
        String hipoteseSolucao, @NotNull ArenaTipo arena, Boolean potencialLeiDoBem,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal dispendioEstimado,
        @NotNull @Positive Long demandanteId, @Positive Long analistaResponsavelId) {
}
