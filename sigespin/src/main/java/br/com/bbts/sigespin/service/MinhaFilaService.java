package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.projeto.EtapaResumoResponse;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.CampanhaFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.ConsultaFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.EntregaFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.EtapaFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.EvidenciaFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.ParecerFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.ProjetoFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.ProjetoReferencia;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.PropostaColuna;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.PropostaFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.PropostaTransicao;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.RecomendacaoFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.SolicitacaoFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.UsuarioFila;
import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse.ValidacaoFila;
import br.com.bbts.sigespin.model.entity.ConsultaEspecialista;
import br.com.bbts.sigespin.model.entity.EntregaProjeto;
import br.com.bbts.sigespin.model.entity.EtapaKanban;
import br.com.bbts.sigespin.model.entity.Evidencia;
import br.com.bbts.sigespin.model.entity.EvidenciaParecer;
import br.com.bbts.sigespin.model.entity.EvidenciaTransicao;
import br.com.bbts.sigespin.model.entity.ParecerTecnico;
import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.entity.RecomendacaoDemanda;
import br.com.bbts.sigespin.model.entity.SolicitacaoConfiguracaoKanban;
import br.com.bbts.sigespin.model.entity.SolicitacaoTransicao;
import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.entity.ValidacaoEntrega;
import br.com.bbts.sigespin.model.enums.EstadoTarefa;
import br.com.bbts.sigespin.model.enums.StatusRecomendacao;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import br.com.bbts.sigespin.model.enums.TipoConfiguracaoKanban;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Minha fila (BE-12): o que espera ação do usuário atual, conforme o perfil.
 *
 * <ul>
 *   <li><b>Demandante:</b> entregas sem validação nos seus projetos ativos, recomendações pendentes dirigidas a
 *   ele e as suas demandas (com pendências).</li>
 *   <li><b>Analista:</b> projetos aguardando triagem (de todos), seus pedidos de avanço pendentes, seus pedidos
 *   encerrados (aprovados, rejeitados ou cancelados) nos últimos 30 dias, pareceres às suas consultas nos últimos
 *   30 dias, validações nos projetos em que é responsável nos últimos 30 dias e suas propostas pendentes.</li>
 *   <li><b>Especialista:</b> consultas dirigidas a ele ainda sem parecer em projetos ativos, e todos os pedidos de
 *   avanço e propostas de configuração pendentes.</li>
 * </ul>
 *
 * <p>Somente leitura: nenhuma regra de escrita muda.</p>
 */
@Service
public class MinhaFilaService {
    static final int JANELA_RETORNOS_DIAS = 30;


    private final EntityManager em;
    private final UsuarioAtualService usuarioAtual;
    private final PendenciaProjetoService pendencias;

    public MinhaFilaService(EntityManager em, UsuarioAtualService usuarioAtual, PendenciaProjetoService pendencias) {
        this.em = em;
        this.usuarioAtual = usuarioAtual;
        this.pendencias = pendencias;
    }

    @Transactional(readOnly = true)
    public MinhaFilaResponse obter() {
        Usuario usuario = usuarioAtual.obterAtivo();
        LocalDateTime limite = LocalDateTime.now().minusDays(JANELA_RETORNOS_DIAS);
        return switch (usuario.getPerfil()) {
            case DEMANDANTE -> new MinhaFilaResponse(
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    entregas(entregasParaValidar(usuario)),
                    List.of(),
                    projetos(minhasDemandas(usuario)),
                    recomendacoesPendentes(usuario));
            case ANALISTA -> new MinhaFilaResponse(
                    projetos(triagem()),
                    List.of(),
                    solicitacoes(solicitacoesPendentes(usuario)),
                    solicitacoes(solicitacoesEncerradas(usuario, limite)),
                    pareceresRecebidos(usuario, limite),
                    propostas(propostasPendentes(usuario)),
                    List.of(),
                    entregas(entregasValidadas(usuario, limite)),
                    List.of(),
                    List.of());
            case ESPECIALISTA -> new MinhaFilaResponse(
                    List.of(),
                    consultasPendentes(usuario),
                    solicitacoes(solicitacoesPendentes(null)),
                    List.of(), List.of(),
                    propostas(propostasPendentes(null)),
                    List.of(), List.of(), List.of(), List.of());
        };
    }

    // ---------- Consultas por perfil ----------

    private List<EntregaProjeto> entregasParaValidar(Usuario demandante) {
        return em.createQuery("""
                        select e from EntregaProjeto e join fetch e.projeto p join fetch e.autor
                        where p.demandante = :u and p.arquivado = false
                          and not exists (select 1 from ValidacaoEntrega v where v.entrega = e)
                        order by e.submetidoEm, e.id""", EntregaProjeto.class)
                .setParameter("u", demandante).getResultList();
    }

    private List<Projeto> minhasDemandas(Usuario demandante) {
        return em.createQuery("""
                        select p from Projeto p join fetch p.etapa
                        where p.demandante = :u order by p.criadoEm desc, p.id desc""", Projeto.class)
                .setParameter("u", demandante).getResultList();
    }

    private List<RecomendacaoFila> recomendacoesPendentes(Usuario destinatario) {
        return em.createQuery("""
                        select r from RecomendacaoDemanda r join fetch r.autor
                        where r.destinatario = :u and r.status = :pendente order by r.criadoEm, r.id""",
                        RecomendacaoDemanda.class)
                .setParameter("u", destinatario)
                .setParameter("pendente", StatusRecomendacao.PENDENTE)
                .getResultList().stream().map(this::recomendacao).toList();
    }

    /** Todos os projetos ativos, fora de etapa terminal e sem qualificação, do mais antigo ao mais novo. */
    private List<Projeto> triagem() {
        return em.createQuery("""
                        select p from Projeto p join fetch p.etapa et
                        where p.arquivado = false and et.terminal is null
                          and not exists (select 1 from QualificacaoDemanda q where q.projeto = p)
                        order by p.criadoEm, p.id""", Projeto.class)
                .getResultList();
    }

    /** Pendentes do solicitante informado; com {@code null}, todos os pendentes (visão do Especialista). */
    private List<SolicitacaoTransicao> solicitacoesPendentes(Usuario solicitante) {
        var consulta = em.createQuery("""
                        select s from SolicitacaoTransicao s join fetch s.projeto join fetch s.origem join fetch s.destino
                        join fetch s.solicitante
                        where s.status = :pendente""" + (solicitante == null ? "" : " and s.solicitante = :u") + """

                        order by s.criadoEm, s.id""", SolicitacaoTransicao.class)
                .setParameter("pendente", StatusSolicitacao.PENDENTE);
        if (solicitante != null) {
            consulta.setParameter("u", solicitante);
        }
        return consulta.getResultList();
    }

    /** Aprovados, rejeitados e cancelados nos últimos 30 dias, do mais recente ao mais antigo. */
    private List<SolicitacaoTransicao> solicitacoesEncerradas(Usuario solicitante, LocalDateTime limite) {
        return em.createQuery("""
                        select s from SolicitacaoTransicao s join fetch s.projeto join fetch s.origem join fetch s.destino
                        join fetch s.solicitante
                        where s.solicitante = :u and s.status <> :pendente and s.encerradoEm >= :limite
                        order by s.encerradoEm desc, s.id desc""", SolicitacaoTransicao.class)
                .setParameter("u", solicitante)
                .setParameter("pendente", StatusSolicitacao.PENDENTE)
                .setParameter("limite", limite)
                .getResultList();
    }

    private List<ParecerFila> pareceresRecebidos(Usuario solicitante, LocalDateTime limite) {
        List<ParecerTecnico> pareceres = em.createQuery("""
                        select p from ParecerTecnico p join fetch p.projeto join fetch p.autor join fetch p.consulta c
                        where c.solicitante = :u and p.criadoEm >= :limite
                        order by p.criadoEm desc, p.id desc""", ParecerTecnico.class)
                .setParameter("u", solicitante)
                .setParameter("limite", limite)
                .getResultList();
        Map<Long, List<EvidenciaFila>> evidencias = pareceres.isEmpty() ? Map.of() : em.createQuery("""
                        select ep from EvidenciaParecer ep join fetch ep.evidencia ev
                        where ep.registro.id in :ids order by ev.id""", EvidenciaParecer.class)
                .setParameter("ids", pareceres.stream().map(ParecerTecnico::getId).toList())
                .getResultList().stream()
                .collect(Collectors.groupingBy(ep -> ep.getRegistro().getId(),
                        Collectors.mapping(ep -> evidencia(ep.getEvidencia()), Collectors.toList())));
        return pareceres.stream().map(p -> new ParecerFila(
                p.getId(),
                p.getProjeto().getId(),
                p.getProjeto().getTitulo(),
                p.getConsulta().getId(),
                p.getConsulta().getQuestao(),
                usuario(p.getAutor()),
                p.getFundamentacao(),
                p.getConclusao(),
                evidencias.getOrDefault(p.getId(), List.of()),
                p.getCriadoEm())).toList();
    }

    /** Pendentes do autor informado; com {@code null}, todas as pendentes (visão do Especialista). */
    private List<SolicitacaoConfiguracaoKanban> propostasPendentes(Usuario autor) {
        var consulta = em.createQuery("""
                        select s from SolicitacaoConfiguracaoKanban s join fetch s.solicitante
                        where s.status = :pendente""" + (autor == null ? "" : " and s.solicitante = :u") + """

                        order by s.criadoEm, s.id""", SolicitacaoConfiguracaoKanban.class)
                .setParameter("pendente", StatusSolicitacao.PENDENTE);
        if (autor != null) {
            consulta.setParameter("u", autor);
        }
        return consulta.getResultList();
    }

    /** Entregas dos projetos em que o Analista é responsável, com validação registrada nos últimos 30 dias. */
    private List<EntregaProjeto> entregasValidadas(Usuario analista, LocalDateTime limite) {
        return em.createQuery("""
                        select e from EntregaProjeto e join fetch e.projeto p join fetch e.autor
                        where p.analistaResponsavel = :u
                          and exists (select 1 from ValidacaoEntrega v where v.entrega = e and v.validadoEm >= :limite)
                        order by e.submetidoEm desc, e.id desc""", EntregaProjeto.class)
                .setParameter("u", analista)
                .setParameter("limite", limite)
                .getResultList();
    }

    private List<ConsultaFila> consultasPendentes(Usuario especialista) {
        return em.createQuery("""
                        select c from ConsultaEspecialista c join fetch c.projeto p join fetch c.solicitante
                        where c.especialista = :u and p.arquivado = false
                          and not exists (select 1 from ParecerTecnico pt where pt.consulta = c)
                        order by c.criadoEm, c.id""", ConsultaEspecialista.class)
                .setParameter("u", especialista)
                .getResultList().stream().map(c -> new ConsultaFila(
                        c.getId(),
                        c.getProjeto().getId(),
                        c.getProjeto().getTitulo(),
                        usuario(c.getSolicitante()),
                        usuario(especialista),
                        null,
                        c.getQuestao(),
                        "PENDENTE",
                        null,
                        c.getCriadoEm(),
                        null)).toList();
    }

    // ---------- Conversões para o formato do frontend ----------

    private List<ProjetoFila> projetos(List<Projeto> projetos) {
        Map<Long, List<EstadoTarefa>> porProjeto = pendencias.calcular(projetos);
        return projetos.stream().map(p -> new ProjetoFila(
                p.getId(),
                p.getTitulo(),
                p.getDescricaoDor(),
                p.getOrigem(),
                p.getAcaoMobilizacao() == null ? null
                        : new CampanhaFila(p.getAcaoMobilizacao().getId(), p.getAcaoMobilizacao().getNome(), "OUTRA"),
                p.getAreaDemandanteId(),
                null,
                usuario(p.getDemandante()),
                usuario(p.getAnalistaResponsavel()),
                p.getArena(),
                p.getHipoteseSolucao(),
                p.getPotencialLeiDoBem(),
                p.getDispendioEstimado(),
                etapa(p.getEtapa()),
                p.getTrlAtual(),
                porProjeto.getOrDefault(p.getId(), List.of()),
                Boolean.TRUE.equals(p.getArquivado()),
                p.getMotivoArquivamento(),
                p.getArquivadoEm(),
                p.getVersao(),
                p.getCriadoEm(),
                p.getAtualizadoEm())).toList();
    }

    private List<SolicitacaoFila> solicitacoes(List<SolicitacaoTransicao> solicitacoes) {
        Map<Long, List<EvidenciaFila>> evidencias = solicitacoes.isEmpty() ? Map.of() : em.createQuery("""
                        select et from EvidenciaTransicao et join fetch et.evidencia ev
                        where et.registro.id in :ids order by ev.id""", EvidenciaTransicao.class)
                .setParameter("ids", solicitacoes.stream().map(SolicitacaoTransicao::getId).toList())
                .getResultList().stream()
                .collect(Collectors.groupingBy(et -> et.getRegistro().getId(),
                        Collectors.mapping(et -> evidencia(et.getEvidencia()), Collectors.toList())));
        return solicitacoes.stream().map(s -> new SolicitacaoFila(
                s.getId(),
                s.getProjeto().getId(),
                s.getProjeto().getTitulo(),
                s.getTransicao() == null ? null : s.getTransicao().getId(),
                etapa(s.getOrigem()),
                etapa(s.getDestino()),
                usuario(s.getSolicitante()),
                s.getJustificativa(),
                evidencias.getOrDefault(s.getId(), List.of()),
                s.getStatus(),
                usuario(s.getResponsavelEncerramento()),
                s.getJustificativaEncerramento(),
                s.getEncerradoEm(),
                s.getCriadoEm())).toList();
    }

    private List<PropostaFila> propostas(List<SolicitacaoConfiguracaoKanban> propostas) {
        return propostas.stream().map(s -> new PropostaFila(
                s.getId(),
                s.getTipo(),
                s.getTipo() == TipoConfiguracaoKanban.COLUNA
                        ? new PropostaColuna(s.getCodigo(), s.getNome(), s.getOrdem(), s.getTerminal())
                        : new PropostaTransicao(
                                s.getOrigem() == null ? null : s.getOrigem().getId(),
                                s.getDestino() == null ? null : s.getDestino().getId(),
                                s.getExigeGovernanca()),
                s.getJustificativa(),
                usuario(s.getSolicitante()),
                s.getStatus(),
                usuario(s.getResponsavelEncerramento()),
                s.getJustificativaEncerramento(),
                s.getEncerradoEm(),
                s.getCriadoEm())).toList();
    }

    private List<EntregaFila> entregas(List<EntregaProjeto> entregas) {
        Map<Long, List<ValidacaoFila>> validacoes = validacoesDe(entregas.stream().map(EntregaProjeto::getId).toList());
        return entregas.stream().map(e -> new EntregaFila(
                e.getId(),
                e.getProjeto().getId(),
                e.getProjeto().getTitulo(),
                e.getDescricao(),
                usuario(e.getAutor()),
                e.getSubmetidoEm(),
                validacoes.getOrDefault(e.getId(), List.of()))).toList();
    }

    /** Validações de cada entrega, da mais recente para a mais antiga. */
    private Map<Long, List<ValidacaoFila>> validacoesDe(Collection<Long> entregaIds) {
        if (entregaIds.isEmpty()) {
            return Map.of();
        }
        return em.createQuery("""
                        select v from ValidacaoEntrega v join fetch v.demandante
                        where v.entrega.id in :ids order by v.validadoEm desc, v.id desc""", ValidacaoEntrega.class)
                .setParameter("ids", entregaIds)
                .getResultList().stream()
                .collect(Collectors.groupingBy(v -> v.getEntrega().getId(),
                        Collectors.mapping(v -> new ValidacaoFila(
                                v.getId(),
                                v.getEntrega().getId(),
                                v.getResultado(),
                                v.getObservacoes(),
                                usuario(v.getDemandante()),
                                v.getValidadoEm()), Collectors.toList())));
    }

    private RecomendacaoFila recomendacao(RecomendacaoDemanda r) {
        return new RecomendacaoFila(
                r.getId(),
                usuario(r.getAutor()),
                r.getAutor().getPerfil(),
                usuario(r.getDestinatario()),
                r.getTitulo(),
                r.getDescricao(),
                r.getStatus(),
                r.getProjeto() == null ? null : new ProjetoReferencia(r.getProjeto().getId(), r.getProjeto().getTitulo()),
                r.getJustificativaEncerramento(),
                r.getEncerradoEm(),
                r.getCriadoEm());
    }

    private static EvidenciaFila evidencia(Evidencia e) {
        return new EvidenciaFila(
                e.getId(),
                e.getProjeto().getId(),
                e.getNomeArquivo(),
                e.getTipoArquivo(),
                e.getTamanhoBytes(),
                e.getDataUpload(),
                usuario(e.getEnviadaPor()),
                "ENVIADO");
    }

    static EtapaFila etapa(EtapaKanban etapa) {
        if (etapa == null) {
            return null;
        }
        return new EtapaFila(etapa.getId(), etapa.getCodigo(), etapa.getNome(),
                EtapaResumoResponse.cor(etapa.getCodigo()), etapa.getTerminal() != null);
    }

    private static UsuarioFila usuario(Usuario u) {
        return u == null ? null : new UsuarioFila(u.getId(), u.getNome());
    }
}
