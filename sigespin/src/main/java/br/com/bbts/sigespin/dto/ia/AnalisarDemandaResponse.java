package br.com.bbts.sigespin.dto.ia;

import br.com.bbts.sigespin.model.enums.ArenaTipo;

import java.math.BigDecimal;

/** Sugestão da IA. É só uma sugestão: o usuário revisa e confirma antes de salvar (human-in-the-loop). */
public record AnalisarDemandaResponse(
        ArenaTipo arenaSugerida,
        String hipotese,
        int trlInicial,
        boolean potencialLeiDoBem,
        String justificativaArena,
        ScoresRadar scoresRadar
) {
    public record ScoresRadar(BigDecimal negocio, BigDecimal produto, BigDecimal time, BigDecimal receita) {
    }
}
