package br.com.bbts.sigespin.dto.parecer;

import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.entity.ParecerTecnico;

import java.time.LocalDateTime;

public record ParecerTecnicoResponse(
        Long id,
        Long projetoId,
        UsuarioResumoResponse autor,
        Long consultaId,
        String fundamentacao,
        String conclusao,
        Boolean evidenciasFechadas,
        LocalDateTime criadoEm
) {
    public static ParecerTecnicoResponse de(ParecerTecnico parecer) {
        if (parecer == null) {
            return null;
        }
        return new ParecerTecnicoResponse(
                parecer.getId(),
                parecer.getProjeto() != null ? parecer.getProjeto().getId() : null,
                UsuarioResumoResponse.de(parecer.getAutor()),
                parecer.getConsulta() != null ? parecer.getConsulta().getId() : null,
                parecer.getFundamentacao(),
                parecer.getConclusao(),
                parecer.getEvidenciasFechadas(),
                parecer.getCriadoEm()
        );
    }
}
