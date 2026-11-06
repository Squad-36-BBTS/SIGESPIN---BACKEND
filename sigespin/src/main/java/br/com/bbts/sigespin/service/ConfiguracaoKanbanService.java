package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.kanban.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.kanban.EtapaKanbanRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.kanban.SolicitacaoTransicaoRepository;
import br.com.bbts.sigespin.repository.kanban.TransicaoKanbanRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@Service
@Validated
public class ConfiguracaoKanbanService {
    private final EtapaKanbanRepository etapas;
    private final TransicaoKanbanRepository transicoes;
    private final ProjetoRepository projetos;
    private final SolicitacaoTransicaoRepository pedidos;
    private final UsuarioAtualService usuarioAtual;

    public ConfiguracaoKanbanService(EtapaKanbanRepository etapas, TransicaoKanbanRepository transicoes,
            ProjetoRepository projetos, SolicitacaoTransicaoRepository pedidos, UsuarioAtualService usuarioAtual) {
        this.etapas = etapas;
        this.transicoes = transicoes;
        this.projetos = projetos;
        this.pedidos = pedidos;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ColunaConfiguradaResponse criarColuna(@NotNull @Valid CriarColunaRequest entrada) {
        usuarioAtual.exigirPerfil(PerfilUsuario.ESPECIALISTA);
        if (etapas.existsByCodigo(entrada.codigo())) {
            throw new RegraNegocioException("O código da coluna já existe.");
        }
        var etapa = new EtapaKanban();
        etapa.setCodigo(entrada.codigo());
        etapa.setNome(entrada.nome().strip());
        etapa.setOrdem(entrada.ordem());
        etapa.setTerminal(entrada.terminal());
        etapa.setAtiva(true);
        try {
            etapas.saveAndFlush(etapa);
        } catch (DataIntegrityViolationException conflito) {
            // A constraint também resolve uma disputa entre duas criações simultâneas.
            throw new RegraNegocioException("Não foi possível criar a coluna. Confira os dados e o código único.");
        }
        return coluna(etapa);
    }

    @Transactional
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ColunaConfiguradaResponse editarColuna(@NotNull @Positive Long id,
            @NotNull @Valid EditarColunaRequest entrada) {
        usuarioAtual.exigirPerfil(PerfilUsuario.ESPECIALISTA);
        var etapa = etapas.findParaConfiguracao(id)
                .orElseThrow(() -> new RegraNegocioException("Coluna não encontrada."));
        conferirVersao(etapa.getVersao(), entrada.versao());
        if (!entrada.ativa() && ("IDEACAO".equals(etapa.getCodigo()) || projetos.existsByEtapaId(id)
                || transicoes.existsByAtivaTrueAndOrigemId(id) || transicoes.existsByAtivaTrueAndDestinoId(id))) {
            throw new RegraNegocioException("A coluna inicial ou utilizada por projetos e transições ativas deve permanecer ativa.");
        }
        etapa.setNome(entrada.nome().strip());
        etapa.setOrdem(entrada.ordem());
        etapa.setAtiva(entrada.ativa());
        etapas.flush();
        return coluna(etapa);
    }

    @Transactional
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public PercursoConfiguradoResponse criarPercurso(@NotNull @Valid CriarPercursoRequest entrada) {
        usuarioAtual.exigirPerfil(PerfilUsuario.ESPECIALISTA);
        var colunas = conferirPercurso(entrada);
        var percurso = new TransicaoKanban();
        percurso.setOrigem(colunas.stream().filter(e -> e.getId().equals(entrada.origemId())).findFirst().orElseThrow());
        percurso.setDestino(colunas.stream().filter(e -> e.getId().equals(entrada.destinoId())).findFirst().orElseThrow());
        percurso.setExigeGovernanca(entrada.exigeGovernanca());
        percurso.setAtiva(true);
        try {
            transicoes.saveAndFlush(percurso);
        } catch (DataIntegrityViolationException conflito) {
            throw new RegraNegocioException("O percurso não pôde ser criado. Confira a combinação única de origem e destino.");
        }
        return percurso(percurso);
    }

    @Transactional
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public PercursoConfiguradoResponse editarPercurso(@NotNull @Positive Long id,
            @NotNull @Valid EditarPercursoRequest entrada) {
        usuarioAtual.exigirPerfil(PerfilUsuario.ESPECIALISTA);
        // Mesma ordem da movimentação da parte 4A, transição antes das etapas.
        var percurso = transicoes.findParaConfiguracao(id)
                .orElseThrow(() -> new RegraNegocioException("Percurso não encontrado."));
        conferirVersao(percurso.getVersao(), entrada.versao());
        boolean mudou = !entrada.ativa().equals(percurso.getAtiva())
                || !entrada.exigeGovernanca().equals(percurso.getExigeGovernanca());
        if (mudou && pedidos.existsByTransicaoIdAndStatus(id, StatusSolicitacao.PENDENTE)) {
            throw new RegraNegocioException("Encerre os pedidos pendentes desse percurso antes de alterá-lo.");
        }
        if (entrada.ativa()) {
            conferirColunas(percurso.getOrigem().getId(), percurso.getDestino().getId());
        }
        percurso.setAtiva(entrada.ativa());
        percurso.setExigeGovernanca(entrada.exigeGovernanca());
        transicoes.flush();
        return percurso(percurso);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<ColunaConfiguradaResponse> listarColunas() {
        usuarioAtual.obterAtivo();
        return etapas.findAll(Sort.by("ordem", "id")).stream().map(this::coluna).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<PercursoConfiguradoResponse> listarPercursos() {
        usuarioAtual.obterAtivo();
        return transicoes.findAll(Sort.by("id")).stream().map(this::percurso).toList();
    }

    // Compartilhado com a validação de propostas do Analista dentro da transação do pedido.
    List<EtapaKanban> conferirPercurso(CriarPercursoRequest entrada) {
        if (transicoes.existsByOrigemIdAndDestinoId(entrada.origemId(), entrada.destinoId())) {
            throw new RegraNegocioException("Já existe um percurso com essa origem e destino.");
        }
        return conferirColunas(entrada.origemId(), entrada.destinoId());
    }

    private List<EtapaKanban> conferirColunas(Long origem, Long destino) {
        if (origem.equals(destino)) {
            throw new RegraNegocioException("Origem e destino precisam ser diferentes.");
        }
        var colunas = etapas.findParaMovimentacao(List.of(origem, destino));
        if (colunas.size() != 2 || colunas.stream().anyMatch(e -> !Boolean.TRUE.equals(e.getAtiva()))) {
            throw new RegraNegocioException("Origem e destino precisam existir e estar ativos.");
        }
        if (colunas.stream().anyMatch(e -> e.getId().equals(origem) && e.getTerminal() != null)) {
            throw new RegraNegocioException("Uma coluna terminal não pode ser origem de percurso.");
        }
        return colunas;
    }

    private void conferirVersao(Long atual, Long recebida) {
        if (!atual.equals(recebida)) {
            throw new RegraNegocioException("A configuração foi alterada. Atualize os dados antes de editar.");
        }
    }

    private ColunaConfiguradaResponse coluna(EtapaKanban etapa) {
        return new ColunaConfiguradaResponse(etapa.getId(), etapa.getCodigo(), etapa.getNome(),
                etapa.getOrdem(), etapa.getAtiva(), etapa.getTerminal(), etapa.getVersao());
    }

    private PercursoConfiguradoResponse percurso(TransicaoKanban percurso) {
        return new PercursoConfiguradoResponse(percurso.getId(), percurso.getOrigem().getId(),
                percurso.getDestino().getId(), percurso.getExigeGovernanca(), percurso.getAtiva(), percurso.getVersao());
    }
}
