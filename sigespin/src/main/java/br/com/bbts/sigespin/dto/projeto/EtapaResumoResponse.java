package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.entity.EtapaKanban;
import br.com.bbts.sigespin.model.enums.TipoTerminal;

public record EtapaResumoResponse(
        Long id,
        String codigo,
        String nome,
        Integer ordem,
        TipoTerminal terminal
) {
    public static EtapaResumoResponse de(EtapaKanban etapa) {
        if (etapa == null) {
            return null;
        }
        return new EtapaResumoResponse(
                etapa.getId(),
                etapa.getCodigo(),
                etapa.getNome(),
                etapa.getOrdem(),
                etapa.getTerminal()
        );
    }
}
