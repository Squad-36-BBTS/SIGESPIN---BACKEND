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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_avanco_id")
    private SolicitacaoAvanco solicitacaoAvanco;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "caminho_storage", nullable = false, length = 500)
    private String caminhoStorage;

    @Column(name = "tipo_arquivo", nullable = false, length = 100)
    private String tipoArquivo;

    @Column(name = "data_upload", nullable = false)
    private LocalDateTime dataUpload;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enviado_por_id")
    private Usuario enviadaPor;

    @Column(name = "armazenamento", length = 16)
    private String armazenamento;

    @PrePersist
    protected void onCreate() {
        if (this.dataUpload == null) {
            this.dataUpload = LocalDateTime.now();
        }
    }
}
