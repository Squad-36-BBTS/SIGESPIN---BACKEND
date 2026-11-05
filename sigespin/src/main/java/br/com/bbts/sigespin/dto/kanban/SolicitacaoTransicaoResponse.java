package br.com.bbts.sigespin.dto.kanban;

import br.com.bbts.sigespin.dto.projeto.EtapaResumoResponse;
import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.entity.SolicitacaoTransicao;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;

import java.time.LocalDateTime;

public record SolicitacaoTransicaoResponse(
        Long id,
        Long projetoId,
        UsuarioResumoResponse solicitante,
        Long transicaoId,
        EtapaResumoResponse origem,
        EtapaResumoResponse destino,
        Boolean exigeGovernanca,
        StatusSolicitacao status,
        String justificativa,
        LocalDateTime criadoEm,
        UsuarioResumoResponse responsavelEncerramento,
        String justificativaEncerramento,
        LocalDateTime encerradoEm,
        Long versao
) {
    public static SolicitacaoTransicaoResponse de(SolicitacaoTransicao solicitacao) {
        if (solicitacao == null) {
            return null;
        }
        return new SolicitacaoTransicaoResponse(
                solicitacao.getId(),
                solicitacao.getProjeto() != null ? solicitacao.getProjeto().getId() : null,
                UsuarioResumoResponse.de(solicitacao.getSolicitante()),
                solicitacao.getTransicao() != null ? solicitacao.getTransicao().getId() : null,
                EtapaResumoResponse.de(solicitacao.getOrigem()),
                EtapaResumoResponse.de(solicitacao.getDestino()),
                solicitacao.getExigeGovernanca(),
                solicitacao.getStatus(),
                solicitacao.getJustificativa(),
                solicitacao.getCriadoEm(),
                UsuarioResumoResponse.de(solicitacao.getResponsavelEncerramento()),
                solicitacao.getJustificativaEncerramento(),
                solicitacao.getEncerradoEm(),
                solicitacao.getVersao()
        );
    }
}
