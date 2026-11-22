package br.com.bbts.sigespin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/** BE-16: contrato HTTP do 429. Contexto próprio, com limite baixo, para não interferir nos demais testes. */
@SpringBootTest(properties = {
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes",
        "app.solicitacoes-acesso.limite-global-minuto=3"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class SolicitacaoAcessoLimiteHttpTests {
    static final String SCHEMA = "acesso_limite_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        registro.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registro.add("spring.flyway.schemas", () -> SCHEMA);
        registro.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @AfterAll
    void removerSchemaDescartavel() throws Exception {
        try (var conexao = DriverManager.getConnection(System.getenv("SIGESPIN_TEST_DB_URL"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_PASSWORD", ""));
             var statement = conexao.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Test
    void excessoRetorna429ComRetryAfter() throws Exception {
        // Com limite 3, sete envios cruzam no máximo uma virada de minuto e sempre geram um 429.
        org.springframework.mock.web.MockHttpServletResponse bloqueada = null;
        for (int i = 0; i < 7 && bloqueada == null; i++) {
            var corpo = Map.of("nome", "Pessoa " + i, "email", UUID.randomUUID() + "@teste.local", "justificativa", "Motivo");
            var resposta = mvc.perform(request(HttpMethod.POST, "/api/auth/solicitacoes-acesso")
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(corpo))).andReturn().getResponse();
            if (resposta.getStatus() == 429) bloqueada = resposta;
            else assertEquals(202, resposta.getStatus());
        }
        assertNotNull(bloqueada, "esperava um 429");
        long espera = Long.parseLong(bloqueada.getHeader("Retry-After"));
        assertTrue(espera >= 1 && espera <= 60);
        assertEquals("LIMITE_SOLICITACOES", json.readTree(bloqueada.getContentAsByteArray()).path("codigo").asText());
    }
}
