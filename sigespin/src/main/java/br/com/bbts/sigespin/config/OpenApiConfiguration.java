package br.com.bbts.sigespin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação OpenAPI gerada a partir dos controllers. O botão "Authorize" do Swagger UI
 * recebe o token devolvido por POST /api/auth/login.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String ESQUEMA_JWT = "jwt";

    @Bean
    OpenAPI sigespinOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SIGESPIN API")
                        .version("1.0")
                        .description("Gestão do portfólio de inovação: demandas, Kanban, governança, entregas e IA. "
                                + "Erros seguem o formato { timestamp, status, codigo, mensagem, detalhes }."))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
