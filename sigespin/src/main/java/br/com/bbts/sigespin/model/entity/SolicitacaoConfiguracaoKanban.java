package br.com.bbts.sigespin.model.entity;

import br.com.bbts.sigespin.model.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/** Proposta histórica do Analista. O encerramento registra o resultado aplicado. */
@Entity
@Table(name = "solicitacoes_configuracao_kanban")
@Getter
@Setter
@NoArgsConstructor
public class SolicitacaoConfiguracaoKanban {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConfiguracaoKanban tipo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;
    private String codigo;
    private String nome;
    private Integer ordem;
    @Enumerated(EnumType.STRING)
    private TipoTerminal terminal;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origem_id")
    private EtapaKanban origem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id")
    private EtapaKanban destino;
    @Column(name = "exige_governanca")
    private Boolean exigeGovernanca;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificativa;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacao status;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_encerramento_id")
    private Usuario responsavelEncerramento;
    @Column(name = "justificativa_encerramento", columnDefinition = "TEXT")
    private String justificativaEncerramento;
    @Column(name = "encerrado_em")
    private LocalDateTime encerradoEm;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coluna_criada_id")
    private EtapaKanban colunaCriada;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transicao_criada_id")
    private TransicaoKanban transicaoCriada;
    @Version
    @Column(nullable = false)
    private Long versao;
}
