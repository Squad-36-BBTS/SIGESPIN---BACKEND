package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.ia.AnalisarDemandaResponse;
import br.com.bbts.sigespin.dto.ia.AnalisarDemandaResponse.ScoresRadar;
import br.com.bbts.sigespin.exception.IaIndisponivelException;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Parser defensivo: a saída da IA nunca é confiável. Remove cercas markdown, valida e normaliza
 * cada campo (arena inválida -> A_DEFINIR, TRL fora de 1..3 -> limitado, scores fora de 0..4 -> limitados).
 */
final class GeminiRespostaParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int LIMITE_TEXTO = 1000;

    private GeminiRespostaParser() {
    }

    static AnalisarDemandaResponse parse(String textoBruto) {
        if (textoBruto == null || textoBruto.isBlank()) {
            throw new IaIndisponivelException("A IA retornou uma resposta vazia.");
        }
        JsonNode raiz;
        try {
            raiz = MAPPER.readTree(removerCercaMarkdown(textoBruto));
        } catch (Exception e) {
            throw new IaIndisponivelException("A IA retornou um JSON inválido.", e);
        }
        if (raiz == null || !raiz.isObject()) {
            throw new IaIndisponivelException("A IA retornou um formato inesperado.");
        }
        JsonNode radar = raiz.path("scoresRadar");
        return new AnalisarDemandaResponse(
                arena(raiz.path("arenaSugerida").asText(null)),
                texto(raiz.path("hipotese")),
                Math.max(1, Math.min(3, raiz.path("trlInicial").asInt(1))),
                raiz.path("potencialLeiDoBem").asBoolean(false),
                texto(raiz.path("justificativaArena")),
                new ScoresRadar(
                        score(radar.path("negocio")),
                        score(radar.path("produto")),
                        score(radar.path("time")),
                        score(radar.path("receita")))
        );
    }

    static String removerCercaMarkdown(String s) {
        String t = s.strip();
        if (t.startsWith("```")) {
            int primeiraQuebra = t.indexOf('\n');
            t = primeiraQuebra >= 0 ? t.substring(primeiraQuebra + 1) : t.substring(3);
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.strip();
    }

    private static ArenaTipo arena(String valor) {
        if (valor == null) {
            return ArenaTipo.A_DEFINIR;
        }
        try {
            return ArenaTipo.valueOf(valor.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ArenaTipo.A_DEFINIR;
        }
    }

    private static String texto(JsonNode no) {
        String t = no.isMissingNode() || no.isNull() ? "" : no.asText("").strip();
        return t.length() > LIMITE_TEXTO ? t.substring(0, LIMITE_TEXTO) : t;
    }

    private static BigDecimal score(JsonNode no) {
        BigDecimal v = no.isNumber() ? no.decimalValue() : BigDecimal.ZERO;
        v = v.max(BigDecimal.ZERO).min(new BigDecimal("4"));
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
