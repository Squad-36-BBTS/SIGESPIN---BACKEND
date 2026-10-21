package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import br.com.bbts.sigespin.model.enums.*;

@Entity
@Table(name = "etapas_kanban")
@Getter
@Setter
@NoArgsConstructor
public class EtapaKanban {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    @Column(name = "ativa", nullable = false)
    private Boolean ativa;

    @Enumerated(EnumType.STRING)
    @Column(name = "terminal")
    private TipoTerminal terminal;

    @Version
    @Column(nullable = false)
    private Long versao;
}
