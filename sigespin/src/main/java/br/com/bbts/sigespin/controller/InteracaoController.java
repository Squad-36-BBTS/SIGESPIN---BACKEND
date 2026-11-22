package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.interacao.InteracaoProjetoResponse;
import br.com.bbts.sigespin.dto.interacao.RegistrarInteracaoRequest;
import br.com.bbts.sigespin.service.InteracaoProjetoService;
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
@RequestMapping("/api/projetos/{projetoId}/interacoes")
public class InteracaoController {

    private final InteracaoProjetoService interacaoService;

    public InteracaoController(InteracaoProjetoService interacaoService) {
        this.interacaoService = interacaoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<InteracaoProjetoResponse> listar(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        // Só página e tamanho vêm do cliente; a ordem é fixa no service.
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(interacaoService.listar(projetoId, seguro.getPageNumber(), seguro.getPageSize()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DEMANDANTE', 'ANALISTA', 'ESPECIALISTA')")
    public ResponseEntity<InteracaoProjetoResponse> registrar(
            @PathVariable("projetoId") Long projetoId,
            @Valid @RequestBody RegistrarInteracaoRequest request) {
        InteracaoProjetoResponse criada = interacaoService.registrar(projetoId, request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();

        return ResponseEntity.created(location).body(criada);
    }
}
