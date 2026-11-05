package br.com.bbts.sigespin.dto.kanban;

import br.com.bbts.sigespin.dto.projeto.EtapaResumoResponse;
import br.com.bbts.sigespin.model.entity.TransicaoKanban;

public record TransicaoKanbanResponse(
        Long id,
        EtapaResumoResponse origem,
        EtapaResumoResponse destino,
        Boolean exigeGovernanca,
        Boolean ativa,
        Long versao
) {
    public static TransicaoKanbanResponse de(TransicaoKanban transicao) {
        if (transicao == null) {
            return null;
        }
        return new TransicaoKanbanResponse(
                transicao.getId(),
                EtapaResumoResponse.de(transicao.getOrigem()),
                EtapaResumoResponse.de(transicao.getDestino()),
                transicao.getExigeGovernanca(),
                transicao.getAtiva(),
                transicao.getVersao()
        );
    }
}
