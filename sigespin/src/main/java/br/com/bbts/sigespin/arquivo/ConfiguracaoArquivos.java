package br.com.bbts.sigespin.arquivo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

/**
 * Cria o cliente S3 quando {@code app.arquivos.tipo=S3}.
 *
 * <p>Sem endpoint, o cliente fala com o AWS S3. Com endpoint, fala com qualquer serviço compatível com o
 * protocolo S3, como o Supabase Storage, que é o destino escolhido para os comprovantes em produção.
 * O Supabase exige endereçamento por caminho ({@code path-style}): {@code endpoint/bucket/chave}.</p>
 *
 * <p>As credenciais vêm da cadeia padrão do SDK ({@code AWS_ACCESS_KEY_ID} e {@code AWS_SECRET_ACCESS_KEY}
 * nas variáveis de ambiente) e nunca do código ou do Git.</p>
 */
@Configuration
public class ConfiguracaoArquivos {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.arquivos.tipo", havingValue = "S3")
    S3Client s3Client(
            @Value("${app.arquivos.s3.region}") String regiao,
            @Value("${app.arquivos.s3.endpoint:}") String endpoint,
            @Value("${app.arquivos.s3.path-style:false}") boolean pathStyle) {
        return construtor(regiao, endpoint, pathStyle).build();
    }

    /** Separado do bean para que os testes verifiquem a configuração sem abrir conexão. */
    static S3ClientBuilder construtor(String regiao, String endpoint, boolean pathStyle) {
        S3ClientBuilder construtor = S3Client.builder()
                .region(Region.of(regiao))
                .forcePathStyle(pathStyle);
        if (endpoint != null && !endpoint.isBlank()) {
            URI uri = URI.create(endpoint.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("O endpoint de arquivos deve usar HTTPS.");
            }
            construtor.endpointOverride(uri);
        }
        return construtor;
    }
}
