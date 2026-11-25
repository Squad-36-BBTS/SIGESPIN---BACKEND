package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.EstadoTarefa;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;

/**
 * Filtros do portfólio, todos opcionais e combinados com "e".
 *
 * @param arquivado    true = só arquivados, false = só ativos, null = todos
 * @param periodoDias  projetos registrados nos últimos N dias (1 a 3650), como no painel
 */
public record FiltroPortfolio(
        ArenaTipo arena,
        Long etapaId,
        String busca,
        Boolean arquivado,
        OrigemDemanda origem,
        Long acaoMobilizacaoId,
        EstadoTarefa pendencia,
        Integer periodoDias
) {
    public static final int PERIODO_MAXIMO_DIAS = 3650;

    public static FiltroPortfolio basico(ArenaTipo arena, Long etapaId, String busca, Boolean arquivado) {
        return new FiltroPortfolio(arena, etapaId, busca, arquivado, null, null, null, null);
    }

    /** Valores do filtro de arquivamento do frontend. */
    public enum Arquivamento {
        ATIVOS, ARQUIVADOS, TODOS;

        public Boolean comoArquivado() {
            return switch (this) {
                case ATIVOS -> false;
                case ARQUIVADOS -> true;
                case TODOS -> null;
            };
        }
    }
}
