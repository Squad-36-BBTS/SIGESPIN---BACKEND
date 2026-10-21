package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacoes_trl")
@Getter
@Setter
@NoArgsConstructor
public class AvaliacaoTrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "nivel_anterior", nullable = false)
    private Integer nivelAnterior;

    @Column(name = "nivel", nullable = false)
    private Integer nivel;

    @Column(name = "justificativa", columnDefinition = "TEXT", nullable = false)
    private String justificativa;

    @Column(name = "avaliado_em", nullable = false)
    private LocalDateTime avaliadoEm;

    @Column(name = "evidencias_fechadas", nullable = false)
    private Boolean evidenciasFechadas = false;
}
