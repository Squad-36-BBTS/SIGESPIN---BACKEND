package br.com.bbts.sigespin.model.entity;

import br.com.bbts.sigespin.model.enums.StatusRecomendacao;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recomendacoes_demanda")
@Getter
@Setter
@NoArgsConstructor
public class RecomendacaoDemanda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;
    @Column(nullable = false)
    private String titulo;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRecomendacao status;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", unique = true)
    private Projeto projeto;
    @Column(name = "encerrado_em")
    private LocalDateTime encerradoEm;
    @Column(name = "justificativa_encerramento", columnDefinition = "TEXT")
    private String justificativaEncerramento;
    @Version
    @Column(nullable = false)
    private Long versao;
}
