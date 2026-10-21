package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@org.hibernate.annotations.Immutable
@Entity
@Table(name = "qualificacoes_demanda")
@Getter
@Setter
@NoArgsConstructor
public class QualificacaoDemanda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "aderencia_tese", columnDefinition = "TEXT", nullable = false)
    private String aderenciaTese;

    @Column(name = "relevancia", columnDefinition = "TEXT", nullable = false)
    private String relevancia;

    @Column(name = "grau_inovacao", columnDefinition = "TEXT", nullable = false)
    private String grauInovacao;

    @Column(name = "viabilidade", columnDefinition = "TEXT", nullable = false)
    private String viabilidade;

    @Column(name = "maturidade_tecnologica", columnDefinition = "TEXT", nullable = false)
    private String maturidadeTecnologica;

    @Column(name = "fundamentacao", columnDefinition = "TEXT", nullable = false)
    private String fundamentacao;

    @Column(name = "conclusao", columnDefinition = "TEXT", nullable = false)
    private String conclusao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
