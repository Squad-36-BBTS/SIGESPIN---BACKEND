package br.com.bbts.sigespin.model.entity;

import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.StatusFunil;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ArenaTipo arena;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_funil", nullable = false, length = 20)
    private StatusFunil statusFunil = StatusFunil.IDEACAO;

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


    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliberacaoHistorico> deliberacoes = new ArrayList<>();

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvaliacaoRadar> avaliacoesRadar = new ArrayList<>();

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoricoTransicoes> historicoTransicoes = new ArrayList<>();

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evidencia> evidencias = new ArrayList<>();


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
