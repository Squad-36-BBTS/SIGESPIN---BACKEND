package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.kanban.NotificacaoKanbanResponse;
import br.com.bbts.sigespin.repository.kanban.NotificacaoKanbanRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class NotificacaoKanbanService {
    private final NotificacaoKanbanRepository notificacoes;
    private final UsuarioAtualService usuarioAtual;

    public NotificacaoKanbanService(NotificacaoKanbanRepository notificacoes, UsuarioAtualService usuarioAtual) {
        this.notificacoes = notificacoes;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<NotificacaoKanbanResponse> listar(@Min(0) int pagina, @Min(1) @Max(100) int tamanho) {
        usuarioAtual.obterAtivo();
        var ordem = Sort.by(Sort.Direction.DESC, "criadaEm", "id");
        return notificacoes.findAll(PageRequest.of(pagina, tamanho, ordem)).map(n ->
                new NotificacaoKanbanResponse(n.getId(), n.getMovimentacao().getId(),
                        n.getMovimentacao().getProjeto().getId(), n.getMensagem(), n.getCriadaEm()));
    }
}
