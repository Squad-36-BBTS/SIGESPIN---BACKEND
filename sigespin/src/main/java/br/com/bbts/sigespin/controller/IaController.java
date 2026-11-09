package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.ia.AnalisarDemandaRequest;
import br.com.bbts.sigespin.dto.ia.AnalisarDemandaResponse;
import br.com.bbts.sigespin.service.GeminiIntegrationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia")
public class IaController {

    private final GeminiIntegrationService gemini;

    public IaController(GeminiIntegrationService gemini) {
        this.gemini = gemini;
    }

    @PostMapping("/analisar-demanda")
    @PreAuthorize("hasAnyRole('DEMANDANTE','ANALISTA')")
    public AnalisarDemandaResponse analisarDemanda(@Valid @RequestBody AnalisarDemandaRequest request) {
        return gemini.analisar(request);
    }
}
