package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.parecer.ParecerTecnicoRequest;
import br.com.bbts.sigespin.dto.parecer.ParecerTecnicoResponse;
import br.com.bbts.sigespin.exception.ServicoIndisponivelException;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import br.com.bbts.sigespin.service.ParecerTecnicoService;
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
@RequestMapping("/api/projetos/{projetoId}/pareceres")
public class ParecerTecnicoController {

    private final ParecerTecnicoService parecerService;

    public ParecerTecnicoController(@Autowired(required = false) ParecerTecnicoService parecerService) {
        this.parecerService = parecerService;
    }

    private ParecerTecnicoService obterServico() {
        if (this.parecerService == null) {
            throw new ServicoIndisponivelException("O serviço de pareceres técnicos da Fase 3 ainda não foi integrado.");
        }
        return this.parecerService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<ParecerTecnicoResponse> listarPorProjeto(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(obterServico().listarPorProjeto(projetoId, seguro));
    }

    @PostMapping
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ResponseEntity<ParecerTecnicoResponse> emitirParecer(
            @PathVariable("projetoId") Long projetoId,
            @Valid @RequestBody ParecerTecnicoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long autorId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        ParecerTecnicoResponse criado = obterServico().emitirParecer(projetoId, request, autorId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(location).body(criado);
    }
}
