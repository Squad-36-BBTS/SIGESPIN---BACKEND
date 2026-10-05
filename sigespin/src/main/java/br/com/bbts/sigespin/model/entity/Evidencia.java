package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evidencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Evidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "caminho_storage", nullable = false, length = 500)
    private String caminhoStorage;

    @Column(name = "tipo_arquivo", nullable = false, length = 100)
    private String tipoArquivo;

    @Column(name = "data_upload", nullable = false)
    private LocalDateTime dataUpload;

    @PrePersist
    protected void onCreate() {
        if (this.dataUpload == null) {
            this.dataUpload = LocalDateTime.now();
        }
    }
}
