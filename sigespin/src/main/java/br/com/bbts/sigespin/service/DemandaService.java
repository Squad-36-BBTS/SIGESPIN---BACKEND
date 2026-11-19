package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.demanda.DemandaRegistradaResponse;
import br.com.bbts.sigespin.dto.demanda.RegistrarDemandaRequest;
import br.com.bbts.sigespin.model.entity.AcaoMobilizacao;
import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.portfolio.AcaoMobilizacaoRepository;
import br.com.bbts.sigespin.repository.kanban.EtapaKanbanRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class DemandaService {
    private final ProjetoRepository projetos;
    private final EtapaKanbanRepository etapas;
    private final AcaoMobilizacaoRepository acoes;
    private final UsuarioAtualService usuarioAtual;

    public DemandaService(ProjetoRepository projetos, EtapaKanbanRepository etapas,
                          AcaoMobilizacaoRepository acoes, UsuarioAtualService usuarioAtual) {
        this.projetos = projetos;
        this.etapas = etapas;
        this.acoes = acoes;
        this.usuarioAtual = usuarioAtual;
    }

    /**
     * Caso de uso de intake. A transação mantém validações e gravação na mesma unidade.
     * A autorização fica no serviço para valer também fora de um futuro controller HTTP.
     */
    @Transactional
    @PreAuthorize("hasRole('DEMANDANTE')")
    public DemandaRegistradaResponse registrar(@NotNull @Valid RegistrarDemandaRequest entrada) {
        var demandante = usuarioAtual.obterAtivo();
        if (demandante.getPerfil() != PerfilUsuario.DEMANDANTE) {
            throw new AccessDeniedException("Somente Demandante pode registrar uma demanda.");
        }

        AcaoMobilizacao acao = resolverAcao(entrada);
        // O código identifica a etapa inicial; a ordenação visual pode mudar no Kanban.
        var etapa = etapas.findByCodigoAndAtivaTrue("IDEACAO")
                .orElseThrow(() -> new RegraNegocioException("A etapa Ideação precisa estar ativa."));
        if (etapa.getTerminal() != null) {
            throw new RegraNegocioException("A etapa inicial não pode ser terminal.");
        }

        Projeto projeto = new Projeto();
        projeto.setTitulo(entrada.titulo().strip());
        projeto.setDescricaoDor(entrada.descricaoDor().strip());
        projeto.setAreaDemandanteId(entrada.areaDemandanteId());
        projeto.setDemandante(demandante);
        // Compatibilidade com o campo PO obrigatório herdado do domínio inicial.
        projeto.setResponsavelPo(demandante);
        projeto.setOrigem(entrada.origem());
        projeto.setAcaoMobilizacao(acao);
        projeto.setEtapa(etapa);
        projeto.setArena(ArenaTipo.A_DEFINIR);
        // Declaração do intake, sem fabricar uma avaliação técnica ou evidências.
        projeto.setTrlAtual(entrada.trlInicialDeclarado());
        projeto.setHipoteseSolucao(entrada.hipoteseSolucao() == null
                ? null : entrada.hipoteseSolucao().strip());
        // Flush antecipa violações de integridade; commit ainda depende do fim da transação.
        projetos.saveAndFlush(projeto);
        return new DemandaRegistradaResponse(projeto.getId(), projeto.getTitulo(), demandante.getId(),
                projeto.getOrigem(), acao == null ? null : acao.getId(), etapa.getId(),
                projeto.getTrlAtual(), projeto.getCriadoEm());
    }

    private AcaoMobilizacao resolverAcao(RegistrarDemandaRequest entrada) {
        if (entrada.origem() == OrigemDemanda.ESPONTANEA) {
            if (entrada.acaoMobilizacaoId() != null) {
                throw new RegraNegocioException("Demanda espontânea não pode ter ação de mobilização.");
            }
            return null;
        }
        if (entrada.acaoMobilizacaoId() == null) {
            throw new RegraNegocioException("Demanda induzida exige ação de mobilização.");
        }
        return acoes.findById(entrada.acaoMobilizacaoId())
                .orElseThrow(() -> new RegraNegocioException("Ação de mobilização não encontrada."));
    }
}
