package br.com.bbts.sigespin.dto.kanban;

import br.com.bbts.sigespin.model.enums.TipoTerminal;

public record ColunaConfiguradaResponse(Long id, String codigo, String nome,
        Integer ordem, boolean ativa, TipoTerminal terminal, Long versao) {
}
