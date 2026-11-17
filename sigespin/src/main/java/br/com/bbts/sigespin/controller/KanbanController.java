package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.kanban.MovimentarProjetoRequest;
import br.com.bbts.sigespin.dto.kanban.MovimentacaoRegistradaResponse;
import br.com.bbts.sigespin.dto.kanban.CancelarGovernancaRequest;
import br.com.bbts.sigespin.dto.kanban.SolicitacaoGovernancaResponse;
import br.com.bbts.sigespin.dto.kanban.DecisaoTransicaoRequest;
import br.com.bbts.sigespin.dto.kanban.EtapaKanbanResponse;
import br.com.bbts.sigespin.dto.kanban.MovimentacaoKanbanResponse;
import br.com.bbts.sigespin.dto.kanban.SolicitacaoTransicaoRequest;
import br.com.bbts.sigespin.dto.kanban.SolicitacaoTransicaoResponse;
import br.com.bbts.sigespin.dto.kanban.TransicaoKanbanResponse;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import br.com.bbts.sigespin.service.KanbanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping
public class KanbanController {

    private final KanbanService kanbanService;

    public KanbanController(KanbanService kanbanService) {
        this.kanbanService = kanbanService;
    }

    @GetMapping("/api/kanban/etapas")
    @PreAuthorize("isAuthenticated()")
    public List<EtapaKanbanResponse> listarEtapas() {
        return kanbanService.listarEtapasAtivas();
    }

    @GetMapping("/api/kanban/transicoes")
    @PreAuthorize("isAuthenticated()")
    public List<TransicaoKanbanResponse> listarTransicoes(@RequestParam("origemId") Long origemId) {
        return kanbanService.listarTransicoesPorOrigem(origemId);
    }

    @GetMapping("/api/projetos/{projetoId}/solicitacoes-transicao")
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<SolicitacaoTransicaoResponse> listarSolicitacoesPorProjeto(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(kanbanService.listarSolicitacoesPorProjeto(projetoId, seguro));
    }

    @PostMapping("/api/projetos/{projetoId}/solicitacoes-transicao")
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<SolicitacaoTransicaoResponse> proporTransicao(
            @PathVariable("projetoId") Long projetoId,
            @Valid @RequestBody SolicitacaoTransicaoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long solicitanteId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        SolicitacaoTransicaoResponse criada = kanbanService.proporTransicao(projetoId, request, solicitanteId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();

        return ResponseEntity.created(location).body(criada);
    }

    @PatchMapping("/api/kanban/solicitacoes-transicao/{id}/decisao")
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ResponseEntity<SolicitacaoTransicaoResponse> decidirSolicitacao(
            @PathVariable("id") Long id,
            @Valid @RequestBody DecisaoTransicaoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long responsavelId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        SolicitacaoTransicaoResponse decidida = kanbanService.decidirSolicitacao(id, request, responsavelId);
        return ResponseEntity.ok(decidida);
    }

    @GetMapping("/api/projetos/{projetoId}/movimentacoes")
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<MovimentacaoKanbanResponse> listarMovimentacoes(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(kanbanService.listarMovimentacoesPorProjeto(projetoId, seguro));
    }

    @PostMapping("/api/projetos/{projetoId}/movimentacoes")
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public ResponseEntity<MovimentacaoRegistradaResponse> movimentar(@PathVariable Long projetoId,
            @Valid @RequestBody MovimentarProjetoRequest entrada) {
        var criada = kanbanService.movimentar(projetoId, entrada);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PostMapping("/api/kanban/solicitacoes-transicao/{id}/cancelamento")
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public SolicitacaoGovernancaResponse cancelar(@PathVariable Long id,
            @Valid @RequestBody CancelarGovernancaRequest entrada) {
        return kanbanService.cancelar(id, entrada);
    }
}
