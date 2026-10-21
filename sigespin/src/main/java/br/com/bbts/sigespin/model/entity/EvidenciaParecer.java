package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "evidencias_parecer")
@Getter
@Setter
@NoArgsConstructor
public class EvidenciaParecer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parecer_id", nullable = false)
    private ParecerTecnico registro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidencia_id", nullable = false)
    private Evidencia evidencia;
}
