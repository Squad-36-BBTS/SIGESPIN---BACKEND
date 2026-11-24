package br.com.bbts.sigespin.dto.dashboard;

import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.EtapaFila;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.EstadoTarefa;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;

import java.util.List;
import java.util.Map;

/**
 * Métricas do painel (BE-13), no mesmo formato do tipo {@code MetricasDashboard} do frontend.
 *
 * <p>Os indicadores consideram o portfólio <b>ativo</b> do recorte filtrado; os arquivados aparecem só em
 * {@code arquivados}. Todas as chaves de origem, arena e pendência vêm sempre preenchidas (com zero, se for o
 * caso), e {@code porEtapa} traz todas as etapas ativas na ordem do Kanban.</p>
 */
public record MetricasDashboardResponse(
        long totalProjetos,
        long arquivados,
        double trlMedio,
        List<ContagemEtapa> porEtapa,
        Map<OrigemDemanda, Long> porOrigem,
        Map<ArenaTipo, Long> porArena,
        Map<EstadoTarefa, Long> pendencias,
        ScoresRadar radarMedioInicial,
        ScoresRadar radarMedioAtual) {

    public record ContagemEtapa(EtapaFila etapa, long total) { }

    /** Médias de 0,00 a 4,00. Sem leituras no recorte, todas as médias são zero. */
    public record ScoresRadar(double negocio, double produto, double time, double receita) { }
}
