package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.dashboard.MetricasDashboardResponse;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import br.com.bbts.sigespin.service.MetricasDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Painel (BE-13). Filtros vazios (como o frontend envia) valem como "todos". */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final MetricasDashboardService metricas;

    public DashboardController(MetricasDashboardService metricas) {
        this.metricas = metricas;
    }

    @GetMapping("/metricas")
    @PreAuthorize("isAuthenticated()")
    public MetricasDashboardResponse metricas(
            @RequestParam(required = false) Integer periodoDias,
            @RequestParam(required = false) Long etapaId,
            @RequestParam(required = false) OrigemDemanda origem) {
        return metricas.calcular(periodoDias, etapaId, origem);
    }
}
