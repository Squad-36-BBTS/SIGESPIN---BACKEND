package br.com.bbts.sigespin.model.entity;

import br.com.bbts.sigespin.model.enums.DecisaoTipo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliberacao_historico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DeliberacaoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisaoTipo decisao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificativa;

    @Column(name = "data_decisao", nullable = false)
    private LocalDateTime dataDecisao;

    @PrePersist
    protected void onCreate() {
        if (this.dataDecisao == null) {
            this.dataDecisao = LocalDateTime.now();
        }
    }
}
