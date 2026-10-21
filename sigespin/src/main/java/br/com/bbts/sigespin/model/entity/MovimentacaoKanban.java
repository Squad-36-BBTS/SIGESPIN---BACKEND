package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "movimentacoes_kanban")
@Getter
@Setter
@NoArgsConstructor
public class MovimentacaoKanban {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origem_id", nullable = false)
    private EtapaKanban origem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id", nullable = false)
    private EtapaKanban destino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id")
    private SolicitacaoTransicao solicitacao;

    @Column(name = "justificativa", columnDefinition = "TEXT", nullable = false)
    private String justificativa;

    @Column(name = "movimentado_em", nullable = false)
    private LocalDateTime movimentadoEm;
}
