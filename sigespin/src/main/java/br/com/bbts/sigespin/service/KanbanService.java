package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.kanban.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import br.com.bbts.sigespin.repository.kanban.EtapaKanbanRepository;
import br.com.bbts.sigespin.repository.kanban.MovimentacaoKanbanRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.kanban.SolicitacaoTransicaoRepository;
import br.com.bbts.sigespin.repository.kanban.TransicaoKanbanRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class KanbanService {
    private final ProjetoRepository projetos;
    private final TransicaoKanbanRepository transicoes;
    private final EtapaKanbanRepository etapas;
    private final SolicitacaoTransicaoRepository solicitacoes;
    private final MovimentacaoKanbanRepository movimentacoes;
    private final UsuarioAtualService usuarioAtual;
    private final EvidenciasDocumentoService documentos;
    private final EntityManager em;

    public KanbanService(ProjetoRepository projetos, TransicaoKanbanRepository transicoes,
            EtapaKanbanRepository etapas, SolicitacaoTransicaoRepository solicitacoes,
            MovimentacaoKanbanRepository movimentacoes, UsuarioAtualService usuarioAtual,
            EvidenciasDocumentoService documentos, EntityManager em) {
        this.projetos = projetos;
        this.transicoes = transicoes;
        this.etapas = etapas;
        this.solicitacoes = solicitacoes;
        this.movimentacoes = movimentacoes;
        this.usuarioAtual = usuarioAtual;
        this.documentos = documentos;
        this.em = em;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public MovimentacaoRegistradaResponse movimentar(@NotNull @Positive Long projetoId,
            @NotNull @Valid MovimentarProjetoRequest entrada) {
        var autor = usuarioAtual.exigirUmDosPerfis(PerfilUsuario.ANALISTA, PerfilUsuario.ESPECIALISTA);
        var projeto = bloquearProjeto(projetoId);
        exigirSemPedidoPendente(projetoId);
        var transicao = validarTransicao(projeto, entrada.transicaoId());
        if (Boolean.TRUE.equals(transicao.getExigeGovernanca()) && autor.getPerfil() != PerfilUsuario.ESPECIALISTA) {
            throw new RegraNegocioException("Esta transição exige uma solicitação de governança.");
        }
        return paraResposta(executar(projeto, transicao, autor, entrada.justificativa().strip(), null));
    }

    @Transactional
    @PreAuthorize("hasRole('ANALISTA')")
    public SolicitacaoGovernancaResponse solicitar(@NotNull @Positive Long projetoId,
            @NotNull @Valid SolicitarGovernancaRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.ANALISTA);
        var projeto = bloquearProjeto(projetoId);
        exigirSemPedidoPendente(projetoId);
        var transicao = validarTransicao(projeto, entrada.transicaoId());
        if (!Boolean.TRUE.equals(transicao.getExigeGovernanca())) {
            throw new RegraNegocioException("Esta transição pode ser executada sem solicitação de governança.");
        }
        var evidencias = documentos.carregarDoProjeto(projetoId, entrada.evidenciaIds());
        var pedido = new SolicitacaoTransicao();
        pedido.setProjeto(projeto);
        pedido.setSolicitante(autor);
        pedido.setTransicao(transicao);
        // O snapshot mantém o percurso solicitado mesmo se a configuração mudar depois.
        pedido.setOrigem(transicao.getOrigem());
        pedido.setDestino(transicao.getDestino());
        pedido.setExigeGovernanca(true);
        pedido.setStatus(StatusSolicitacao.PENDENTE);
        pedido.setJustificativa(entrada.justificativa().strip());
        pedido.setCriadoEm(LocalDateTime.now());
        solicitacoes.saveAndFlush(pedido);
        for (var evidencia : evidencias) {
            var vinculo = new EvidenciaTransicao();
            vinculo.setProjeto(projeto);
            vinculo.setRegistro(pedido);
            vinculo.setEvidencia(evidencia);
            em.persist(vinculo);
        }
        em.flush();
        return paraResposta(pedido);
    }

    @Transactional
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public SolicitacaoGovernancaResponse decidir(@NotNull @Positive Long solicitacaoId,
            @NotNull @Valid DecidirGovernancaRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.ESPECIALISTA);
        var pedido = bloquearPedidoPendente(solicitacaoId);
        if (entrada.decisao() == DecidirGovernancaRequest.Decisao.REJEITAR) {
            encerrar(pedido, StatusSolicitacao.REJEITADA, autor, entrada.justificativa());
        } else {
            var transicao = validarTransicao(pedido.getProjeto(), pedido.getTransicao().getId());
            if (!Boolean.TRUE.equals(transicao.getExigeGovernanca())
                    || !transicao.getOrigem().getId().equals(pedido.getOrigem().getId())
                    || !transicao.getDestino().getId().equals(pedido.getDestino().getId())) {
                throw new RegraNegocioException("A configuração diverge da solicitação. Cancele e registre um novo pedido.");
            }
            encerrar(pedido, StatusSolicitacao.APROVADA, autor, entrada.justificativa());
            executar(pedido.getProjeto(), transicao, autor, entrada.justificativa().strip(), pedido);
        }
        return paraResposta(pedido);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public SolicitacaoGovernancaResponse cancelar(@NotNull @Positive Long solicitacaoId,
            @NotNull @Valid CancelarGovernancaRequest entrada) {
        var autor = usuarioAtual.exigirUmDosPerfis(PerfilUsuario.ANALISTA, PerfilUsuario.ESPECIALISTA);
        var pedido = bloquearPedidoPendente(solicitacaoId);
        if (autor.getPerfil() != PerfilUsuario.ESPECIALISTA
                && !pedido.getSolicitante().getId().equals(autor.getId())) {
            throw new AccessDeniedException("O Analista pode cancelar somente sua própria solicitação.");
        }
        encerrar(pedido, StatusSolicitacao.CANCELADA, autor, entrada.justificativa());
        return paraResposta(pedido);
    }

    private Projeto bloquearProjeto(Long projetoId) {
        return projetos.findAtivoParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado ou arquivado."));
    }

    private SolicitacaoTransicao bloquearPedidoPendente(Long solicitacaoId) {
        var projetoId = solicitacoes.findProjetoId(solicitacaoId)
                .orElseThrow(() -> new RegraNegocioException("Solicitação não encontrada."));
        // Ordem de bloqueio comum a todos os caminhos reduz disputas entre decisão e movimento.
        bloquearProjeto(projetoId);
        var pedido = solicitacoes.findParaDecisao(solicitacaoId)
                .orElseThrow(() -> new RegraNegocioException("Solicitação não encontrada."));
        if (pedido.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new RegraNegocioException("A solicitação já foi encerrada.");
        }
        return pedido;
    }

    private void exigirSemPedidoPendente(Long projetoId) {
        if (solicitacoes.existsByProjetoIdAndStatus(projetoId, StatusSolicitacao.PENDENTE)) {
            throw new RegraNegocioException("Decida ou cancele a solicitação pendente antes de movimentar o projeto.");
        }
    }

    private TransicaoKanban validarTransicao(Projeto projeto, Long transicaoId) {
        var transicao = transicoes.findParaExecucao(transicaoId)
                .orElseThrow(() -> new RegraNegocioException("Transição não encontrada."));
        var colunas = etapas.findParaMovimentacao(List.of(transicao.getOrigem().getId(), transicao.getDestino().getId()));
        if (!Boolean.TRUE.equals(transicao.getAtiva())
                || colunas.stream().anyMatch(etapa -> !Boolean.TRUE.equals(etapa.getAtiva()))) {
            throw new RegraNegocioException("A transição e suas etapas precisam estar ativas.");
        }
        if (!projeto.getEtapa().getId().equals(transicao.getOrigem().getId())) {
            throw new RegraNegocioException("A etapa atual do projeto não corresponde à origem da transição.");
        }
        if (transicao.getOrigem().getTerminal() != null) {
            throw new RegraNegocioException("Uma etapa terminal não permite novas movimentações.");
        }
        return transicao;
    }

    private void encerrar(SolicitacaoTransicao pedido, StatusSolicitacao status, Usuario autor, String justificativa) {
        pedido.setStatus(status);
        pedido.setResponsavelEncerramento(autor);
        pedido.setJustificativaEncerramento(justificativa.strip());
        pedido.setEncerradoEm(LocalDateTime.now());
        solicitacoes.saveAndFlush(pedido);
    }

    private MovimentacaoKanban executar(Projeto projeto, TransicaoKanban transicao, Usuario autor,
            String justificativa, SolicitacaoTransicao pedido) {
        var movimento = new MovimentacaoKanban();
        movimento.setProjeto(projeto);
        movimento.setAutor(autor);
        movimento.setOrigem(transicao.getOrigem());
        movimento.setDestino(transicao.getDestino());
        movimento.setSolicitacao(pedido);
        movimento.setJustificativa(justificativa);
        movimento.setMovimentadoEm(LocalDateTime.now());
        movimentacoes.saveAndFlush(movimento);
        projeto.setEtapa(transicao.getDestino());

        if (autor.getPerfil() == PerfilUsuario.ESPECIALISTA) {
            var notificacao = new NotificacaoKanban();
            notificacao.setMovimentacao(movimento);
            notificacao.setCriadaEm(movimento.getMovimentadoEm());
            notificacao.setMensagem(autor.getNome() + " movimentou o projeto " + projeto.getTitulo()
                    + " de " + transicao.getOrigem().getNome() + " para " + transicao.getDestino().getNome()
                    + ". Justificativa: " + justificativa);
            em.persist(notificacao);
        }
        // Decisão, auditoria, etapa e notificação compartilham o mesmo commit.
        em.flush();
        return movimento;
    }

    private MovimentacaoRegistradaResponse paraResposta(MovimentacaoKanban movimento) {
        return new MovimentacaoRegistradaResponse(movimento.getId(), movimento.getProjeto().getId(),
                movimento.getAutor().getId(), movimento.getOrigem().getId(), movimento.getDestino().getId(),
                movimento.getSolicitacao() == null ? null : movimento.getSolicitacao().getId(),
                movimento.getJustificativa(), movimento.getMovimentadoEm());
    }

    private SolicitacaoGovernancaResponse paraResposta(SolicitacaoTransicao pedido) {
        return new SolicitacaoGovernancaResponse(pedido.getId(), pedido.getProjeto().getId(),
                pedido.getSolicitante().getId(), pedido.getTransicao().getId(), pedido.getOrigem().getId(),
                pedido.getDestino().getId(), pedido.getStatus(), pedido.getJustificativa(), pedido.getCriadoEm(),
                pedido.getResponsavelEncerramento() == null ? null : pedido.getResponsavelEncerramento().getId(),
                pedido.getJustificativaEncerramento(), pedido.getEncerradoEm());
    }
}
