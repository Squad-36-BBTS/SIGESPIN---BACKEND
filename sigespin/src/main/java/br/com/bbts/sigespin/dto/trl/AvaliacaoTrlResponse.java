package br.com.bbts.sigespin.dto.trl;

import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.entity.AvaliacaoTrl;

import java.time.LocalDateTime;

public record AvaliacaoTrlResponse(
        Long id,
        Long projetoId,
        UsuarioResumoResponse autor,
        Integer nivelAnterior,
        Integer nivel,
        String justificativa,
        LocalDateTime avaliadoEm,
        Boolean evidenciasFechadas
) {
    public static AvaliacaoTrlResponse de(AvaliacaoTrl avaliacao) {
        if (avaliacao == null) {
            return null;
        }
        return new AvaliacaoTrlResponse(
                avaliacao.getId(),
                avaliacao.getProjeto() != null ? avaliacao.getProjeto().getId() : null,
                UsuarioResumoResponse.de(avaliacao.getAutor()),
                avaliacao.getNivelAnterior(),
                avaliacao.getNivel(),
                avaliacao.getJustificativa(),
                avaliacao.getAvaliadoEm(),
                avaliacao.getEvidenciasFechadas()
        );
    }
}
