package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.entity.EtapaKanban;
import br.com.bbts.sigespin.model.enums.TipoTerminal;

import java.util.Map;

/**
 * Etapa no formato {@code EtapaResumo} do frontend: {@code terminal} é booleano e o tipo de
 * encerramento vai em {@code tipoTerminal}. A cor é derivada do código, a mesma em todas as telas.
 */
public record EtapaResumoResponse(
        Long id,
        String codigo,
        String nome,
        Integer ordem,
        String cor,
        boolean terminal,
        TipoTerminal tipoTerminal
) {
    public static final String COR_NEUTRA = "#64748B";
    private static final Map<String, String> CORES = Map.of(
            "IDEACAO", "#F59E0B",
            "POC", "#0EA5E9",
            "MVP", "#6366F1",
            "CONCLUIDO", "#10B981",
            "DESCONTINUADO", "#F43F5E");

    public EtapaResumoResponse(Long id, String codigo, String nome, Integer ordem, TipoTerminal tipoTerminal) {
        this(id, codigo, nome, ordem, cor(codigo), tipoTerminal != null, tipoTerminal);
    }

    public static String cor(String codigo) {
        return codigo == null ? COR_NEUTRA : CORES.getOrDefault(codigo, COR_NEUTRA);
    }

    public static EtapaResumoResponse de(EtapaKanban etapa) {
        if (etapa == null) {
            return null;
        }
        return new EtapaResumoResponse(etapa.getId(), etapa.getCodigo(), etapa.getNome(), etapa.getOrdem(),
                etapa.getTerminal());
    }
}
