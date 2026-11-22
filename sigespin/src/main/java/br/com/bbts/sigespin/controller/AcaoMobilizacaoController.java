package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.mobilizacao.AcaoMobilizacaoResponse;
import br.com.bbts.sigespin.service.AcaoMobilizacaoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * BE-09 — GET /api/acoes-mobilizacao
 * Rota para consulta das ações de mobilização.
 */
@RestController
@RequestMapping("/api/acoes-mobilizacao")
public class AcaoMobilizacaoController {

    private final AcaoMobilizacaoService acaoMobilizacaoService;

    public AcaoMobilizacaoController(AcaoMobilizacaoService acaoMobilizacaoService) {
        this.acaoMobilizacaoService = acaoMobilizacaoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AcaoMobilizacaoResponse> listar() {
        return acaoMobilizacaoService.listar();
    }
}
