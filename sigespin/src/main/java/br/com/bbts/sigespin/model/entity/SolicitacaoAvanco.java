package br.com.bbts.sigespin.model.entity;

import br.com.bbts.sigespin.model.enums.StatusFunil;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import br.com.bbts.sigespin.model.enums.TipoMudanca;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Registro exclusivamente legado V1/V2; nao participa do novo fluxo. */
@Entity
@org.hibernate.annotations.Immutable
@Table(name = "solicitacoes_avanco")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SolicitacaoAvanco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMudanca tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificativa;

    @Column(name = "trl_anterior")
    private Integer trlAnterior;

    @Column(name = "trl_pretendido")
    private Integer trlPretendido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusFunil statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pretendido", length = 20)
    private StatusFunil statusPretendido;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_encerramento_id")
    private Usuario responsavelEncerramento;

    @Column(name = "justificativa_encerramento", columnDefinition = "TEXT")
    private String justificativaEncerramento;

    @Column(name = "encerrado_em")
    private LocalDateTime encerradoEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    @OneToMany(mappedBy = "solicitacaoAvanco")
    private List<Evidencia> evidencias = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.criadoEm == null) {
            this.criadoEm = LocalDateTime.now();
        }
    }
}
