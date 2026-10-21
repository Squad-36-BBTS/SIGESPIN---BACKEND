package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "evidencias_trl")
@Getter
@Setter
@NoArgsConstructor
public class EvidenciaTrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliacao_id", nullable = false)
    private AvaliacaoTrl registro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidencia_id", nullable = false)
    private Evidencia evidencia;
}
