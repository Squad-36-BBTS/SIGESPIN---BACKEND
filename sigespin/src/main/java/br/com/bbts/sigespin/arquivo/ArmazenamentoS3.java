package br.com.bbts.sigespin.arquivo;

import br.com.bbts.sigespin.exception.ArmazenamentoIndisponivelException;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Component
@ConditionalOnProperty(name = "app.arquivos.tipo", havingValue = "S3")
public class ArmazenamentoS3 implements ArmazenamentoArquivo {
    private final S3Client cliente;
    private final String bucket;

    public ArmazenamentoS3(S3Client cliente, @Value("${app.arquivos.s3.bucket}") String bucket) {
        if (bucket.isBlank()) throw new IllegalArgumentException("Configure o bucket de arquivos.");
        this.cliente = cliente;
        this.bucket = bucket;
    }

    public String tipo() { return "S3"; }

    public void guardar(String chave, byte[] conteudo, String contentType) {
        try {
            cliente.putObject(PutObjectRequest.builder().bucket(bucket).key(chave).contentType(contentType).build(),
                    RequestBody.fromBytes(conteudo));
        } catch (RuntimeException e) { throw new ArmazenamentoIndisponivelException(e); }
    }

    public Resource abrir(String chave) {
        try {
            return new InputStreamResource(cliente.getObject(GetObjectRequest.builder().bucket(bucket).key(chave).build()));
        } catch (NoSuchKeyException e) {
            throw new RecursoNaoEncontradoException("Conteúdo do arquivo não encontrado.");
        } catch (RuntimeException e) { throw new ArmazenamentoIndisponivelException(e); }
    }

    public void remover(String chave) {
        try { cliente.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(chave).build()); }
        catch (RuntimeException e) { throw new ArmazenamentoIndisponivelException(e); }
    }
}
