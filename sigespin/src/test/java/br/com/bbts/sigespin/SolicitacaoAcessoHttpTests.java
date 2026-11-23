package br.com.bbts.sigespin;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.seguranca.SolicitacaoAcessoRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** BE-16: rota pública de solicitação de acesso. Limite alto aqui; o 429 tem teste próprio. */
@SpringBootTest(properties = {
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes",
        "app.solicitacoes-acesso.limite-global-minuto=1000"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class SolicitacaoAcessoHttpTests {
    static final String SCHEMA = "acesso_" + UUID.randomUUID().toString().replace("-", "");
    static final String ROTA = "/api/auth/solicitacoes-acesso";

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        registro.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registro.add("spring.flyway.schemas", () -> SCHEMA);
        registro.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired SolicitacaoAcessoRepository solicitacoes;
    @Autowired JdbcTemplate jdbc;

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
    void registraPedidoSemAutenticacaoENormalizaEmail() throws Exception {
        String email = UUID.randomUUID() + "@teste.local";
        String corpo = enviar(null, pedido("  Maria Silva ", "  " + email.toUpperCase() + " ", " Preciso cadastrar demandas "), 202);
        assertTrue(corpo.contains("Solicitação recebida"));
        var linha = jdbc.queryForMap("SELECT nome, email, justificativa, status FROM solicitacoes_acesso WHERE email = ?", email);
        assertEquals("Maria Silva", linha.get("nome"));
        assertEquals("Preciso cadastrar demandas", linha.get("justificativa"));
        assertEquals("PENDENTE", linha.get("status"));
        // Token inválido não atrapalha uma rota pública.
        enviar("Bearer token-invalido", pedido("Outra", UUID.randomUUID() + "@teste.local", "Acesso"), 202);
    }

    @Test
    void respostaIdenticaNaoRevelaUsuarioExistenteNemPedidoRepetido() throws Exception {
        String novo = enviar(null, pedido("Novo", UUID.randomUUID() + "@teste.local", "Quero acesso"), 202);

        String emailUsuario = usuario().getEmail();
        String paraUsuario = enviar(null, pedido("Intruso", emailUsuario.toUpperCase(), "Quero acesso"), 202);

        String repetido = UUID.randomUUID() + "@teste.local";
        enviar(null, pedido("Ana", repetido, "Primeiro pedido"), 202);
        String segundo = enviar(null, pedido("Ana de novo", repetido, "Segundo pedido"), 202);

        assertEquals(novo, paraUsuario);
        assertEquals(novo, segundo);
        assertEquals(0, contar(emailUsuario));
        assertEquals(1, contar(repetido));
        assertEquals("Primeiro pedido", jdbc.queryForObject(
                "SELECT justificativa FROM solicitacoes_acesso WHERE email = ?", String.class, repetido));
    }

    @Test
    void bancoGaranteUmPedidoPendentePorEmail() {
        String email = UUID.randomUUID() + "@teste.local";
        assertEquals(1, solicitacoes.registrarSeNaoHouverPendente("A", email, "Motivo", LocalDateTime.now()));
        assertEquals(0, solicitacoes.registrarSeNaoHouverPendente("B", email, "Motivo", LocalDateTime.now()));
        assertEquals(1, contar(email));
    }

    @Test
    void validaCamposETamanhos() throws Exception {
        String email = UUID.randomUUID() + "@teste.local";
        assertTrue(enviar(null, Map.of("email", email, "justificativa", "Motivo"), 400).contains("VALIDACAO_FALHOU"));
        enviar(null, pedido("Nome", "nao-e-email", "Motivo"), 400);
        enviar(null, pedido("   ", email, "Motivo"), 400);
        enviar(null, pedido("Nome", email, "   "), 400);
        enviar(null, pedido(" ", email, "Motivo"), 400);
        enviar(null, pedido("n".repeat(151), email, "Motivo"), 400);
        enviar(null, pedido("Nome", "a".repeat(195) + "@x.com", "Motivo"), 400);
        enviar(null, pedido("Nome", email, "j".repeat(1001)), 400);
        mvc.perform(request(HttpMethod.POST, ROTA).contentType(MediaType.APPLICATION_JSON).content("{nome"))
                .andExpect(status().isBadRequest());
        assertEquals(0, contar(email));
        enviar(null, pedido("n".repeat(150), email, "j".repeat(1000)), 202);
        assertEquals(1, contar(email));
    }

    @Test
    void somenteOPostEhPublico() throws Exception {
        mvc.perform(request(HttpMethod.GET, ROTA)).andExpect(status().isUnauthorized());
    }

    private Map<String, Object> pedido(String nome, String email, String justificativa) {
        return Map.of("nome", nome, "email", email, "justificativa", justificativa);
    }

    private String enviar(String authorization, Object corpo, int esperado) throws Exception {
        var requisicao = request(HttpMethod.POST, ROTA).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(corpo));
        if (authorization != null) requisicao.header("Authorization", authorization);
        return mvc.perform(requisicao).andExpect(status().is(esperado)).andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private long contar(String email) {
        return jdbc.queryForObject("SELECT count(*) FROM solicitacoes_acesso WHERE email = ?", Long.class, email);
    }

    private Usuario usuario() {
        var usuario = new Usuario();
        usuario.setNome("Usuário existente");
        usuario.setEmail(UUID.randomUUID() + "@teste.local");
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(PerfilUsuario.DEMANDANTE);
        usuario.setAtivo(true);
        return usuarios.saveAndFlush(usuario);
    }
}
