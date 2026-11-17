package br.com.bbts.sigespin.dto.kanban;

public record PercursoConfiguradoResponse(Long id, Long origemId, Long destinoId,
        boolean exigeGovernanca, boolean ativa, Long versao) {
}
