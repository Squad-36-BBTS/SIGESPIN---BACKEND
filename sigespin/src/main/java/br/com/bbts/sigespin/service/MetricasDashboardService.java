package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.dashboard.MetricasDashboardResponse;
import br.com.bbts.sigespin.dto.dashboard.MetricasDashboardResponse.ContagemEtapa;
import br.com.bbts.sigespin.dto.dashboard.MetricasDashboardResponse.ScoresRadar;
import br.com.bbts.sigespin.model.entity.AvaliacaoRadar;
import br.com.bbts.sigespin.model.entity.EtapaKanban;
import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.EstadoTarefa;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import br.com.bbts.sigespin.model.enums.TipoLeituraRadar;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Métricas do painel (BE-13), visíveis a todos os perfis (o portfólio é público para quem está logado).
 *
 * <p>Recorte: projetos registrados nos últimos {@code periodoDias} dias (opcional), numa etapa (opcional) e com
 * uma origem (opcional). Os indicadores usam os projetos ativos do recorte; os arquivados só são contados.</p>
 *
 * <p>Radar: a média inicial usa todas as leituras INICIAL dos projetos; a média atual usa, por projeto, a leitura
 * ATUAL mais recente ou, se não houver, a leitura mais recente de qualquer tipo. Regras iguais às do protótipo.</p>
 */
@Service
public class MetricasDashboardService {
    static final int PERIODO_MAXIMO_DIAS = 3650;

    private final EntityManager em;
    private final UsuarioAtualService usuarioAtual;
    private final PendenciaProjetoService pendencias;

    public MetricasDashboardService(EntityManager em, UsuarioAtualService usuarioAtual, PendenciaProjetoService pendencias) {
        this.em = em;
        this.usuarioAtual = usuarioAtual;
        this.pendencias = pendencias;
    }

    @Transactional(readOnly = true)
    public MetricasDashboardResponse calcular(Integer periodoDias, Long etapaId, OrigemDemanda origem) {
        usuarioAtual.obterAtivo();
        if (periodoDias != null && (periodoDias < 1 || periodoDias > PERIODO_MAXIMO_DIAS)) {
            throw new IllegalArgumentException("O período deve ficar entre 1 e " + PERIODO_MAXIMO_DIAS + " dias.");
        }

        List<Projeto> recorte = projetosDoRecorte(periodoDias, etapaId, origem);
        List<Projeto> ativos = recorte.stream().filter(p -> !Boolean.TRUE.equals(p.getArquivado())).toList();

        return new MetricasDashboardResponse(
                ativos.size(),
                recorte.size() - ativos.size(),
                media(ativos.stream().map(Projeto::getTrlAtual).filter(Objects::nonNull)
                        .map(BigDecimal::valueOf).toList()),
                porEtapa(ativos),
                contarPor(ativos, Projeto::getOrigem, OrigemDemanda.class),
                contarPor(ativos, Projeto::getArena, ArenaTipo.class),
                contarPendencias(ativos),
                radarInicial(ativos),
                radarAtual(ativos));
    }

    private List<Projeto> projetosDoRecorte(Integer periodoDias, Long etapaId, OrigemDemanda origem) {
        StringBuilder jpql = new StringBuilder("select p from Projeto p join fetch p.etapa where 1 = 1");
        if (periodoDias != null) jpql.append(" and p.criadoEm >= :limite");
        if (etapaId != null) jpql.append(" and p.etapa.id = :etapa");
        if (origem != null) jpql.append(" and p.origem = :origem");
        var consulta = em.createQuery(jpql.toString(), Projeto.class);
        if (periodoDias != null) consulta.setParameter("limite", LocalDateTime.now().minusDays(periodoDias));
        if (etapaId != null) consulta.setParameter("etapa", etapaId);
        if (origem != null) consulta.setParameter("origem", origem);
        return consulta.getResultList();
    }

    /** Todas as etapas ativas, na ordem do Kanban, com a quantidade de projetos ativos em cada uma. */
    private List<ContagemEtapa> porEtapa(List<Projeto> ativos) {
        Map<Long, Long> contagem = ativos.stream()
                .collect(Collectors.groupingBy(p -> p.getEtapa().getId(), Collectors.counting()));
        return em.createQuery("select e from EtapaKanban e where e.ativa = true order by e.ordem, e.id", EtapaKanban.class)
                .getResultList().stream()
                .map(e -> new ContagemEtapa(MinhaFilaService.etapa(e), contagem.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private static <E extends Enum<E>> Map<E, Long> contarPor(List<Projeto> projetos, Function<Projeto, E> chave, Class<E> tipo) {
        Map<E, Long> contagem = new EnumMap<>(tipo);
        for (E valor : tipo.getEnumConstants()) {
            contagem.put(valor, 0L);
        }
        projetos.stream().map(chave).filter(Objects::nonNull).forEach(v -> contagem.merge(v, 1L, Long::sum));
        return new LinkedHashMap<>(contagem);
    }

    private Map<EstadoTarefa, Long> contarPendencias(List<Projeto> ativos) {
        Map<EstadoTarefa, Long> contagem = new EnumMap<>(EstadoTarefa.class);
        for (EstadoTarefa estado : EstadoTarefa.values()) {
            contagem.put(estado, 0L);
        }
        pendencias.calcular(ativos).values().forEach(lista -> lista.forEach(e -> contagem.merge(e, 1L, Long::sum)));
        return new LinkedHashMap<>(contagem);
    }

    private ScoresRadar radarInicial(List<Projeto> ativos) {
        return mediaRadar(leiturasDe(ativos).stream()
                .filter(r -> r.getTipoLeitura() == TipoLeituraRadar.INICIAL).toList());
    }

    private ScoresRadar radarAtual(List<Projeto> ativos) {
        Map<Long, List<AvaliacaoRadar>> porProjeto = leiturasDe(ativos).stream()
                .collect(Collectors.groupingBy(r -> r.getProjeto().getId(), LinkedHashMap::new, Collectors.toList()));
        List<AvaliacaoRadar> atuais = new ArrayList<>();
        // As leituras já vêm da mais recente para a mais antiga.
        porProjeto.values().forEach(leituras -> atuais.add(leituras.stream()
                .filter(r -> r.getTipoLeitura() == TipoLeituraRadar.ATUAL)
                .findFirst()
                .orElse(leituras.get(0))));
        return mediaRadar(atuais);
    }

    private List<AvaliacaoRadar> leiturasDe(Collection<Projeto> projetos) {
        if (projetos.isEmpty()) {
            return List.of();
        }
        return em.createQuery("""
                        select r from AvaliacaoRadar r
                        where r.projeto.id in :ids
                        order by r.dataAvaliacao desc nulls last, r.id desc""", AvaliacaoRadar.class)
                .setParameter("ids", projetos.stream().map(Projeto::getId).toList())
                .getResultList();
    }

    private static ScoresRadar mediaRadar(List<AvaliacaoRadar> leituras) {
        return new ScoresRadar(
                media(leituras.stream().map(AvaliacaoRadar::getScoreNegocio).toList()),
                media(leituras.stream().map(AvaliacaoRadar::getScoreProduto).toList()),
                media(leituras.stream().map(AvaliacaoRadar::getScoreTime).toList()),
                media(leituras.stream().map(AvaliacaoRadar::getScoreReceita).toList()));
    }

    /** Média com duas casas decimais; zero quando não há valores. */
    private static double media(List<BigDecimal> valores) {
        List<BigDecimal> presentes = valores.stream().filter(Objects::nonNull).toList();
        if (presentes.isEmpty()) {
            return 0.0;
        }
        BigDecimal soma = presentes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(BigDecimal.valueOf(presentes.size()), 2, RoundingMode.HALF_UP).doubleValue();
    }
}
