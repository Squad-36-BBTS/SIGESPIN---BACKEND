package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.kanban.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.kanban.EtapaKanbanRepository;
import br.com.bbts.sigespin.repository.kanban.SolicitacaoConfiguracaoKanbanRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDateTime;

@Service
@Validated
public class PedidoConfiguracaoKanbanService {
    private final SolicitacaoConfiguracaoKanbanRepository pedidos;
    private final EtapaKanbanRepository etapas;
    private final ConfiguracaoKanbanService configuracao;
    private final UsuarioAtualService usuarioAtual;
    private final EntityManager em;

    public PedidoConfiguracaoKanbanService(SolicitacaoConfiguracaoKanbanRepository pedidos,
            EtapaKanbanRepository etapas, ConfiguracaoKanbanService configuracao,
            UsuarioAtualService usuarioAtual, EntityManager em) {
        this.pedidos = pedidos;
        this.etapas = etapas;
        this.configuracao = configuracao;
        this.usuarioAtual = usuarioAtual;
        this.em = em;
    }

    @Transactional
    @PreAuthorize("hasRole('ANALISTA')")
    public PedidoConfiguracaoResponse solicitarColuna(@NotNull @Valid SolicitarColunaRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.ANALISTA);
        if (etapas.existsByCodigo(entrada.proposta().codigo())) {
            throw new RegraNegocioException("O código de coluna proposto já existe.");
        }
        var pedido = novoPedido(autor, TipoConfiguracaoKanban.COLUNA, entrada.justificativa());
        pedido.setCodigo(entrada.proposta().codigo());
        pedido.setNome(entrada.proposta().nome().strip());
        pedido.setOrdem(entrada.proposta().ordem());
        pedido.setTerminal(entrada.proposta().terminal());
        pedidos.saveAndFlush(pedido);
        return resposta(pedido);
    }

    @Transactional
    @PreAuthorize("hasRole('ANALISTA')")
    public PedidoConfiguracaoResponse solicitarPercurso(@NotNull @Valid SolicitarPercursoRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.ANALISTA);
        var colunas = configuracao.conferirPercurso(entrada.proposta());
        var pedido = novoPedido(autor, TipoConfiguracaoKanban.TRANSICAO, entrada.justificativa());
        pedido.setOrigem(colunas.stream().filter(e -> e.getId().equals(entrada.proposta().origemId())).findFirst().orElseThrow());
        pedido.setDestino(colunas.stream().filter(e -> e.getId().equals(entrada.proposta().destinoId())).findFirst().orElseThrow());
        pedido.setExigeGovernanca(entrada.proposta().exigeGovernanca());
        pedidos.saveAndFlush(pedido);
        return resposta(pedido);
    }

    @Transactional
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public PedidoConfiguracaoResponse decidir(@NotNull @Positive Long id,
            @NotNull @Valid DecidirGovernancaRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.ESPECIALISTA);
        var pedido = pendente(id);
        if (entrada.decisao() == DecidirGovernancaRequest.Decisao.APROVAR) {
            // A configuração é revalidada no momento da decisão. A proposta pode ter ficado obsoleta.
            if (pedido.getTipo() == TipoConfiguracaoKanban.COLUNA) {
                var criada = configuracao.criarColuna(propostaColuna(pedido));
                pedido.setColunaCriada(em.getReference(EtapaKanban.class, criada.id()));
            } else {
                var criado = configuracao.criarPercurso(propostaPercurso(pedido));
                pedido.setTransicaoCriada(em.getReference(TransicaoKanban.class, criado.id()));
            }
            encerrar(pedido, StatusSolicitacao.APROVADA, autor, entrada.justificativa());
        } else {
            encerrar(pedido, StatusSolicitacao.REJEITADA, autor, entrada.justificativa());
        }
        return resposta(pedido);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public PedidoConfiguracaoResponse cancelar(@NotNull @Positive Long id,
            @NotNull @Valid CancelarGovernancaRequest entrada) {
        var autor = usuarioAtual.exigirUmDosPerfis(PerfilUsuario.ANALISTA, PerfilUsuario.ESPECIALISTA);
        var pedido = pendente(id);
        if (autor.getPerfil() != PerfilUsuario.ESPECIALISTA && !pedido.getSolicitante().getId().equals(autor.getId())) {
            throw new AccessDeniedException("O Analista pode cancelar somente seus próprios pedidos.");
        }
        encerrar(pedido, StatusSolicitacao.CANCELADA, autor, entrada.justificativa());
        return resposta(pedido);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<PedidoConfiguracaoResponse> listar(@NotNull StatusSolicitacao status,
            @Min(0) int pagina, @Min(1) @Max(100) int tamanho) {
        usuarioAtual.obterAtivo();
        return pedidos.findByStatus(status, PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Direction.DESC, "criadoEm", "id"))).map(this::resposta);
    }

    private SolicitacaoConfiguracaoKanban novoPedido(Usuario autor, TipoConfiguracaoKanban tipo, String justificativa) {
        var pedido = new SolicitacaoConfiguracaoKanban();
        pedido.setSolicitante(autor);
        pedido.setTipo(tipo);
        pedido.setJustificativa(justificativa.strip());
        pedido.setStatus(StatusSolicitacao.PENDENTE);
        pedido.setCriadoEm(LocalDateTime.now());
        return pedido;
    }

    private SolicitacaoConfiguracaoKanban pendente(Long id) {
        var pedido = pedidos.findParaDecisao(id)
                .orElseThrow(() -> new RegraNegocioException("Pedido de configuração não encontrado."));
        if (pedido.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new RegraNegocioException("O pedido de configuração já foi encerrado.");
        }
        return pedido;
    }

    private void encerrar(SolicitacaoConfiguracaoKanban pedido, StatusSolicitacao status,
            Usuario autor, String justificativa) {
        pedido.setStatus(status);
        pedido.setResponsavelEncerramento(autor);
        pedido.setJustificativaEncerramento(justificativa.strip());
        pedido.setEncerradoEm(LocalDateTime.now());
        pedidos.saveAndFlush(pedido);
    }

    private CriarColunaRequest propostaColuna(SolicitacaoConfiguracaoKanban pedido) {
        return new CriarColunaRequest(pedido.getCodigo(), pedido.getNome(), pedido.getOrdem(), pedido.getTerminal());
    }

    private CriarPercursoRequest propostaPercurso(SolicitacaoConfiguracaoKanban pedido) {
        return new CriarPercursoRequest(pedido.getOrigem().getId(), pedido.getDestino().getId(), pedido.getExigeGovernanca());
    }

    private PedidoConfiguracaoResponse resposta(SolicitacaoConfiguracaoKanban pedido) {
        return new PedidoConfiguracaoResponse(pedido.getId(), pedido.getTipo(), pedido.getSolicitante().getId(),
                pedido.getTipo() == TipoConfiguracaoKanban.COLUNA ? propostaColuna(pedido) : null,
                pedido.getTipo() == TipoConfiguracaoKanban.TRANSICAO ? propostaPercurso(pedido) : null,
                pedido.getJustificativa(), pedido.getStatus(), pedido.getCriadoEm(),
                pedido.getResponsavelEncerramento() == null ? null : pedido.getResponsavelEncerramento().getId(),
                pedido.getJustificativaEncerramento(), pedido.getEncerradoEm(),
                pedido.getColunaCriada() == null ? null : pedido.getColunaCriada().getId(),
                pedido.getTransicaoCriada() == null ? null : pedido.getTransicaoCriada().getId(), pedido.getVersao());
    }
}
