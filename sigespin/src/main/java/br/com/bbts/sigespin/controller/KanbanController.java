package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.kanban.DecisaoTransicaoRequest;
import br.com.bbts.sigespin.dto.kanban.EtapaKanbanResponse;
import br.com.bbts.sigespin.dto.kanban.MovimentacaoKanbanResponse;
import br.com.bbts.sigespin.dto.kanban.SolicitacaoTransicaoRequest;
import br.com.bbts.sigespin.dto.kanban.SolicitacaoTransicaoResponse;
import br.com.bbts.sigespin.dto.kanban.TransicaoKanbanResponse;
import br.com.bbts.sigespin.exception.ServicoIndisponivelException;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import br.com.bbts.sigespin.service.KanbanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    public KanbanController(@Autowired(required = false) KanbanService kanbanService) {
        this.kanbanService = kanbanService;
    }

    private KanbanService obterServico() {
        if (this.kanbanService == null) {
            throw new ServicoIndisponivelException("O serviço de governança e Kanban da Fase 3 ainda não foi integrado.");
        }
        return this.kanbanService;
    }

    @GetMapping("/api/kanban/etapas")
    @PreAuthorize("isAuthenticated()")
    public List<EtapaKanbanResponse> listarEtapas() {
        return obterServico().listarEtapasAtivas();
    }

    @GetMapping("/api/kanban/transicoes")
    @PreAuthorize("isAuthenticated()")
    public List<TransicaoKanbanResponse> listarTransicoes(@RequestParam("origemId") Long origemId) {
        return obterServico().listarTransicoesPorOrigem(origemId);
    }

    @GetMapping("/api/projetos/{projetoId}/solicitacoes-transicao")
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<SolicitacaoTransicaoResponse> listarSolicitacoesPorProjeto(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(obterServico().listarSolicitacoesPorProjeto(projetoId, seguro));
    }

    @PostMapping("/api/projetos/{projetoId}/solicitacoes-transicao")
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<SolicitacaoTransicaoResponse> proporTransicao(
            @PathVariable("projetoId") Long projetoId,
            @Valid @RequestBody SolicitacaoTransicaoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long solicitanteId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        SolicitacaoTransicaoResponse criada = obterServico().proporTransicao(projetoId, request, solicitanteId);

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
        SolicitacaoTransicaoResponse decidida = obterServico().decidirSolicitacao(id, request, responsavelId);
        return ResponseEntity.ok(decidida);
    }

    @GetMapping("/api/projetos/{projetoId}/movimentacoes")
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<MovimentacaoKanbanResponse> listarMovimentacoes(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(obterServico().listarMovimentacoesPorProjeto(projetoId, seguro));
    }
}
