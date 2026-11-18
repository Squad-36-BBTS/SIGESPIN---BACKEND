package br.com.bbts.sigespin.service;

import java.util.List;
import java.util.Map;

/** O provedor recebe o contrato e o parser local ainda valida a resposta recebida. */
final class GeminiContrato {
    private GeminiContrato() { }
    static Map<String, Object> esquema() {
        var texto = Map.of("type", "string", "minLength", 1, "maxLength", 1000);
        var nota = Map.of("type", "number", "minimum", 0, "maximum", 4);
        return Map.of("type", "object", "additionalProperties", false,
                "required", List.of("arenaSugerida", "hipotese", "trlInicial", "potencialLeiDoBem", "justificativaArena", "scoresRadar"),
                "properties", Map.of(
                        "arenaSugerida", Map.of("type", "string", "enum", List.of("EFICIENCIA_OPERACIONAL", "NEGOCIOS_DIGITAIS", "SEGURANCA_INTEGRADA")),
                        "hipotese", texto,
                        "trlInicial", Map.of("type", "integer", "minimum", 1, "maximum", 3),
                        "potencialLeiDoBem", Map.of("type", "boolean"),
                        "justificativaArena", texto,
                        "scoresRadar", Map.of("type", "object", "additionalProperties", false,
                                "required", List.of("negocio", "produto", "time", "receita"),
                                "properties", Map.of("negocio", nota, "produto", nota, "time", nota, "receita", nota))));
    }
}
