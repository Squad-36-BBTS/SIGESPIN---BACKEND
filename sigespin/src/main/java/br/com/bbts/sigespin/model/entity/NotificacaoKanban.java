package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

/** Publicação global. Todos os usuários consultam a mesma notificação persistida. */
@Entity
@Table(name = "notificacoes_kanban")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class NotificacaoKanban {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimentacao_id", nullable = false, unique = true)
    private MovimentacaoKanban movimentacao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;
}
