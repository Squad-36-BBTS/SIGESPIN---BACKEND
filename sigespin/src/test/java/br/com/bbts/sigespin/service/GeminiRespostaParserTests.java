package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.ia.AnalisarDemandaResponse;
import br.com.bbts.sigespin.exception.IaIndisponivelException;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiRespostaParserTests {

    private static final String JSON_OK = """
            {"arenaSugerida":"EFICIENCIA_OPERACIONAL","hipotese":"Se X então Y","trlInicial":2,
             "potencialLeiDoBem":true,"justificativaArena":"Automação",
             "scoresRadar":{"negocio":2.5,"produto":1.8,"time":3.0,"receita":1.2}}
            """;

    @Test
    void parseiaJsonValido() {
        AnalisarDemandaResponse r = GeminiRespostaParser.parse(JSON_OK);
        assertEquals(ArenaTipo.EFICIENCIA_OPERACIONAL, r.arenaSugerida());
        assertEquals(2, r.trlInicial());
        assertEquals(new BigDecimal("2.50"), r.scoresRadar().negocio());
    }

    @Test
    void removeCercaMarkdown() {
        AnalisarDemandaResponse r = GeminiRespostaParser.parse("```json\n" + JSON_OK + "\n```");
        assertEquals("Se X então Y", r.hipotese());
    }

    @Test
    void arenaInvalidaViraADefinir() {
        AnalisarDemandaResponse r = GeminiRespostaParser.parse(JSON_OK.replace("EFICIENCIA_OPERACIONAL", "INVENTADA"));
        assertEquals(ArenaTipo.A_DEFINIR, r.arenaSugerida());
    }

    @Test
    void limitaTrlEScores() {
        String json = JSON_OK.replace("\"trlInicial\":2", "\"trlInicial\":9")
                .replace("\"negocio\":2.5", "\"negocio\":7").replace("\"time\":3.0", "\"time\":-1");
        AnalisarDemandaResponse r = GeminiRespostaParser.parse(json);
        assertEquals(3, r.trlInicial());
        assertEquals(new BigDecimal("4.00"), r.scoresRadar().negocio());
        assertEquals(new BigDecimal("0.00"), r.scoresRadar().time());
    }

    @Test
    void rejeitaVazioEInvalido() {
        assertThrows(IaIndisponivelException.class, () -> GeminiRespostaParser.parse(""));
        assertThrows(IaIndisponivelException.class, () -> GeminiRespostaParser.parse("não é json"));
        assertThrows(IaIndisponivelException.class, () -> GeminiRespostaParser.parse("[1,2]"));
    }
}
