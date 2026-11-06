package br.com.bbts.sigespin.dto.gestao;

import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import java.time.LocalDateTime;

public record ProjetoConsultadoResponse(Long id, DadosProjetoRequest dados, Long etapaId,
        Integer trlAtual, OrigemDemanda origem, Long acaoMobilizacaoId, boolean arquivado,
        LocalDateTime arquivadoEm, Long arquivadoPorId, String motivoArquivamento,
        Long versao, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
    public static ProjetoConsultadoResponse de(Projeto projeto) {
        var dados = new DadosProjetoRequest(projeto.getTitulo(), projeto.getDescricaoDor(),
                projeto.getAreaDemandanteId(), projeto.getHipoteseSolucao(), projeto.getArena(),
                projeto.getPotencialLeiDoBem(), projeto.getDispendioEstimado(),
                projeto.getDemandante() == null ? null : projeto.getDemandante().getId(),
                projeto.getAnalistaResponsavel() == null ? null : projeto.getAnalistaResponsavel().getId());
        return new ProjetoConsultadoResponse(projeto.getId(), dados, projeto.getEtapa().getId(),
                projeto.getTrlAtual(), projeto.getOrigem(),
                projeto.getAcaoMobilizacao() == null ? null : projeto.getAcaoMobilizacao().getId(),
                projeto.getArquivado(), projeto.getArquivadoEm(),
                projeto.getArquivadoPor() == null ? null : projeto.getArquivadoPor().getId(),
                projeto.getMotivoArquivamento(), projeto.getVersao(), projeto.getCriadoEm(), projeto.getAtualizadoEm());
    }
}
