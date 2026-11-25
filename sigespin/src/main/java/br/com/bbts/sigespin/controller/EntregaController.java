package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.entrega.EntregaProjetoRequest;
import br.com.bbts.sigespin.dto.entrega.EntregaProjetoResponse;
import br.com.bbts.sigespin.dto.entrega.ValidacaoEntregaRequest;
import br.com.bbts.sigespin.dto.entrega.ValidacaoEntregaResponse;
import br.com.bbts.sigespin.service.EntregaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/projetos/{projetoId}")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @GetMapping("/entregas")
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<EntregaProjetoResponse> listarEntregas(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(entregaService.listarEntregasPorProjeto(projetoId, seguro));
    }

    @PostMapping("/entregas")
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public ResponseEntity<EntregaProjetoResponse> submeterEntrega(
            @PathVariable("projetoId") Long projetoId,
            @Valid @RequestBody EntregaProjetoRequest request) {
        EntregaProjetoResponse criada = entregaService.submeterEntrega(projetoId, request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();

        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping("/validacoes")
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<ValidacaoEntregaResponse> listarValidacoes(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(entregaService.listarValidacoesPorProjeto(projetoId, seguro));
    }

    @PostMapping("/entregas/{entregaId}/validacoes")
    @PreAuthorize("hasRole('DEMANDANTE')")
    public ResponseEntity<ValidacaoEntregaResponse> registrarValidacao(
            @PathVariable("projetoId") Long projetoId,
            @PathVariable("entregaId") Long entregaId,
            @Valid @RequestBody ValidacaoEntregaRequest request) {
        ValidacaoEntregaResponse validacao = entregaService.registrarValidacao(projetoId, entregaId, request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(validacao.id())
                .toUri();

        return ResponseEntity.created(location).body(validacao);
    }
}
