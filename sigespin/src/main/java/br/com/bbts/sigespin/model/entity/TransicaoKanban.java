package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transicoes_kanban")
@Getter
@Setter
@NoArgsConstructor
public class TransicaoKanban {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origem_id", nullable = false)
    private EtapaKanban origem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id", nullable = false)
    private EtapaKanban destino;

    @Column(name = "exige_governanca", nullable = false)
    private Boolean exigeGovernanca;

    @Column(name = "ativa", nullable = false)
    private Boolean ativa;

    @Version
    @Column(nullable = false)
    private Long versao;
}
