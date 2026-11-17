package br.com.bbts.sigespin.model.entity;

import br.com.bbts.sigespin.model.enums.TipoAlteracaoProjeto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "alteracoes_projeto")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class AlteracaoProjeto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlteracaoProjeto tipo;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificativa;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> anterior;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> atual;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
