package br.com.bbts.sigespin.dto.kanban;

import br.com.bbts.sigespin.model.entity.EtapaKanban;
import br.com.bbts.sigespin.model.enums.TipoTerminal;

public record EtapaKanbanResponse(
        Long id,
        String codigo,
        String nome,
        Integer ordem,
        Boolean ativa,
        TipoTerminal terminal,
        Long versao
) {
    public static EtapaKanbanResponse de(EtapaKanban etapa) {
        if (etapa == null) {
            return null;
        }
        return new EtapaKanbanResponse(
                etapa.getId(),
                etapa.getCodigo(),
                etapa.getNome(),
                etapa.getOrdem(),
                etapa.getAtiva(),
                etapa.getTerminal(),
                etapa.getVersao()
        );
    }
}
