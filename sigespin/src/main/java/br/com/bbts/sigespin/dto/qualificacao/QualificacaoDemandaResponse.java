package br.com.bbts.sigespin.dto.qualificacao;

import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.entity.QualificacaoDemanda;

import java.time.LocalDateTime;

public record QualificacaoDemandaResponse(
        Long id,
        Long projetoId,
        UsuarioResumoResponse autor,
        String aderenciaTese,
        String relevancia,
        String grauInovacao,
        String viabilidade,
        String maturidadeTecnologica,
        String fundamentacao,
        String conclusao,
        LocalDateTime criadoEm
) {
    public static QualificacaoDemandaResponse de(QualificacaoDemanda qualificacao) {
        if (qualificacao == null) {
            return null;
        }
        return new QualificacaoDemandaResponse(
                qualificacao.getId(),
                qualificacao.getProjeto() != null ? qualificacao.getProjeto().getId() : null,
                UsuarioResumoResponse.de(qualificacao.getAutor()),
                qualificacao.getAderenciaTese(),
                qualificacao.getRelevancia(),
                qualificacao.getGrauInovacao(),
                qualificacao.getViabilidade(),
                qualificacao.getMaturidadeTecnologica(),
                qualificacao.getFundamentacao(),
                qualificacao.getConclusao(),
                qualificacao.getCriadoEm()
        );
    }
}
