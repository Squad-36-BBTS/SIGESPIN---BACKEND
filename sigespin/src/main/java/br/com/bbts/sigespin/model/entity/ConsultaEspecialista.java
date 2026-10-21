package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "consultas_especialista")
@Getter
@Setter
@NoArgsConstructor
public class ConsultaEspecialista {
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
    @JoinColumn(name = "especialista_id", nullable = false)
    private Usuario especialista;

    @Column(name = "questao", columnDefinition = "TEXT", nullable = false)
    private String questao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
