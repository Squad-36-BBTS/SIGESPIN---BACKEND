package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;

import java.time.LocalDateTime;

public record ProjetoResponse(
        Long id,
        String titulo,
        ArenaTipo arena,
        EtapaResumoResponse etapa,
        OrigemDemanda origem,
        Integer trlAtual,
        UsuarioResumoResponse responsavelPo,
        UsuarioResumoResponse demandante,
        UsuarioResumoResponse analistaResponsavel,
        LocalDateTime criadoEm
) {
    public static ProjetoResponse de(Projeto projeto) {
        if (projeto == null) {
            return null;
        }
        return new ProjetoResponse(
                projeto.getId(),
                projeto.getTitulo(),
                projeto.getArena(),
                EtapaResumoResponse.de(projeto.getEtapa()),
                projeto.getOrigem(),
                projeto.getTrlAtual(),
                UsuarioResumoResponse.de(projeto.getResponsavelPo()),
                UsuarioResumoResponse.de(projeto.getDemandante()),
                UsuarioResumoResponse.de(projeto.getAnalistaResponsavel()),
                projeto.getCriadoEm()
        );
    }
}
