package br.com.bbts.sigespin;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.DriverManager;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contratos que o ambiente publicado (Render + Vercel) depende:
 * verificação de saúde pública e sem detalhes, e CORS liberado só para as origens configuradas.
 */
@SpringBootTest(properties = {
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes",
        "app.cors.allowed-origins=https://sigespin-frontend.vercel.app"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicacaoHttpTests {
    private static final String SCHEMA = "publicacao_" + UUID.randomUUID().toString().replace("-", "");
    private static final String FRONTEND = "https://sigespin-frontend.vercel.app";

    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        registro.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registro.add("spring.flyway.schemas", () -> SCHEMA);
        registro.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @AfterAll
    void encerrar() throws Exception {
        try (var conexao = DriverManager.getConnection(System.getenv("SIGESPIN_TEST_DB_URL"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_PASSWORD", ""));
             var statement = conexao.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Test
    void saudeEPublicaERespondeSoOEstado() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void outrosEndpointsDoActuatorNaoFicamExpostos() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().is(not(200)));
        mvc.perform(get("/actuator/beans")).andExpect(status().is(not(200)));
    }

    @Test
    void corsAceitaOFrontendPublicadoEExpoeCabecalhosNecessarios() throws Exception {
        mvc.perform(options("/api/projetos")
                        .header("Origin", FRONTEND)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND));

        mvc.perform(get("/actuator/health").header("Origin", FRONTEND))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Retry-After")))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Content-Disposition")));
    }

    @Test
    void corsRecusaOrigemDesconhecida() throws Exception {
        mvc.perform(options("/api/projetos")
                        .header("Origin", "https://site-qualquer.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
