package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProjetoDetalheResponse(
        Long id,
        String titulo,
        String descricaoDor,
        Long areaDemandanteId,
        ArenaTipo arena,
        EtapaResumoResponse etapa,
        OrigemDemanda origem,
        Long acaoMobilizacaoId,
        Integer trlAtual,
        String hipoteseSolucao,
        Boolean potencialLeiDoBem,
        BigDecimal dispendioEstimado,
        UsuarioResumoResponse responsavelPo,
        UsuarioResumoResponse demandante,
        UsuarioResumoResponse analistaResponsavel,
        Long versao,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static ProjetoDetalheResponse de(Projeto projeto) {
        if (projeto == null) {
            return null;
        }
        return new ProjetoDetalheResponse(
                projeto.getId(),
                projeto.getTitulo(),
                projeto.getDescricaoDor(),
                projeto.getAreaDemandanteId(),
                projeto.getArena(),
                EtapaResumoResponse.de(projeto.getEtapa()),
                projeto.getOrigem(),
                projeto.getAcaoMobilizacao() != null ? projeto.getAcaoMobilizacao().getId() : null,
                projeto.getTrlAtual(),
                projeto.getHipoteseSolucao(),
                projeto.getPotencialLeiDoBem(),
                projeto.getDispendioEstimado(),
                UsuarioResumoResponse.de(projeto.getResponsavelPo()),
                UsuarioResumoResponse.de(projeto.getDemandante()),
                UsuarioResumoResponse.de(projeto.getAnalistaResponsavel()),
                projeto.getVersao(),
                projeto.getCriadoEm(),
                projeto.getAtualizadoEm()
        );
    }
}
