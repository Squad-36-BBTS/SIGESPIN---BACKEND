package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pareceres_tecnicos")
@Getter
@Setter
@NoArgsConstructor
public class ParecerTecnico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id")
    private ConsultaEspecialista consulta;

    @Column(name = "fundamentacao", columnDefinition = "TEXT", nullable = false)
    private String fundamentacao;

    @Column(name = "conclusao", columnDefinition = "TEXT", nullable = false)
    private String conclusao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "evidencias_fechadas", nullable = false)
    private Boolean evidenciasFechadas = false;
}
