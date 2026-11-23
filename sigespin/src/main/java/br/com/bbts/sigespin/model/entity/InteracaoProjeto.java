package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Comentário registrado em um projeto; imutável após a criação. */
@org.hibernate.annotations.Immutable
@Entity
@Table(name = "interacoes_projeto")
@Getter
@Setter
@NoArgsConstructor
public class InteracaoProjeto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "texto", columnDefinition = "TEXT", nullable = false)
    private String texto;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
