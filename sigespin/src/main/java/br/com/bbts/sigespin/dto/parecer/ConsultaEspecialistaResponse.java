package br.com.bbts.sigespin.dto.parecer;

import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.entity.ConsultaEspecialista;

import java.time.LocalDateTime;

public record ConsultaEspecialistaResponse(
        Long id,
        Long projetoId,
        UsuarioResumoResponse solicitante,
        UsuarioResumoResponse especialista,
        String questao,
        LocalDateTime criadoEm
) {
    public static ConsultaEspecialistaResponse de(ConsultaEspecialista consulta) {
        if (consulta == null) {
            return null;
        }
        return new ConsultaEspecialistaResponse(
                consulta.getId(),
                consulta.getProjeto() != null ? consulta.getProjeto().getId() : null,
                UsuarioResumoResponse.de(consulta.getSolicitante()),
                UsuarioResumoResponse.de(consulta.getEspecialista()),
                consulta.getQuestao(),
                consulta.getCriadoEm()
        );
    }
}
