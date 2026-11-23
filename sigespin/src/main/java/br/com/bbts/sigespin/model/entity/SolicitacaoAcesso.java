package br.com.bbts.sigespin.model.entity;

import br.com.bbts.sigespin.model.enums.StatusSolicitacaoAcesso;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Pedido de acesso de alguém que ainda não é usuário. Gravado por INSERT nativo no repository. */
@Entity
@Table(name = "solicitacoes_acesso")
@Getter
@Setter
@NoArgsConstructor
public class SolicitacaoAcesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacaoAcesso status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
