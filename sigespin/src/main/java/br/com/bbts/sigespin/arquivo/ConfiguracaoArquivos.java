package br.com.bbts.sigespin.arquivo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class ConfiguracaoArquivos {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.arquivos.tipo", havingValue = "S3")
    S3Client s3Client(@Value("${app.arquivos.s3.region}") String regiao) {
        return S3Client.builder().region(Region.of(regiao)).build();
    }
}
