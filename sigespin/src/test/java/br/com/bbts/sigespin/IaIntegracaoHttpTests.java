package br.com.bbts.sigespin;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa o cliente HTTP de produção contra um provedor local controlado. */
@SpringBootTest(properties = {
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes",
        "app.ia.gemini.api-key=chave-simulada",
        "app.ia.gemini.model=modelo-teste",
        "app.ia.gemini.timeout-ms=500"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class IaIntegracaoHttpTests {
    private static final String SCHEMA = "ia_api_" + UUID.randomUUID().toString().replace("-", "");
    private static final String ROTA = "/api/ia/analisar-demanda";
    private static final String SUGESTAO = """
            {"arenaSugerida":"EFICIENCIA_OPERACIONAL","hipotese":"Se automatizarmos, reduziremos o retrabalho.",
             "trlInicial":2,"potencialLeiDoBem":true,"justificativaArena":"Eficiência do processo",
             "scoresRadar":{"negocio":2.5,"produto":1.5,"time":2,"receita":1}}
            """;
    private static final AtomicReference<RespostaProvedor> resposta = new AtomicReference<>();
    private static final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private static final AtomicReference<String> chaveRecebida = new AtomicReference<>();
    private static final AtomicReference<String> caminhoRecebido = new AtomicReference<>();
    private static final AtomicInteger chamadas = new AtomicInteger();
    private static HttpServer servidor;
    private static ExecutorService executor;

    private record RespostaProvedor(int status, String corpo, long atrasoMs) { }

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        servidor.setExecutor(executor);
        servidor.createContext("/v1beta/", troca -> {
            chamadas.incrementAndGet();
            caminhoRecebido.set(troca.getRequestURI().toString());
            chaveRecebida.set(troca.getRequestHeaders().getFirst("x-goog-api-key"));
            corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var atual = resposta.get();
            try {
                Thread.sleep(atual.atrasoMs());
                byte[] corpo = atual.corpo().getBytes(StandardCharsets.UTF_8);
                troca.getResponseHeaders().set("Content-Type", "application/json");
                troca.sendResponseHeaders(atual.status(), corpo.length);
                troca.getResponseBody().write(corpo);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                troca.close();
            }
        });
        servidor.start();
        registro.add("app.ia.gemini.base-url", () -> "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1beta");
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        registro.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registro.add("spring.flyway.schemas", () -> SCHEMA);
        registro.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProjetoRepository projetos;
    @Autowired PasswordEncoder senhas;
    @Autowired br.com.bbts.sigespin.service.ProtecaoIa protecao;
    String demandante, analista, especialista;

    @BeforeEach
    void preparar() throws Exception {
        protecao.circuito().reset();
        chamadas.set(0);
        corpoRecebido.set(null);
        resposta.set(new RespostaProvedor(200, envelope(SUGESTAO), 0));
        demandante = token(PerfilUsuario.DEMANDANTE);
        analista = token(PerfilUsuario.ANALISTA);
        especialista = token(PerfilUsuario.ESPECIALISTA);
    }

    @Test
    void limitePorUsuarioRetorna429SemAcionarProvedorNovamente() throws Exception {
        for (int i=0; i<10; i++) analisar(demandante, entrada(), 200);
        var resultado = mvc.perform(request(HttpMethod.POST, ROTA)
                .header("Authorization", "Bearer "+demandante).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("titulo", "Demanda", "descricao", "Problema operacional"))))
                .andExpect(status().isTooManyRequests()).andReturn();
        assertNotNull(resultado.getResponse().getHeader("Retry-After"));
        assertEquals(10, chamadas.get());
        analisar(analista, entrada(), 200);
        assertEquals(11, chamadas.get());
    }

    @Test
    void circuitoAbertoRetorna503SemNovaChamadaAoProvedor() throws Exception {
        resposta.set(new RespostaProvedor(503, "{}", 0));
        for (int i=0; i<6; i++) analisar(demandante, entrada(), 503);
        assertEquals(5, chamadas.get());
    }

    @AfterAll
    void encerrar() throws Exception {
        servidor.stop(0);
        executor.shutdownNow();
        try (var conexao = DriverManager.getConnection(System.getenv("SIGESPIN_TEST_DB_URL"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_PASSWORD", ""));
             var statement = conexao.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Test
    void demandanteRecebeSugestaoComProtocoloRealSemPersistirProjeto() throws Exception {
        long antes = projetos.count();
        var resultado = analisar(demandante, entrada(), 200);
        assertEquals("EFICIENCIA_OPERACIONAL", resultado.path("arenaSugerida").asText());
        assertEquals(2, resultado.path("trlInicial").asInt());
        assertEquals(2.5, resultado.path("scoresRadar").path("negocio").asDouble());
        assertEquals(antes, projetos.count());
        assertEquals("chave-simulada", chaveRecebida.get());
        assertEquals("/v1beta/models/modelo-teste:generateContent", caminhoRecebido.get());
        var enviado = json.readTree(corpoRecebido.get());
        assertTrue(enviado.path("systemInstruction").path("parts").path(0).path("text").asText().contains("PRO663-009"));
        assertTrue(enviado.path("contents").path(0).path("parts").path(0).path("text").asText().contains("O processo é manual."));
        assertEquals("application/json", enviado.path("generationConfig").path("responseMimeType").asText());
        var esquema = enviado.path("generationConfig").path("responseJsonSchema");
        assertEquals(6, esquema.path("required").size());
        assertEquals(3, esquema.path("properties").path("trlInicial").path("maximum").asInt());
        assertEquals(4, esquema.path("properties").path("scoresRadar").path("properties").path("receita").path("maximum").asInt());
    }

    @Test
    void analistaTambemPodeSolicitarSugestao() throws Exception {
        analisar(analista, entrada(), 200);
        assertEquals(1, chamadas.get());
    }

    @Test
    void aceitaDescricaoDorComoAliasDoContratoDeIntake() throws Exception {
        analisar(demandante, Map.of("titulo", "Triagem", "descricaoDor", "O processo é manual."), 200);
        assertTrue(corpoRecebido.get().contains("O processo é manual."));
    }

    @Test
    void especialistaEVisitanteRecebemBloqueioAntesDeConsultarProvedor() throws Exception {
        analisar(especialista, entrada(), 403);
        analisar(null, entrada(), 401);
        assertEquals(0, chamadas.get());
    }

    @Test
    void validaCamposEComprimentosAntesDeConsultarProvedor() throws Exception {
        analisar(demandante, Map.of("titulo", "Triagem"), 400);
        analisar(demandante, Map.of("titulo", " ", "descricao", "Problema"), 400);
        analisar(demandante, Map.of("titulo", "X".repeat(256), "descricao", "Problema"), 400);
        analisar(demandante, Map.of("titulo", "Triagem", "descricao", "X".repeat(4001)), 400);
        assertEquals(0, chamadas.get());
    }

    @Test
    void limiteDeUsoDoProvedorRetornaIndisponibilidadeComMensagemControlada() throws Exception {
        resposta.set(new RespostaProvedor(429, json.writeValueAsString(Map.of("error", Map.of("message", "detalhe reservado do provedor"))), 0));
        var erro = analisar(demandante, entrada(), 503);
        assertEquals("IA_INDISPONIVEL", erro.path("codigo").asText());
        assertFalse(erro.toString().contains("detalhe reservado"));
        assertFalse(erro.toString().contains("chave-simulada"));
    }

    @Test
    void falhaInternaDoProvedorRetorna503() throws Exception {
        resposta.set(new RespostaProvedor(500, "{}", 0));
        analisar(demandante, entrada(), 503);
    }

    @Test
    void timeoutDoClienteDeProducaoRetorna503() throws Exception {
        resposta.set(new RespostaProvedor(200, envelope(SUGESTAO), 1500));
        var erro = analisar(demandante, entrada(), 503);
        assertEquals("IA_INDISPONIVEL", erro.path("codigo").asText());
    }

    @Test
    void bloqueioSemCandidatosRetorna503() throws Exception {
        resposta.set(new RespostaProvedor(200, json.writeValueAsString(Map.of("promptFeedback", Map.of("blockReason", "SAFETY"))), 0));
        analisar(demandante, entrada(), 503);
    }

    @Test
    void jsonGeradoInvalidoOuIncompletoRetorna503() throws Exception {
        resposta.set(new RespostaProvedor(200, envelope("texto fora do JSON"), 0));
        analisar(demandante, entrada(), 503);
        resposta.set(new RespostaProvedor(200, envelope("{}"), 0));
        analisar(demandante, entrada(), 503);
    }

    @Test
    void respostaHttpComJsonMalformadoRetorna503() throws Exception {
        resposta.set(new RespostaProvedor(200, "{malformado", 0));
        analisar(demandante, entrada(), 503);
    }

    @Test
    void consideraTextoFinalEmMultiplasPartes() throws Exception {
        var partes = List.of(Map.of("thought", true, "text", "Resumo interno"),
                Map.of("text", SUGESTAO.substring(0, 40)), Map.of("text", SUGESTAO.substring(40)));
        String corpo = json.writeValueAsString(Map.of("candidates", List.of(Map.of("finishReason", "STOP", "content", Map.of("parts", partes)))));
        resposta.set(new RespostaProvedor(200, corpo, 0));
        assertEquals(2, analisar(demandante, entrada(), 200).path("trlInicial").asInt());
    }

    @Test
    void geracaoInterrompidaRetorna503MesmoComTextoJson() throws Exception {
        String corpo = json.writeValueAsString(Map.of("candidates", List.of(
                Map.of("finishReason", "MAX_TOKENS", "content",
                        Map.of("parts", List.of(Map.of("text", SUGESTAO)))))));
        resposta.set(new RespostaProvedor(200, corpo, 0));
        analisar(demandante, entrada(), 503);
    }

    private String envelope(String texto) throws Exception {
        return json.writeValueAsString(Map.of("candidates", List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", texto)))))));
    }

    private Map<String, String> entrada() {
        return Map.of("titulo", "Triagem", "descricao", "O processo é manual.");
    }

    private String token(PerfilUsuario perfil) throws Exception {
        var usuario = new Usuario();
        usuario.setNome("Teste IA " + perfil);
        usuario.setEmail(UUID.randomUUID() + "@teste.local");
        usuario.setSenhaHash(senhas.encode("senha-ia-123"));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuarios.saveAndFlush(usuario);
        return api("/api/auth/login", null, Map.of("email", usuario.getEmail(), "senha", "senha-ia-123"), 200).path("token").asText();
    }

    private JsonNode analisar(String token, Object entrada, int esperado) throws Exception {
        return api(ROTA, token, entrada, esperado);
    }

    private JsonNode api(String rota, String token, Object corpo, int esperado) throws Exception {
        var requisicao = request(HttpMethod.POST, rota).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(corpo));
        if (token != null) requisicao.header("Authorization", "Bearer " + token);
        var resultado = mvc.perform(requisicao).andExpect(status().is(esperado)).andReturn();
        return json.readTree(resultado.getResponse().getContentAsByteArray());
    }
}
