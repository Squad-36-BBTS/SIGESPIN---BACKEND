package br.com.bbts.sigespin.model.enums;

/**
 * Define as fases do funil de inovação (Stage-Gates).

 * O fluxo normal de um projeto segue a sequência:
 *   IDEACAO → POC → MVP → CONCLUIDO
 * Um projeto pode ser DESCONTINUADO a qualquer momento via deliberação.
 * Regras importantes:
 * - Transições de status só podem avançar para o estágio ADJACENTE (ex: IDEACAO → POC).
 * - Para ir a CONCLUIDO ou DESCONTINUADO é obrigatória uma deliberação do COMITE.
 */
public enum StatusFunil {
    IDEACAO,
    POC,
    MVP,
    CONCLUIDO,
    DESCONTINUADO
}
