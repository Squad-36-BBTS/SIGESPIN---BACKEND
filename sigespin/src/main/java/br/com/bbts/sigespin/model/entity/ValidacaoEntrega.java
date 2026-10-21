package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import br.com.bbts.sigespin.model.enums.*;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "validacoes_entrega")
@Getter
@Setter
@NoArgsConstructor
public class ValidacaoEntrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_id", nullable = false)
    private EntregaProjeto entrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandante_id", nullable = false)
    private Usuario demandante;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false)
    private ResultadoValidacao resultado;

    @Column(name = "observacoes", columnDefinition = "TEXT", nullable = false)
    private String observacoes;

    @Column(name = "validado_em", nullable = false)
    private LocalDateTime validadoEm;
}
