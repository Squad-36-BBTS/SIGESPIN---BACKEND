package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity
@Table(name = "correcoes_demanda")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class CorrecaoDemanda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;
    @Column(length = 500)
    private String referencia;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidencia_id")
    private Evidencia evidencia;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
