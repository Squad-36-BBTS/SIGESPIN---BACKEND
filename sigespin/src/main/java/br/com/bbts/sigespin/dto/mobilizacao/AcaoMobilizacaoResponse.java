package br.com.bbts.sigespin.dto.mobilizacao;

import br.com.bbts.sigespin.model.entity.AcaoMobilizacao;

/**
 * DTO de resposta para Ação de Mobilização.
 */
public record AcaoMobilizacaoResponse(
        Long id,
        String nome,
        String descricao
) {
    public static AcaoMobilizacaoResponse de(AcaoMobilizacao a) {
        return new AcaoMobilizacaoResponse(a.getId(), a.getNome(), a.getDescricao());
    }
}
