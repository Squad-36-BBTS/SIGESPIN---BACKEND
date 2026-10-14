package br.com.bbts.sigespin.model.entity;

import br.com.bbts.sigespin.model.enums.TipoMudanca;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_transicoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class HistoricoTransicoes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mudanca", nullable = false, length = 20)
    private TipoMudanca tipoMudanca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_avanco_id")
    private SolicitacaoAvanco solicitacaoAvanco;

    @Column(name = "valor_anterior", nullable = false, length = 30)
    private String valorAnterior;

    @Column(name = "valor_novo", nullable = false, length = 30)
    private String valorNovo;

    @Column(name = "data_mudanca", nullable = false)
    private LocalDateTime dataMudanca;

    @PrePersist
    protected void onCreate() {
        if (this.dataMudanca == null) {
            this.dataMudanca = LocalDateTime.now();
        }
    }
}
