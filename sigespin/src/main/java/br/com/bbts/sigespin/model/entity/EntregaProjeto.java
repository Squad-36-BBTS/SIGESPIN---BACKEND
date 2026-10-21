package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "entregas_projeto")
@Getter
@Setter
@NoArgsConstructor
public class EntregaProjeto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "descricao", columnDefinition = "TEXT", nullable = false)
    private String descricao;

    @Column(name = "submetido_em", nullable = false)
    private LocalDateTime submetidoEm;
}
