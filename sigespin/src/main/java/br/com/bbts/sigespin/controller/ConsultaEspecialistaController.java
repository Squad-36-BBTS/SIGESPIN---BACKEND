package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.parecer.ConsultaEspecialistaRequest;
import br.com.bbts.sigespin.dto.parecer.ConsultaEspecialistaResponse;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import br.com.bbts.sigespin.service.ConsultaEspecialistaService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/projetos/{projetoId}/consultas")
public class ConsultaEspecialistaController {

    private final ConsultaEspecialistaService consultaService;

    public ConsultaEspecialistaController(ConsultaEspecialistaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<ConsultaEspecialistaResponse> listarPorProjeto(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(consultaService.listarPorProjeto(projetoId, seguro));
    }

    @PostMapping
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<ConsultaEspecialistaResponse> encaminharConsulta(
            @PathVariable("projetoId") Long projetoId,
            @Valid @RequestBody ConsultaEspecialistaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long solicitanteId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        ConsultaEspecialistaResponse criada = consultaService.encaminharConsulta(projetoId, request, solicitanteId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();

        return ResponseEntity.created(location).body(criada);
    }
}
