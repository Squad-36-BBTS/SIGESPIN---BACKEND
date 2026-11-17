package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.kanban.NotificacaoKanbanResponse;
import br.com.bbts.sigespin.service.NotificacaoKanbanService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoKanbanController {
    private final NotificacaoKanbanService notificacoes;

    public NotificacaoKanbanController(NotificacaoKanbanService notificacoes) {
        this.notificacoes = notificacoes;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<NotificacaoKanbanResponse> listar(Pageable pageable) {
        var pagina = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(notificacoes.listar(pagina.getPageNumber(), pagina.getPageSize()));
    }
}
