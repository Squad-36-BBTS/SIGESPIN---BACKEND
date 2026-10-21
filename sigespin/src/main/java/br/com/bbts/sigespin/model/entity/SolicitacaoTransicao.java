package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import br.com.bbts.sigespin.model.enums.*;

@Entity
@Table(name = "solicitacoes_transicao")
@Getter
@Setter
@NoArgsConstructor
public class SolicitacaoTransicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transicao_id", nullable = false)
    private TransicaoKanban transicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origem_id", nullable = false)
    private EtapaKanban origem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id", nullable = false)
    private EtapaKanban destino;

    @Column(name = "exige_governanca", nullable = false)
    private Boolean exigeGovernanca;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusSolicitacao status;

    @Column(name = "justificativa", columnDefinition = "TEXT", nullable = false)
    private String justificativa;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_encerramento_id")
    private Usuario responsavelEncerramento;

    @Column(name = "justificativa_encerramento", columnDefinition = "TEXT")
    private String justificativaEncerramento;

    @Column(name = "encerrado_em")
    private LocalDateTime encerradoEm;

    @Version
    @Column(nullable = false)
    private Long versao;
}
