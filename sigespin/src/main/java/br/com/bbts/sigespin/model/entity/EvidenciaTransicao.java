package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evidencias_transicao")
@Getter
@Setter
@NoArgsConstructor
public class EvidenciaTransicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoTransicao registro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidencia_id", nullable = false)
    private Evidencia evidencia;
}
