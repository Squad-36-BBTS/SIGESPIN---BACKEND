package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.kanban.DecisaoTransicaoRequest;
import br.com.bbts.sigespin.dto.kanban.EtapaKanbanResponse;
import br.com.bbts.sigespin.dto.kanban.MovimentacaoKanbanResponse;
import br.com.bbts.sigespin.dto.kanban.SolicitacaoTransicaoRequest;
import br.com.bbts.sigespin.dto.kanban.SolicitacaoTransicaoResponse;
import br.com.bbts.sigespin.dto.kanban.TransicaoKanbanResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Contrato de serviço para etapas, transições, governança e movimentações do quadro Kanban.
 * As regras de negócio transacionais e persistência são de responsabilidade da Fase 3.
 */
public interface KanbanService {

    List<EtapaKanbanResponse> listarEtapasAtivas();

    List<TransicaoKanbanResponse> listarTransicoesPorOrigem(Long origemId);

    Page<SolicitacaoTransicaoResponse> listarSolicitacoesPorProjeto(Long projetoId, Pageable pageable);

    SolicitacaoTransicaoResponse proporTransicao(Long projetoId, SolicitacaoTransicaoRequest request, Long solicitanteId);

    SolicitacaoTransicaoResponse decidirSolicitacao(Long solicitacaoId, DecisaoTransicaoRequest request, Long responsavelId);

    Page<MovimentacaoKanbanResponse> listarMovimentacoesPorProjeto(Long projetoId, Pageable pageable);
}
