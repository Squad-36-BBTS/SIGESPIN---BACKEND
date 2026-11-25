package br.com.bbts.sigespin;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Detalhe completo do projeto (GET /api/projetos/{id}) montado a partir do fluxo real pela API:
 * recomendação convertida, comprovantes, qualificação, consultas, parecer, TRL, governança, entrega,
 * validação, correção e comentário. Roda com open-in-view desligado, como em produção.
 */
@SpringBootTest(properties = {
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DetalheProjetoHttpTests {
    private static final String SCHEMA = "detalhe_" + UUID.randomUUID().toString().replace("-", "");
    private static final Path ARQUIVOS = Path.of(System.getProperty("java.io.tmpdir"), SCHEMA);
    private static final String SENHA = "Senha-Teste-123";
    private static final long TRANSICAO_IDEACAO_POC = 1;
    private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes();

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    private long dem, ana, esp, projeto, recomendacao, evParecer, evTrl, evPedido, evCorrecao, consultaRespondida,
            consultaPendente, parecer, pedido, entrega;
    private String tDem, tAna, tEsp;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        registro.add("app.arquivos.tipo", () -> "LOCAL");
        registro.add("app.arquivos.diretorio", ARQUIVOS::toString);
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
        if (Files.exists(ARQUIVOS)) {
            try (var caminhos = Files.walk(ARQUIVOS)) {
                for (var caminho : caminhos.sorted(Comparator.reverseOrder()).toList()) Files.delete(caminho);
            }
        }
    }

    @BeforeAll
    void percorrerOFluxo() throws Exception {
        dem = usuario("dem", PerfilUsuario.DEMANDANTE);
        ana = usuario("ana", PerfilUsuario.ANALISTA);
        esp = usuario("esp", PerfilUsuario.ESPECIALISTA);
        tDem = login("dem");
        tAna = login("ana");
        tEsp = login("esp");

        recomendacao = api(HttpMethod.POST, "/api/recomendacoes", tEsp, Map.of("destinatarioId", dem,
                "titulo", "Automatizar triagem", "descricao", "Sugestão fictícia"), 201).get("id").asLong();
        projeto = api(HttpMethod.POST, "/api/recomendacoes/" + recomendacao + "/conversao", tDem, Map.of(
                "titulo", "Triagem automática de chamados", "descricaoDor", "Dor fictícia de teste",
                "origem", "ESPONTANEA", "hipoteseSolucao", "Classificador de texto", "areaDemandanteId", 1,
                "trlInicialDeclarado", 2), 200).get("projetoId").asLong();
        String base = "/api/projetos/" + projeto;

        evParecer = enviar(tEsp, "parecer.pdf");
        evTrl = enviar(tAna, "trl.pdf");
        evPedido = enviar(tAna, "poc.pdf");
        evCorrecao = enviar(tDem, "correcao.pdf");

        api(HttpMethod.POST, base + "/qualificacoes", tAna, Map.of("aderenciaTese", "ALTA", "relevancia", "ALTA",
                "grauInovacao", "MEDIO", "viabilidade", "ALTA", "maturidadeTecnologica", "TRL 2",
                "fundamentacao", "Fundamentação", "conclusao", "Seguir"), 201);
        consultaRespondida = api(HttpMethod.POST, base + "/consultas", tAna,
                Map.of("especialistaId", esp, "questao", "Qual modelo usar?"), 201).get("id").asLong();
        consultaPendente = api(HttpMethod.POST, base + "/consultas", tAna,
                Map.of("especialistaId", esp, "questao", "Qual o custo?"), 201).get("id").asLong();
        parecer = api(HttpMethod.POST, base + "/pareceres", tEsp, Map.of("consultaId", consultaRespondida,
                "fundamentacao", "Modelo pequeno basta", "conclusao", "Recomendado",
                "evidenciasIds", List.of(evParecer)), 201).get("id").asLong();
        api(HttpMethod.POST, base + "/avaliacoes-trl", tAna, Map.of("nivel", 3, "justificativa", "Prova em bancada",
                "evidenciasIds", List.of(evTrl)), 201);
        pedido = api(HttpMethod.POST, base + "/solicitacoes-transicao", tAna, Map.of("transicaoId", TRANSICAO_IDEACAO_POC,
                "justificativa", "Pronto para POC", "evidenciasIds", List.of(evPedido)), 201).get("id").asLong();
        api(HttpMethod.PATCH, "/api/kanban/solicitacoes-transicao/" + pedido + "/decisao", tEsp,
                Map.of("aprovada", true, "justificativa", "Aprovado"), 200);
        entrega = api(HttpMethod.POST, base + "/entregas", tAna, Map.of("descricao", "Protótipo"), 201).get("id").asLong();
        api(HttpMethod.POST, base + "/entregas/" + entrega + "/validacoes", tDem,
                Map.of("resultado", "VALIDADA_COM_RESSALVAS", "observacoes", "Ajustar textos"), 201);
        api(HttpMethod.POST, base + "/correcoes", tDem, Map.of("descricao", "Corrigir categoria",
                "referencia", "Entrega 1", "evidenciaId", evCorrecao), 201);
        api(HttpMethod.POST, base + "/interacoes", tDem, Map.of("texto", "Obrigado pelo retorno"), 201);
    }

    @Test
    void detalheTrazOProjetoNoPrimeiroNivelEOsRegistrosDoFluxo() throws Exception {
        JsonNode d = api(HttpMethod.GET, "/api/projetos/" + projeto, tAna, null, 200);

        assertEquals(projeto, d.get("id").asLong());
        assertEquals("Triagem automática de chamados", d.get("titulo").asText());
        assertEquals("POC", d.get("etapa").get("codigo").asText());
        assertEquals("#0EA5E9", d.get("etapa").get("cor").asText());
        assertEquals(3, d.get("trlAtual").asInt());
        assertEquals(List.of("AGUARDANDO_PARECER"), textos(d.get("pendencias")));
        assertFalse(d.has("projeto"), "o projeto vem desembrulhado no primeiro nível");
        assertEquals(recomendacao, d.get("recomendacao").get("id").asLong());
        assertEquals("Automatizar triagem", d.get("recomendacao").get("titulo").asText());

        assertEquals(1, d.get("qualificacoes").size());
        assertEquals("ana", d.get("qualificacoes").get(0).get("analista").get("nome").asText());

        JsonNode consultas = d.get("consultas");
        assertEquals(2, consultas.size());
        assertEquals("RESPONDIDA", consultas.get(0).get("status").asText());
        assertEquals(parecer, consultas.get(0).get("parecerId").asLong());
        assertTrue(consultas.get(0).hasNonNull("respondidaEm"));
        assertEquals("PENDENTE", consultas.get(1).get("status").asText());
        assertTrue(consultas.get(1).get("parecerId").isNull());

        JsonNode p = d.get("pareceres").get(0);
        assertEquals(consultaRespondida, p.get("consultaId").asLong());
        assertEquals("Qual modelo usar?", p.get("questao").asText());
        assertEquals("esp", p.get("especialista").get("nome").asText());
        assertEquals(List.of(evParecer), ids(p.get("evidencias")));

        JsonNode trl = d.get("avaliacoesTrl").get(0);
        assertEquals(3, trl.get("trlNovo").asInt());
        assertEquals(List.of(evTrl), ids(trl.get("evidencias")));

        JsonNode s = d.get("solicitacoes").get(0);
        assertEquals("APROVADA", s.get("status").asText());
        assertEquals("IDEACAO", s.get("origem").get("codigo").asText());
        assertEquals("esp", s.get("encerradaPor").get("nome").asText());
        assertEquals(List.of(evPedido), ids(s.get("evidencias")));

        JsonNode e = d.get("entregas").get(0);
        assertEquals("Protótipo", e.get("descricao").asText());
        assertEquals("VALIDADA_COM_RESSALVAS", e.get("validacoes").get(0).get("resultado").asText());
        assertEquals("dem", e.get("validacoes").get(0).get("por").get("nome").asText());

        JsonNode c = d.get("correcoes").get(0);
        assertEquals(evCorrecao, c.get("evidencia").get("id").asLong());
        assertEquals(4, d.get("evidencias").size());
        JsonNode evidencia = d.get("evidencias").get(0);
        assertEquals("ENVIADO", evidencia.get("estadoEnvio").asText());
        assertEquals("/api/projetos/" + projeto + "/evidencias/" + evidencia.get("id").asLong() + "/conteudo",
                evidencia.get("downloadUrl").asText());

        List<String> mudancas = textos(d.get("historicoTransicoes"), "tipoMudanca");
        assertTrue(mudancas.contains("TRL") && mudancas.contains("ETAPA"), mudancas.toString());
        JsonNode etapa = encontrar(d.get("historicoTransicoes"), "tipoMudanca", "ETAPA");
        assertEquals("Ideação", etapa.get("valorAnterior").asText());
        assertEquals("POC", etapa.get("valorNovo").asText());
        assertEquals(pedido, etapa.get("solicitacaoId").asLong());

        JsonNode interacao = d.get("interacoes").get(0);
        assertEquals("Obrigado pelo retorno", interacao.get("texto").asText());
        assertEquals("DEMANDANTE", interacao.get("perfil").asText());

        List<String> destinos = new ArrayList<>();
        d.get("transicoesDisponiveis").forEach(t -> destinos.add(t.get("destino").get("codigo").asText()));
        assertEquals(List.of("MVP", "DESCONTINUADO"), destinos);
        assertTrue(d.get("historicoGestao").isArray());
    }

    @Test
    void projetoInexistenteResponde404EVisitanteSemTokenResponde401() throws Exception {
        assertEquals("RECURSO_NAO_ENCONTRADO",
                api(HttpMethod.GET, "/api/projetos/999999", tDem, null, 404).get("codigo").asText());
        api(HttpMethod.GET, "/api/projetos/" + projeto, null, null, 401);
    }

    private long enviar(String token, String nome) throws Exception {
        var resposta = mvc.perform(multipart("/api/projetos/" + projeto + "/evidencias")
                        .file(new MockMultipartFile("arquivo", nome, "application/pdf", PDF))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return json.readTree(resposta.getContentAsByteArray()).get("id").asLong();
    }

    private JsonNode api(HttpMethod metodo, String caminho, String token, Object corpo, int esperado) throws Exception {
        var requisicao = request(metodo, caminho);
        if (token != null) requisicao.header("Authorization", "Bearer " + token);
        if (corpo != null) requisicao.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(corpo));
        var http = mvc.perform(requisicao).andReturn().getResponse();
        byte[] resposta = http.getContentAsByteArray();
        assertEquals(esperado, http.getStatus(), metodo + " " + caminho + ": " + new String(resposta,
                java.nio.charset.StandardCharsets.UTF_8));
        return resposta.length == 0 ? json.nullNode() : json.readTree(resposta);
    }

    private String login(String nome) throws Exception {
        return api(HttpMethod.POST, "/api/auth/login", null,
                Map.of("email", nome + "@detalhe.teste", "senha", SENHA), 200).get("token").asText();
    }

    private long usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome(login);
        usuario.setEmail(login + "@detalhe.teste");
        usuario.setSenhaHash(encoder.encode(SENHA));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarios.save(usuario).getId();
    }

    private static JsonNode encontrar(JsonNode itens, String campo, String valor) {
        for (JsonNode i : itens) if (valor.equals(i.get(campo).asText())) return i;
        throw new AssertionError(campo + "=" + valor + " ausente");
    }

    private static List<Long> ids(JsonNode itens) {
        List<Long> lista = new ArrayList<>();
        itens.forEach(i -> lista.add(i.get("id").asLong()));
        return lista;
    }

    private static List<String> textos(JsonNode valores) {
        List<String> lista = new ArrayList<>();
        valores.forEach(v -> lista.add(v.asText()));
        return lista;
    }

    private static List<String> textos(JsonNode itens, String campo) {
        List<String> lista = new ArrayList<>();
        itens.forEach(i -> lista.add(i.get(campo).asText()));
        return lista;
    }
}
