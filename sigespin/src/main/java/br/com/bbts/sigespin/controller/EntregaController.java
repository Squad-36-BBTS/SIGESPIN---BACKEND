package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.entrega.EntregaProjetoRequest;
import br.com.bbts.sigespin.dto.entrega.EntregaProjetoResponse;
import br.com.bbts.sigespin.dto.entrega.ValidacaoEntregaRequest;
import br.com.bbts.sigespin.dto.entrega.ValidacaoEntregaResponse;
import br.com.bbts.sigespin.exception.ServicoIndisponivelException;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import br.com.bbts.sigespin.service.EntregaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    public EntregaController(@Autowired(required = false) EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    private EntregaService obterServico() {
        if (this.entregaService == null) {
            throw new ServicoIndisponivelException("O serviço de entregas e validações da Fase 3 ainda não foi integrado.");
        }
        return this.entregaService;
    }

    @GetMapping("/entregas")
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<EntregaProjetoResponse> listarEntregas(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(obterServico().listarEntregasPorProjeto(projetoId, seguro));
    }

    @PostMapping("/entregas")
    @PreAuthorize("hasAnyRole('ANALISTA', 'DEMANDANTE')")
    public ResponseEntity<EntregaProjetoResponse> submeterEntrega(
            @PathVariable("projetoId") Long projetoId,
            @Valid @RequestBody EntregaProjetoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long autorId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        EntregaProjetoResponse criada = obterServico().submeterEntrega(projetoId, request, autorId);

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
        return PaginaResponse.de(obterServico().listarValidacoesPorProjeto(projetoId, seguro));
    }

    @PostMapping("/entregas/{entregaId}/validacoes")
    @PreAuthorize("hasRole('DEMANDANTE')")
    public ResponseEntity<ValidacaoEntregaResponse> registrarValidacao(
            @PathVariable("projetoId") Long projetoId,
            @PathVariable("entregaId") Long entregaId,
            @Valid @RequestBody ValidacaoEntregaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long demandanteId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        ValidacaoEntregaResponse validacao = obterServico().registrarValidacao(projetoId, entregaId, request, demandanteId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(validacao.id())
                .toUri();

        return ResponseEntity.created(location).body(validacao);
    }
}
