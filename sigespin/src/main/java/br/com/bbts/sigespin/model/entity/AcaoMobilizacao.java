package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "acoes_mobilizacao")
@Getter
@Setter
@NoArgsConstructor
public class AcaoMobilizacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", columnDefinition = "TEXT", nullable = false)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;
}
