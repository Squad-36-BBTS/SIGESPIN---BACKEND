package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.entity.AcaoMobilizacao;

/** Ação de mobilização no formato {@code CampanhaResumo} do frontend. O banco não guarda o tipo: vai como OUTRA. */
public record CampanhaResumoResponse(Long id, String titulo, String tipo) {
    public static CampanhaResumoResponse de(AcaoMobilizacao acao) {
        return acao == null ? null : new CampanhaResumoResponse(acao.getId(), acao.getNome(), "OUTRA");
    }
}
