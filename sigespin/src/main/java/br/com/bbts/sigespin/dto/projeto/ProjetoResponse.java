package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.EstadoTarefa;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Projeto do portfólio no formato {@code Projeto} do frontend, com as pendências calculadas. */
public record ProjetoResponse(
        Long id,
        String titulo,
        String descricaoDor,
        OrigemDemanda origem,
        CampanhaResumoResponse campanha,
        Long areaDemandanteId,
        UsuarioResumoResponse demandante,
        UsuarioResumoResponse analistaResponsavel,
        UsuarioResumoResponse responsavelPo,
        ArenaTipo arena,
        String hipoteseSolucao,
        Boolean potencialLeiDoBem,
        BigDecimal dispendioEstimado,
        EtapaResumoResponse etapa,
        Integer trlAtual,
        List<EstadoTarefa> pendencias,
        boolean arquivado,
        String motivoArquivamento,
        LocalDateTime arquivadoEm,
        Long versao,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static ProjetoResponse de(Projeto projeto) {
        return de(projeto, List.of());
    }

    public static ProjetoResponse de(Projeto projeto, List<EstadoTarefa> pendencias) {
        if (projeto == null) {
            return null;
        }
        return new ProjetoResponse(
                projeto.getId(),
                projeto.getTitulo(),
                projeto.getDescricaoDor(),
                projeto.getOrigem(),
                CampanhaResumoResponse.de(projeto.getAcaoMobilizacao()),
                projeto.getAreaDemandanteId(),
                UsuarioResumoResponse.de(projeto.getDemandante()),
                UsuarioResumoResponse.de(projeto.getAnalistaResponsavel()),
                UsuarioResumoResponse.de(projeto.getResponsavelPo()),
                projeto.getArena(),
                projeto.getHipoteseSolucao(),
                projeto.getPotencialLeiDoBem(),
                projeto.getDispendioEstimado(),
                EtapaResumoResponse.de(projeto.getEtapa()),
                projeto.getTrlAtual(),
                pendencias == null ? List.of() : pendencias,
                Boolean.TRUE.equals(projeto.getArquivado()),
                projeto.getMotivoArquivamento(),
                projeto.getArquivadoEm(),
                projeto.getVersao(),
                projeto.getCriadoEm(),
                projeto.getAtualizadoEm()
        );
    }
}
