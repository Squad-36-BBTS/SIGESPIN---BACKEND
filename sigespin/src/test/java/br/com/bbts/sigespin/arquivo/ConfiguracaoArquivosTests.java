package br.com.bbts.sigespin.arquivo;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica, sem acessar a rede, como o cliente S3 monta os endereços.
 * Credenciais fictícias evitam depender das variáveis de ambiente da máquina.
 */
class ConfiguracaoArquivosTests {
    private static final StaticCredentialsProvider CREDENCIAIS_FICTICIAS =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("teste", "teste"));

    @Test
    void supabaseUsaEndpointProprioComEnderecoPorCaminho() {
        String endpoint = "https://exemplo.storage.supabase.co/storage/v1/s3";
        try (var cliente = ConfiguracaoArquivos.construtor("sa-east-1", endpoint, true)
                .credentialsProvider(CREDENCIAIS_FICTICIAS).build()) {
            assertEquals(URI.create(endpoint), cliente.serviceClientConfiguration().endpointOverride().orElseThrow());
            var url = cliente.utilities().getUrl(GetUrlRequest.builder().bucket("comprovantes").key("projetos/1/a.pdf").build());
            assertEquals("https://exemplo.storage.supabase.co/storage/v1/s3/comprovantes/projetos/1/a.pdf", url.toString());
        }
    }

    @Test
    void semEndpointContinuaUsandoAwsS3() {
        try (var cliente = ConfiguracaoArquivos.construtor("us-east-1", "", false)
                .credentialsProvider(CREDENCIAIS_FICTICIAS).build()) {
            assertTrue(cliente.serviceClientConfiguration().endpointOverride().isEmpty());
            var url = cliente.utilities().getUrl(GetUrlRequest.builder().bucket("comprovantes").key("a.pdf").build());
            assertTrue(url.getHost().endsWith("amazonaws.com"), url.toString());
        }
    }

    @Test
    void endpointSemHttpsERecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfiguracaoArquivos.construtor("sa-east-1", "http://inseguro.exemplo/storage/v1/s3", true));
    }
}
