package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.projeto.DemandaCreateRequest;
import br.com.bbts.sigespin.dto.projeto.ProjetoDetalheResponse;
import br.com.bbts.sigespin.dto.projeto.ProjetoResponse;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import br.com.bbts.sigespin.service.ProjetoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<ProjetoResponse> listarProjetos(
            @RequestParam(name = "arena", required = false) ArenaTipo arena,
            @RequestParam(name = "etapaId", required = false) Long etapaId,
            @RequestParam(name = "busca", required = false) String busca,
            @RequestParam(name = "arquivado", required = false) Boolean arquivado,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(projetoService.listarProjetos(arena, etapaId, busca, arquivado, seguro));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProjetoDetalheResponse buscarPorId(@PathVariable("id") Long id) {
        return projetoService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('DEMANDANTE')")
    public ResponseEntity<ProjetoDetalheResponse> criarDemanda(
            @Valid @RequestBody DemandaCreateRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long usuarioId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        ProjetoDetalheResponse criado = projetoService.criarDemanda(request, usuarioId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(location).body(criado);
    }
}
