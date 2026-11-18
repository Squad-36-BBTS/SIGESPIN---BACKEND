package br.com.bbts.sigespin.arquivo;

import br.com.bbts.sigespin.exception.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArmazenamentoS3Tests {
    private final S3Client cliente = mock(S3Client.class);
    private final ArmazenamentoS3 s3 = new ArmazenamentoS3(cliente, "bucket-privado");

    @Test void uploadUsaChaveInternaTipoValidadoESemAclPublica() throws Exception {
        s3.guardar("projetos/1/uuid.pdf", new byte[]{1,2}, "application/pdf");
        var pedido = ArgumentCaptor.forClass(PutObjectRequest.class);
        var corpo = ArgumentCaptor.forClass(RequestBody.class);
        verify(cliente).putObject(pedido.capture(), corpo.capture());
        assertEquals("bucket-privado", pedido.getValue().bucket());
        assertEquals("projetos/1/uuid.pdf", pedido.getValue().key());
        assertEquals("application/pdf", pedido.getValue().contentType());
        assertNull(pedido.getValue().acl());
        try (var entrada = corpo.getValue().contentStreamProvider().newStream()) {
            assertArrayEquals(new byte[]{1,2}, entrada.readAllBytes());
        }
    }
    @Test void downloadAbreStreamERemoveApenasChaveInformada() throws Exception {
        when(cliente.getObject(any(GetObjectRequest.class))).thenReturn(new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength(2L).build(), new ByteArrayInputStream(new byte[]{1,2})));
        try (var entrada = s3.abrir("prova.pdf").getInputStream()) {
            assertArrayEquals(new byte[]{1,2}, entrada.readAllBytes());
        }
        s3.remover("prova.pdf");
        verify(cliente).deleteObject(DeleteObjectRequest.builder().bucket("bucket-privado").key("prova.pdf").build());
    }
    @Test void arquivoInexistenteRetornaNaoEncontradoEFalhaDoProvedorIndisponivel() {
        when(cliente.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().message("missing").build());
        assertThrows(RecursoNaoEncontradoException.class, () -> s3.abrir("ausente.pdf"));
        when(cliente.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(S3Exception.builder().statusCode(503).build());
        assertThrows(ArmazenamentoIndisponivelException.class, () -> s3.guardar("prova.pdf", new byte[]{1}, "application/pdf"));
    }
}
