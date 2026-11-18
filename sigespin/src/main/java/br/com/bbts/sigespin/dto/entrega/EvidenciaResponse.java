package br.com.bbts.sigespin.dto.entrega;

import br.com.bbts.sigespin.model.entity.Evidencia;
import java.time.LocalDateTime;

public record EvidenciaResponse(Long id, Long projetoId, String nomeArquivo, String tipoArquivo,
        Long tamanhoBytes, String sha256, Long enviadaPorId, LocalDateTime dataUpload, String downloadUrl) {
    public static EvidenciaResponse de(Evidencia evidencia) {
        return new EvidenciaResponse(evidencia.getId(), evidencia.getProjeto().getId(), evidencia.getNomeArquivo(),
                evidencia.getTipoArquivo(), evidencia.getTamanhoBytes(), evidencia.getSha256(),
                evidencia.getEnviadaPor() == null ? null : evidencia.getEnviadaPor().getId(), evidencia.getDataUpload(),
                "/api/projetos/" + evidencia.getProjeto().getId() + "/evidencias/" + evidencia.getId() + "/conteudo");
    }
}
