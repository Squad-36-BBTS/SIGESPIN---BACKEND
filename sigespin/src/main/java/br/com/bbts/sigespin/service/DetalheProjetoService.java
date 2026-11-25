package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.projeto.EtapaResumoResponse;
import br.com.bbts.sigespin.dto.projeto.ProjetoDetalheCompletoResponse;
import br.com.bbts.sigespin.dto.projeto.ProjetoDetalheCompletoResponse.*;
import br.com.bbts.sigespin.dto.projeto.ProjetoResponse;
import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.model.entity.AlteracaoProjeto;
import br.com.bbts.sigespin.model.entity.ConsultaEspecialista;
import br.com.bbts.sigespin.model.entity.CorrecaoDemanda;
import br.com.bbts.sigespin.model.entity.EntregaProjeto;
import br.com.bbts.sigespin.model.entity.InteracaoProjeto;
import br.com.bbts.sigespin.model.entity.MovimentacaoKanban;
import br.com.bbts.sigespin.model.entity.ParecerTecnico;
import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.entity.QualificacaoDemanda;
import br.com.bbts.sigespin.model.entity.RecomendacaoDemanda;
import br.com.bbts.sigespin.model.entity.SolicitacaoTransicao;
import br.com.bbts.sigespin.model.entity.TransicaoKanban;
import br.com.bbts.sigespin.model.entity.ValidacaoEntrega;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta o detalhe completo do projeto (tela de detalhe do frontend) com uma consulta por tipo de registro,
 * sempre trazendo as pessoas na mesma consulta. Somente leitura.
 */
@Service
public class DetalheProjetoService {
    private final EntityManager em;
    private final UsuarioAtualService usuarioAtual;
    private final PendenciaProjetoService pendencias;

    public DetalheProjetoService(EntityManager em, UsuarioAtualService usuarioAtual, PendenciaProjetoService pendencias) {
        this.em = em;
        this.usuarioAtual = usuarioAtual;
        this.pendencias = pendencias;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ProjetoDetalheCompletoResponse detalhar(Long id) {
        usuarioAtual.obterAtivo();
        Projeto p = em.createQuery("""
                        select p from Projeto p
                        left join fetch p.etapa left join fetch p.demandante left join fetch p.analistaResponsavel
                        left join fetch p.responsavelPo left join fetch p.acaoMobilizacao left join fetch p.arquivadoPor
                        where p.id = :id""", Projeto.class)
                .setParameter("id", id).getResultStream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado."));

        Map<Long, List<Evidencia>> evidenciasParecer = evidenciasPorRegistro("EvidenciaParecer", id);
        Map<Long, List<Evidencia>> evidenciasTrl = evidenciasPorRegistro("EvidenciaTrl", id);
        Map<Long, List<Evidencia>> evidenciasPedido = evidenciasPorRegistro("EvidenciaTransicao", id);

        List<ParecerTecnico> pareceresDoProjeto = lista("""
                select pt from ParecerTecnico pt join fetch pt.autor left join fetch pt.consulta
                where pt.projeto.id = :id order by pt.criadoEm, pt.id""", ParecerTecnico.class, id);
        Map<Long, ParecerTecnico> parecerPorConsulta = pareceresDoProjeto.stream()
                .filter(pt -> pt.getConsulta() != null)
                .collect(Collectors.toMap(pt -> pt.getConsulta().getId(), Function.identity(), (a, b) -> a));

        List<ValidacaoEntrega> validacoes = lista("""
                select v from ValidacaoEntrega v join fetch v.demandante join fetch v.entrega
                where v.projeto.id = :id order by v.validadoEm, v.id""", ValidacaoEntrega.class, id);
        Map<Long, List<Validacao>> validacoesPorEntrega = validacoes.stream().collect(Collectors.groupingBy(
                v -> v.getEntrega().getId(),
                Collectors.mapping(v -> new Validacao(v.getId(), v.getEntrega().getId(), v.getResultado(),
                        v.getObservacoes(), u(v.getDemandante()), v.getValidadoEm()), Collectors.toList())));

        List<Evidencia> todasEvidencias = em.createQuery("""
                        select e from Evidencia e left join fetch e.enviadaPor
                        where e.projeto.id = :id order by e.dataUpload, e.id""", br.com.bbts.sigespin.model.entity.Evidencia.class)
                .setParameter("id", id).getResultList().stream().map(DetalheProjetoService::evidencia).toList();
        Map<Long, Evidencia> evidenciaPorId = todasEvidencias.stream()
                .collect(Collectors.toMap(Evidencia::id, Function.identity()));

        return new ProjetoDetalheCompletoResponse(
                ProjetoResponse.de(p, pendencias.calcular(List.of(p)).get(p.getId())),
                p.getAcaoMobilizacao() == null ? null : p.getAcaoMobilizacao().getId(),
                p.getArquivadoPor() == null ? null : p.getArquivadoPor().getId(),
                lista("select r from RecomendacaoDemanda r where r.projeto.id = :id order by r.id",
                        RecomendacaoDemanda.class, id).stream().findFirst()
                        .map(r -> new RecomendacaoOrigem(r.getId(), r.getTitulo())).orElse(null),
                lista("""
                        select q from QualificacaoDemanda q join fetch q.autor
                        where q.projeto.id = :id order by q.criadoEm, q.id""", QualificacaoDemanda.class, id).stream()
                        .map(q -> new Qualificacao(q.getId(), id, u(q.getAutor()), q.getAderenciaTese(),
                                q.getRelevancia(), q.getGrauInovacao(), q.getViabilidade(),
                                q.getMaturidadeTecnologica(), q.getFundamentacao(), q.getConclusao(), q.getCriadoEm()))
                        .toList(),
                lista("""
                        select c from ConsultaEspecialista c join fetch c.solicitante join fetch c.especialista
                        where c.projeto.id = :id order by c.criadoEm, c.id""", ConsultaEspecialista.class, id).stream()
                        .map(c -> {
                            ParecerTecnico resposta = parecerPorConsulta.get(c.getId());
                            return new Consulta(c.getId(), id, u(c.getSolicitante()), u(c.getEspecialista()), null,
                                    c.getQuestao(), resposta == null ? "PENDENTE" : "RESPONDIDA",
                                    resposta == null ? null : resposta.getId(), c.getCriadoEm(),
                                    resposta == null ? null : resposta.getCriadoEm());
                        }).toList(),
                pareceresDoProjeto.stream()
                        .map(pt -> new Parecer(pt.getId(), id,
                                pt.getConsulta() == null ? null : pt.getConsulta().getId(),
                                pt.getConsulta() == null ? null : pt.getConsulta().getQuestao(),
                                u(pt.getAutor()), pt.getFundamentacao(), pt.getConclusao(),
                                evidenciasParecer.getOrDefault(pt.getId(), List.of()), pt.getCriadoEm()))
                        .toList(),
                lista("""
                        select a from AvaliacaoTrl a join fetch a.autor
                        where a.projeto.id = :id order by a.avaliadoEm, a.id""",
                        br.com.bbts.sigespin.model.entity.AvaliacaoTrl.class, id).stream()
                        .map(a -> new AvaliacaoTrl(a.getId(), id, u(a.getAutor()), a.getNivelAnterior(), a.getNivel(),
                                a.getJustificativa(), evidenciasTrl.getOrDefault(a.getId(), List.of()), null,
                                a.getAvaliadoEm()))
                        .toList(),
                lista("""
                        select r from AvaliacaoRadar r
                        where r.projeto.id = :id order by r.dataAvaliacao, r.id""",
                        br.com.bbts.sigespin.model.entity.AvaliacaoRadar.class, id).stream()
                        .map(r -> new AvaliacaoRadar(r.getId(), id, r.getTipoLeitura(), r.getScoreNegocio(),
                                r.getScoreProduto(), r.getScoreTime(), r.getScoreReceita(), r.getDataAvaliacao()))
                        .toList(),
                lista("""
                        select s from SolicitacaoTransicao s join fetch s.solicitante join fetch s.origem
                        join fetch s.destino left join fetch s.responsavelEncerramento
                        where s.projeto.id = :id order by s.criadoEm, s.id""", SolicitacaoTransicao.class, id).stream()
                        .map(s -> new Solicitacao(s.getId(), id,
                                s.getTransicao() == null ? null : s.getTransicao().getId(),
                                EtapaResumoResponse.de(s.getOrigem()), EtapaResumoResponse.de(s.getDestino()),
                                u(s.getSolicitante()), s.getJustificativa(),
                                evidenciasPedido.getOrDefault(s.getId(), List.of()), s.getStatus(),
                                u(s.getResponsavelEncerramento()), s.getJustificativaEncerramento(),
                                s.getEncerradoEm(), s.getCriadoEm()))
                        .toList(),
                lista("""
                        select e from EntregaProjeto e join fetch e.autor
                        where e.projeto.id = :id order by e.submetidoEm, e.id""", EntregaProjeto.class, id).stream()
                        .map(e -> new Entrega(e.getId(), id, e.getDescricao(), u(e.getAutor()), e.getSubmetidoEm(),
                                validacoesPorEntrega.getOrDefault(e.getId(), List.of())))
                        .toList(),
                lista("""
                        select c from CorrecaoDemanda c join fetch c.autor
                        where c.projeto.id = :id order by c.criadoEm, c.id""", CorrecaoDemanda.class, id).stream()
                        .map(c -> new Correcao(c.getId(), id, c.getDescricao(), c.getReferencia(),
                                c.getEvidencia() == null ? null : evidenciaPorId.get(c.getEvidencia().getId()),
                                u(c.getAutor()), c.getCriadoEm()))
                        .toList(),
                todasEvidencias,
                historicoTransicoes(id),
                lista("""
                        select a from AlteracaoProjeto a join fetch a.autor
                        where a.projeto.id = :id order by a.criadoEm, a.id""", AlteracaoProjeto.class, id).stream()
                        .map(a -> new HistoricoGestao(a.getId(), id, a.getTipo(), u(a.getAutor()), a.getJustificativa(),
                                alteracoes(a.getAnterior(), a.getAtual()), a.getCriadoEm()))
                        .toList(),
                lista("""
                        select i from InteracaoProjeto i join fetch i.autor
                        where i.projeto.id = :id order by i.criadoEm, i.id""", InteracaoProjeto.class, id).stream()
                        .map(i -> new Interacao(i.getId(), id, u(i.getAutor()), i.getAutor().getPerfil(), i.getTexto(),
                                i.getCriadoEm()))
                        .toList(),
                transicoesDisponiveis(p));
    }

    private List<HistoricoTransicao> historicoTransicoes(Long id) {
        List<HistoricoTransicao> historico = new ArrayList<>();
        lista("""
                select m from MovimentacaoKanban m join fetch m.autor join fetch m.origem join fetch m.destino
                left join fetch m.solicitacao where m.projeto.id = :id""", MovimentacaoKanban.class, id)
                .forEach(m -> historico.add(new HistoricoTransicao(m.getId() * 2, id, u(m.getAutor()), "ETAPA",
                        m.getOrigem().getNome(), m.getDestino().getNome(), m.getJustificativa(),
                        m.getSolicitacao() == null ? null : m.getSolicitacao().getId(), m.getMovimentadoEm())));
        lista("""
                select a from AvaliacaoTrl a join fetch a.autor where a.projeto.id = :id""",
                br.com.bbts.sigespin.model.entity.AvaliacaoTrl.class, id)
                .forEach(a -> historico.add(new HistoricoTransicao(a.getId() * 2 + 1, id, u(a.getAutor()), "TRL",
                        String.valueOf(a.getNivelAnterior()), String.valueOf(a.getNivel()), a.getJustificativa(),
                        null, a.getAvaliadoEm())));
        historico.sort(Comparator.comparing(HistoricoTransicao::dataMudanca,
                Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(HistoricoTransicao::id));
        return historico;
    }

    /** Transições ativas a partir da etapa atual. Projeto arquivado ou em etapa terminal não tem nenhuma. */
    private List<TransicaoDisponivel> transicoesDisponiveis(Projeto p) {
        if (Boolean.TRUE.equals(p.getArquivado()) || p.getEtapa() == null || p.getEtapa().getTerminal() != null) {
            return List.of();
        }
        return em.createQuery("""
                        select t from TransicaoKanban t join fetch t.destino
                        where t.origem.id = :etapa and t.ativa = true and t.destino.ativa = true
                        order by t.destino.ordem, t.id""", TransicaoKanban.class)
                .setParameter("etapa", p.getEtapa().getId()).getResultList().stream()
                .map(t -> new TransicaoDisponivel(t.getId(), p.getEtapa().getId(), t.getDestino().getId(),
                        Boolean.TRUE.equals(t.getExigeGovernanca()), true, t.getVersao(),
                        EtapaResumoResponse.de(t.getDestino())))
                .toList();
    }

    /** Evidências ligadas a pareceres, avaliações de TRL ou pedidos de avanço, agrupadas pelo id do registro. */
    private Map<Long, List<Evidencia>> evidenciasPorRegistro(String entidade, Long id) {
        List<Object[]> linhas = em.createQuery("select v.registro.id, e from " + entidade + " v join v.evidencia e "
                        + "left join fetch e.enviadaPor where v.projeto.id = :id order by e.id", Object[].class)
                .setParameter("id", id).getResultList();
        return linhas.stream().collect(Collectors.groupingBy(l -> (Long) l[0], Collectors.mapping(
                l -> evidencia((br.com.bbts.sigespin.model.entity.Evidencia) l[1]), Collectors.toList())));
    }

    private static Evidencia evidencia(br.com.bbts.sigespin.model.entity.Evidencia e) {
        return new Evidencia(e.getId(), e.getProjeto().getId(), e.getNomeArquivo(), e.getTipoArquivo(),
                e.getTamanhoBytes(), e.getDataUpload(), u(e.getEnviadaPor()), "ENVIADO",
                "/api/projetos/" + e.getProjeto().getId() + "/evidencias/" + e.getId() + "/conteudo");
    }

    private static List<AlteracaoCampo> alteracoes(Map<String, Object> anterior, Map<String, Object> atual) {
        Map<String, Object> antes = anterior == null ? Map.of() : anterior;
        Map<String, Object> depois = atual == null ? Map.of() : atual;
        Set<String> campos = new LinkedHashSet<>(antes.keySet());
        campos.addAll(depois.keySet());
        return campos.stream()
                .filter(c -> !Objects.equals(antes.get(c), depois.get(c)))
                .map(c -> new AlteracaoCampo(c, texto(antes.get(c)), texto(depois.get(c))))
                .toList();
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    private <T> List<T> lista(String jpql, Class<T> tipo, Long id) {
        return em.createQuery(jpql, tipo).setParameter("id", id).getResultList();
    }

    private static UsuarioResumoResponse u(br.com.bbts.sigespin.model.entity.Usuario usuario) {
        return UsuarioResumoResponse.de(usuario);
    }
}
