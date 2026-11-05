package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.trl.AvaliacaoTrlRequest;
import br.com.bbts.sigespin.dto.trl.AvaliacaoTrlResponse;
import br.com.bbts.sigespin.exception.ServicoIndisponivelException;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import br.com.bbts.sigespin.service.AvaliacaoTrlService;
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
@RequestMapping("/api/projetos/{projetoId}/avaliacoes-trl")
public class AvaliacaoTrlController {

    private final AvaliacaoTrlService avaliacaoTrlService;

    public AvaliacaoTrlController(@Autowired(required = false) AvaliacaoTrlService avaliacaoTrlService) {
        this.avaliacaoTrlService = avaliacaoTrlService;
    }

    private AvaliacaoTrlService obterServico() {
        if (this.avaliacaoTrlService == null) {
            throw new ServicoIndisponivelException("O serviço de avaliações de TRL da Fase 3 ainda não foi integrado.");
        }
        return this.avaliacaoTrlService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<AvaliacaoTrlResponse> listarPorProjeto(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(obterServico().listarPorProjeto(projetoId, seguro));
    }

    @PostMapping
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<AvaliacaoTrlResponse> registrarAvaliacao(
            @PathVariable("projetoId") Long projetoId,
            @Valid @RequestBody AvaliacaoTrlRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long autorId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        AvaliacaoTrlResponse criada = obterServico().registrarAvaliacao(projetoId, request, autorId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();

        return ResponseEntity.created(location).body(criada);
    }
}
