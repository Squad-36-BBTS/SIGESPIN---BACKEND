package br.com.bbts.sigespin;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** BE-14: comentários em projetos, exercitando HTTP, JWT, regras por perfil e a migration V10. */
@SpringBootTest(properties = {
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class InteracaoProjetoHttpTests {
    static final String SCHEMA = "interacoes_" + UUID.randomUUID().toString().replace("-", "");

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
    @Autowired PasswordEncoder senhas;
    @Autowired JdbcTemplate jdbc;

    Usuario demandante, analista, especialista;
    String tokenD, tokenOutroD, tokenA, tokenE;

    @BeforeEach
    void preparar() throws Exception {
        demandante = usuario(PerfilUsuario.DEMANDANTE);
        analista = usuario(PerfilUsuario.ANALISTA);
        especialista = usuario(PerfilUsuario.ESPECIALISTA);
        tokenD = login(demandante);
        tokenOutroD = login(usuario(PerfilUsuario.DEMANDANTE));
        tokenA = login(analista);
        tokenE = login(especialista);
    }

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
    void perfisAutorizadosComentamEAutoriaVemDoJwt() throws Exception {
        long id = cadastrar();
        // autorId enviado pelo cliente é ignorado: o autor é sempre quem está autenticado.
        var resposta = mvc.perform(request(HttpMethod.POST, interacoes(id))
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("texto", "  Precisamos do orçamento  ",
                                "autorId", especialista.getId()))))
                .andExpect(status().isCreated()).andReturn().getResponse();
        var criada = json.readTree(resposta.getContentAsByteArray());
        assertEquals(analista.getId(), criada.path("autor").path("id").asLong());
        assertEquals("ANALISTA", criada.path("autor").path("perfil").asText());
        assertEquals("Precisamos do orçamento", criada.path("texto").asText());
        assertEquals(id, criada.path("projetoId").asLong());
        assertFalse(criada.path("criadoEm").isNull());
        assertTrue(resposta.getHeader("Location").endsWith(interacoes(id) + "/" + criada.path("id").asLong()));

        assertEquals(especialista.getId(), comentar(id, tokenE, "Parecer em andamento", 201).path("autor").path("id").asLong());
        assertEquals(demandante.getId(), comentar(id, tokenD, "Orçamento enviado", 201).path("autor").path("id").asLong());
    }

    @Test
    void demandanteDeOutroProjetoNaoComentaMasTodosConsultam() throws Exception {
        long id = cadastrar();
        assertEquals("ACESSO_NEGADO", comentar(id, tokenOutroD, "Intromissão", 403).path("codigo").asText());
        comentar(id, null, "Sem login", 401);
        comentar(id, tokenA, "Comentário visível", 201);
        api(HttpMethod.GET, interacoes(id), null, null, 401);
        for (String token : new String[]{tokenOutroD, tokenD, tokenA, tokenE}) {
            assertEquals(1, api(HttpMethod.GET, interacoes(id), token, null, 200).path("totalElementos").asInt());
        }
    }

    @Test
    void validaTextoObrigatorioELimiteDe2000Caracteres() throws Exception {
        long id = cadastrar();
        assertEquals("VALIDACAO_FALHOU", comentar(id, tokenA, "", 400).path("codigo").asText());
        comentar(id, tokenA, "   ", 400);
        comentar(id, tokenA, "  ", 400);
        comentar(id, tokenA, "a".repeat(2001), 400);
        api(HttpMethod.POST, interacoes(id), tokenA, Map.of(), 400);
        mvc.perform(request(HttpMethod.POST, interacoes(id)).header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON).content("{texto")).andExpect(status().isBadRequest());
        assertEquals(0, api(HttpMethod.GET, interacoes(id), tokenA, null, 200).path("totalElementos").asInt());
        assertEquals(2000, comentar(id, tokenA, "a".repeat(2000), 201).path("texto").asText().length());
    }

    @Test
    void projetoInexistenteRetorna404() throws Exception {
        assertEquals("RECURSO_NAO_ENCONTRADO", comentar(999_999_999L, tokenA, "Olá", 404).path("codigo").asText());
        api(HttpMethod.GET, interacoes(999_999_999L), tokenA, null, 404);
    }

    @Test
    void projetoArquivadoBloqueiaNovosComentariosEPreservaLeitura() throws Exception {
        long id = cadastrar();
        comentar(id, tokenA, "Antes do arquivamento", 201);
        long versao = api(HttpMethod.GET, "/api/projetos/" + id, tokenE, null, 200).path("versao").asLong();
        api(HttpMethod.PUT, "/api/projetos/" + id + "/arquivamento", tokenE,
                Map.of("arquivado", true, "versao", versao, "justificativa", "Pausa"), 200);
        assertEquals("REGRA_NEGOCIO", comentar(id, tokenA, "Depois", 422).path("codigo").asText());
        comentar(id, tokenD, "Depois", 422);
        assertEquals(1, api(HttpMethod.GET, interacoes(id), tokenOutroD, null, 200).path("totalElementos").asInt());
    }

    @Test
    void listaMaisRecentesPrimeiroComPaginacaoLimitadaEOrdemFixa() throws Exception {
        long id = cadastrar();
        comentar(id, tokenA, "primeiro", 201);
        comentar(id, tokenE, "segundo", 201);
        comentar(id, tokenD, "terceiro", 201);
        comentar(cadastrar(), tokenA, "de outro projeto", 201);

        // O parâmetro sort do cliente é ignorado; a ordem é sempre a mais recente primeiro.
        var lista = api(HttpMethod.GET, interacoes(id) + "?size=250&sort=texto,asc", tokenA, null, 200);
        assertEquals(100, lista.path("tamanho").asInt());
        assertEquals(3, lista.path("totalElementos").asInt());
        assertEquals("terceiro", lista.path("conteudo").get(0).path("texto").asText());
        assertEquals("primeiro", lista.path("conteudo").get(2).path("texto").asText());

        var pagina = api(HttpMethod.GET, interacoes(id) + "?page=1&size=2", tokenA, null, 200);
        assertEquals(1, pagina.path("conteudo").size());
        assertEquals("primeiro", pagina.path("conteudo").get(0).path("texto").asText());
        assertTrue(pagina.path("ultima").asBoolean());
    }

    @Test
    void comentarioEhImutavelNoBanco() throws Exception {
        long interacao = comentar(cadastrar(), tokenA, "Registro histórico", 201).path("id").asLong();
        // Última instrução do teste: a falha aborta a transação, que é desfeita em seguida.
        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE interacoes_projeto SET texto = 'alterado' WHERE id = ?", interacao));
    }

    private Usuario usuario(PerfilUsuario perfil) {
        var usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setEmail(UUID.randomUUID() + "@teste.local");
        usuario.setSenhaHash(senhas.encode("senha-api-123"));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarios.saveAndFlush(usuario);
    }

    private String login(Usuario usuario) throws Exception {
        return api(HttpMethod.POST, "/api/auth/login", null,
                Map.of("email", usuario.getEmail(), "senha", "senha-api-123"), 200).path("token").asText();
    }

    /** Projeto criado pelo Demandante principal, que passa a ser o demandante responsável. */
    private long cadastrar() throws Exception {
        var demanda = json.createObjectNode().put("titulo", "Demanda interações").put("descricaoDor", "Problema da área")
                .put("areaDemandanteId", 1).put("origem", "ESPONTANEA").put("trlInicialDeclarado", 2);
        return api(HttpMethod.POST, "/api/projetos", tokenD, demanda, 201).path("id").asLong();
    }

    private JsonNode comentar(long projetoId, String token, String texto, int esperado) throws Exception {
        return api(HttpMethod.POST, interacoes(projetoId), token, Map.of("texto", texto), esperado);
    }

    private String interacoes(long projetoId) { return "/api/projetos/" + projetoId + "/interacoes"; }

    private JsonNode api(HttpMethod metodo, String caminho, String token, Object corpo, int esperado) throws Exception {
        var requisicao = request(metodo, caminho);
        if (token != null) requisicao.header("Authorization", "Bearer " + token);
        if (corpo != null) requisicao.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(corpo));
        var resultado = mvc.perform(requisicao).andExpect(status().is(esperado)).andReturn();
        byte[] resposta = resultado.getResponse().getContentAsByteArray();
        return resposta.length == 0 ? json.nullNode() : json.readTree(resposta);
    }
}
