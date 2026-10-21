package br.com.bbts.sigespin.model.entity;

import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projetos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(name = "descricao_dor", nullable = false, columnDefinition = "TEXT")
    private String descricaoDor;

    @Column(name = "area_demandante_id", nullable = false)
    private Long areaDemandanteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_po_id", nullable = false)
    private Usuario responsavelPo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandante_id")
    private Usuario demandante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analista_responsavel_id")
    private Usuario analistaResponsavel;

    @Version
    @Column(nullable = false)
    private Long versao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ArenaTipo arena;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id", nullable = false)
    private EtapaKanban etapa;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrigemDemanda origem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acao_mobilizacao_id")
    private AcaoMobilizacao acaoMobilizacao;

    @Column(name = "trl_atual", nullable = false)
    private Integer trlAtual;

    @Column(name = "hipotese_solucao", columnDefinition = "TEXT")
    private String hipoteseSolucao;

    @Column(name = "potencial_lei_do_bem")
    private Boolean potencialLeiDoBem;

    @Column(name = "dispendio_estimado", precision = 12, scale = 2)
    private BigDecimal dispendioEstimado;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;


    @OneToMany(mappedBy = "projeto")
    private List<DeliberacaoHistorico> deliberacoes = new ArrayList<>();

    @OneToMany(mappedBy = "projeto")
    private List<AvaliacaoRadar> avaliacoesRadar = new ArrayList<>();

    @OneToMany(mappedBy = "projeto")
    private List<HistoricoTransicoes> historicoTransicoes = new ArrayList<>();

    @OneToMany(mappedBy = "projeto")
    private List<Evidencia> evidencias = new ArrayList<>();

    @OneToMany(mappedBy = "projeto")
    private List<SolicitacaoAvanco> solicitacoesAvanco = new ArrayList<>();


    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
