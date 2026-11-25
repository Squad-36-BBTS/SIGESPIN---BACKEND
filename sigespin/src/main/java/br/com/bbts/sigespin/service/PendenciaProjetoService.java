package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.entity.ConsultaEspecialista;
import br.com.bbts.sigespin.model.entity.EntregaProjeto;
import br.com.bbts.sigespin.model.entity.ParecerTecnico;
import br.com.bbts.sigespin.model.entity.QualificacaoDemanda;
import br.com.bbts.sigespin.model.entity.SolicitacaoTransicao;
import br.com.bbts.sigespin.model.entity.ValidacaoEntrega;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import br.com.bbts.sigespin.model.enums.EstadoTarefa;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calcula as pendências de cada projeto (selos "Aguardando ..." do frontend).
 *
 * <p>As pendências não são gravadas: saem dos próprios registros do projeto, então nunca ficam
 * desatualizadas. O cálculo é feito em lote (quatro consultas para qualquer quantidade de projetos),
 * para servir à Minha fila, ao portfólio e às métricas sem consultas por projeto.</p>
 */
@Service
public class PendenciaProjetoService {
    private final EntityManager entityManager;

    public PendenciaProjetoService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Pendências de cada projeto, na ordem do enum. Projetos sem pendência recebem lista vazia. */
    @Transactional(readOnly = true)
    public Map<Long, List<EstadoTarefa>> calcular(Collection<Projeto> projetos) {
        Map<Long, List<EstadoTarefa>> resultado = new LinkedHashMap<>();
        if (projetos.isEmpty()) {
            return resultado;
        }
        List<Long> ids = projetos.stream().map(Projeto::getId).distinct().toList();

        Set<Long> qualificados = idsDe("""
                select distinct q.projeto.id from QualificacaoDemanda q where q.projeto.id in :ids""", ids);
        Set<Long> comConsultaSemParecer = idsDe("""
                select distinct c.projeto.id from ConsultaEspecialista c
                where c.projeto.id in :ids
                  and not exists (select 1 from ParecerTecnico p where p.consulta = c)""", ids);
        Set<Long> comPedidoPendente = new HashSet<>(entityManager.createQuery("""
                        select distinct s.projeto.id from SolicitacaoTransicao s
                        where s.projeto.id in :ids and s.status = :pendente""", Long.class)
                .setParameter("ids", ids)
                .setParameter("pendente", StatusSolicitacao.PENDENTE)
                .getResultList());
        Set<Long> comEntregaSemValidacao = idsDe("""
                select distinct e.projeto.id from EntregaProjeto e
                where e.projeto.id in :ids
                  and not exists (select 1 from ValidacaoEntrega v where v.entrega = e)""", ids);

        for (Projeto projeto : projetos) {
            List<EstadoTarefa> pendencias = new ArrayList<>(4);
            boolean ativo = !Boolean.TRUE.equals(projeto.getArquivado());
            boolean terminal = projeto.getEtapa() != null && projeto.getEtapa().getTerminal() != null;
            if (ativo && !terminal && !qualificados.contains(projeto.getId())) {
                pendencias.add(EstadoTarefa.AGUARDANDO_TRIAGEM);
            }
            if (comConsultaSemParecer.contains(projeto.getId())) {
                pendencias.add(EstadoTarefa.AGUARDANDO_PARECER);
            }
            if (comPedidoPendente.contains(projeto.getId())) {
                pendencias.add(EstadoTarefa.AGUARDANDO_GOVERNANCA);
            }
            if (comEntregaSemValidacao.contains(projeto.getId())) {
                pendencias.add(EstadoTarefa.AGUARDANDO_VALIDACAO);
            }
            resultado.put(projeto.getId(), List.copyOf(pendencias));
        }
        return resultado;
    }

    /**
     * Mesmas regras de {@link #calcular}, escritas como condição de consulta, para filtrar o portfólio
     * por pendência sem carregar os projetos em memória.
     */
    public static Specification<Projeto> com(EstadoTarefa estado) {
        return (raiz, consulta, cb) -> switch (estado) {
            case AGUARDANDO_TRIAGEM -> {
                Subquery<Long> qualificacao = consulta.subquery(Long.class);
                Root<QualificacaoDemanda> q = qualificacao.from(QualificacaoDemanda.class);
                qualificacao.select(q.get("id")).where(cb.equal(q.get("projeto"), raiz));
                yield cb.and(cb.isFalse(raiz.get("arquivado")),
                        cb.isNull(raiz.get("etapa").get("terminal")),
                        cb.not(cb.exists(qualificacao)));
            }
            case AGUARDANDO_PARECER -> {
                Subquery<Long> consultaSemParecer = consulta.subquery(Long.class);
                Root<ConsultaEspecialista> c = consultaSemParecer.from(ConsultaEspecialista.class);
                Subquery<Long> parecer = consulta.subquery(Long.class);
                Root<ParecerTecnico> pt = parecer.from(ParecerTecnico.class);
                parecer.select(pt.get("id")).where(cb.equal(pt.get("consulta"), c));
                consultaSemParecer.select(c.get("id"))
                        .where(cb.equal(c.get("projeto"), raiz), cb.not(cb.exists(parecer)));
                yield cb.exists(consultaSemParecer);
            }
            case AGUARDANDO_GOVERNANCA -> {
                Subquery<Long> pedido = consulta.subquery(Long.class);
                Root<SolicitacaoTransicao> st = pedido.from(SolicitacaoTransicao.class);
                pedido.select(st.get("id")).where(cb.equal(st.get("projeto"), raiz),
                        cb.equal(st.get("status"), StatusSolicitacao.PENDENTE));
                yield cb.exists(pedido);
            }
            case AGUARDANDO_VALIDACAO -> {
                Subquery<Long> entregaSemValidacao = consulta.subquery(Long.class);
                Root<EntregaProjeto> e = entregaSemValidacao.from(EntregaProjeto.class);
                Subquery<Long> validacao = consulta.subquery(Long.class);
                Root<ValidacaoEntrega> v = validacao.from(ValidacaoEntrega.class);
                validacao.select(v.get("id")).where(cb.equal(v.get("entrega"), e));
                entregaSemValidacao.select(e.get("id"))
                        .where(cb.equal(e.get("projeto"), raiz), cb.not(cb.exists(validacao)));
                yield cb.exists(entregaSemValidacao);
            }
        };
    }

    private Set<Long> idsDe(String jpql, List<Long> ids) {
        return new HashSet<>(entityManager.createQuery(jpql, Long.class).setParameter("ids", ids).getResultList());
    }
}
