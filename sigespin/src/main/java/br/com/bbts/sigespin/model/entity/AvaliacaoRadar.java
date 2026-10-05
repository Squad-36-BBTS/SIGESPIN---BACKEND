package br.com.bbts.sigespin.model.entity;

import br.com.bbts.sigespin.model.enums.TipoLeituraRadar;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacao_radar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AvaliacaoRadar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_leitura", nullable = false, length = 10)
    private TipoLeituraRadar tipoLeitura;

    @Column(name = "score_negocio", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreNegocio;

    @Column(name = "score_produto", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreProduto;

    @Column(name = "score_time", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreTime;

    @Column(name = "score_receita", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreReceita;

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDateTime dataAvaliacao;

    @PrePersist
    protected void onCreate() {
        if (this.dataAvaliacao == null) {
            this.dataAvaliacao = LocalDateTime.now();
        }
    }
}
