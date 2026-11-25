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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Portfólio: filtros do frontend, pendências por projeto e formato {@code Projeto}, contra o PostgreSQL real. */
@SpringBootTest(properties = {
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PortfolioHttpTests {
    private static final String SCHEMA = "portfolio_" + UUID.randomUUID().toString().replace("-", "");
    private static final String SENHA = "Senha-Teste-123";
    private static final long IDEACAO = 1, POC = 2, MVP = 3, CONCLUIDO = 4;
    private static final long TRANSICAO_IDEACAO_POC = 1;

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transacao;

    private long dem, ana, esp, campanha;
    private long pTriagem, pParecer, pGovernanca, pValidacao, pSemPendencia, pArquivado, pConcluido, pAntigo;
    private String token;

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

    @BeforeAll
    void montarCenario() throws Exception {
        dem = usuario("dem", PerfilUsuario.DEMANDANTE);
        ana = usuario("ana", PerfilUsuario.ANALISTA);
        esp = usuario("esp", PerfilUsuario.ESPECIALISTA);
        LocalDateTime agora = LocalDateTime.now();
        campanha = jdbc.queryForObject(
                "insert into acoes_mobilizacao(nome) values ('Hackathon de inovação') returning id", Long.class);

        pTriagem = projeto("Gestão de numerário", IDEACAO, agora.minusDays(1), campanha);

        pParecer = projeto("Conferência de documentos 100%", IDEACAO, agora.minusDays(2), null);
        qualificar(pParecer);
        consulta(pParecer, agora.minusDays(2));

        pGovernanca = projeto("Tokenização de garantias", IDEACAO, agora.minusDays(3), null);
        qualificar(pGovernanca);
        pedido(pGovernanca, agora.minusDays(3));

        pValidacao = projeto("Plataforma IoT", MVP, agora.minusDays(4), null);
        qualificar(pValidacao);
        jdbc.update("insert into entregas_projeto(projeto_id, autor_id, descricao, submetido_em) values (?, ?, 'Painel', ?)",
                pValidacao, ana, ts(agora.minusDays(4)));

        pSemPendencia = projeto("Biometria comportamental", POC, agora.minusDays(5), null);
        qualificar(pSemPendencia);
        long respondida = consulta(pSemPendencia, agora.minusDays(5));
        transacao.executeWithoutResult(t -> {
            long id = jdbc.queryForObject("""
                    insert into pareceres_tecnicos(projeto_id, autor_id, consulta_id, fundamentacao, conclusao, criado_em, evidencias_fechadas)
                    values (?, ?, ?, 'Fundamentação', 'Recomendado', now(), false) returning id""",
                    Long.class, pSemPendencia, esp, respondida);
            jdbc.update("update pareceres_tecnicos set evidencias_fechadas = true where id = ?", id);
        });

        pArquivado = projeto("Projeto arquivado", IDEACAO, agora.minusDays(6), null);
        jdbc.update("update projetos set arquivado = true, arquivado_em = now(), arquivado_por_id = ?, motivo_arquivamento = 'Fora do escopo' where id = ?",
                esp, pArquivado);
        pConcluido = projeto("Projeto concluído", CONCLUIDO, agora.minusDays(7), null);
        pAntigo = projeto("Projeto antigo", IDEACAO, agora.minusDays(400), null);

        token = login("ana");
    }

    @Test
    void semFiltroTrazTodosDoMaisRecenteParaOMaisAntigoNoFormatoDoFrontend() throws Exception {
        JsonNode pagina = listar(get("/api/projetos"));
        assertEquals(8, pagina.get("totalElementos").asLong());
        assertEquals(List.of(pTriagem, pParecer, pGovernanca, pValidacao, pSemPendencia, pArquivado, pConcluido, pAntigo),
                ids(pagina.get("conteudo")));

        JsonNode triagem = pagina.get("conteudo").get(0);
        assertEquals("Dor fictícia de teste", triagem.get("descricaoDor").asText());
        assertEquals("INDUZIDA", triagem.get("origem").asText());
        assertEquals("Hackathon de inovação", triagem.get("campanha").get("titulo").asText());
        assertEquals("OUTRA", triagem.get("campanha").get("tipo").asText());
        assertEquals("#F59E0B", triagem.get("etapa").get("cor").asText());
        assertEquals(false, triagem.get("etapa").get("terminal").asBoolean());
        assertEquals("dem", triagem.get("demandante").get("nome").asText());
        assertTrue(triagem.hasNonNull("versao"));
        assertEquals(List.of("AGUARDANDO_TRIAGEM"), textos(triagem.get("pendencias")));

        JsonNode arquivado = projetoDe(pagina, pArquivado);
        assertEquals("Fora do escopo", arquivado.get("motivoArquivamento").asText());
        assertEquals(List.of(), textos(arquivado.get("pendencias")), "arquivado não aguarda triagem");
        assertEquals(true, projetoDe(pagina, pConcluido).get("etapa").get("terminal").asBoolean());
        assertEquals("CONCLUSAO", projetoDe(pagina, pConcluido).get("etapa").get("tipoTerminal").asText());
    }

    @Test
    void cadaPendenciaFiltraOsProjetosCertos() throws Exception {
        assertEquals(List.of(pTriagem, pAntigo), ids(pendencia("AGUARDANDO_TRIAGEM")));
        assertEquals(List.of(pParecer), ids(pendencia("AGUARDANDO_PARECER")));
        assertEquals(List.of(pGovernanca), ids(pendencia("AGUARDANDO_GOVERNANCA")));
        assertEquals(List.of(pValidacao), ids(pendencia("AGUARDANDO_VALIDACAO")));
        JsonNode governanca = listar(get("/api/projetos").param("pendencia", "AGUARDANDO_GOVERNANCA")).get("conteudo").get(0);
        assertEquals(List.of("AGUARDANDO_GOVERNANCA"), textos(governanca.get("pendencias")));
    }

    @Test
    void filtrosDoFrontendCombinam() throws Exception {
        assertEquals(List.of(pTriagem), ids(listar(get("/api/projetos").param("campanhaId", String.valueOf(campanha))).get("conteudo")));
        assertEquals(List.of(pTriagem), ids(listar(get("/api/projetos").param("origem", "INDUZIDA")).get("conteudo")));
        assertEquals(7, listar(get("/api/projetos").param("periodoDias", "30")).get("totalElementos").asLong());
        assertEquals(7, listar(get("/api/projetos").param("arquivamento", "ATIVOS")).get("totalElementos").asLong());
        assertEquals(List.of(pArquivado), ids(listar(get("/api/projetos").param("arquivamento", "ARQUIVADOS")).get("conteudo")));
        assertEquals(8, listar(get("/api/projetos").param("arquivamento", "TODOS")).get("totalElementos").asLong());
        assertEquals(List.of(pParecer), ids(listar(get("/api/projetos").param("busca", "100%")).get("conteudo")),
                "o % da busca é literal");
        assertEquals(List.of(pTriagem), ids(listar(get("/api/projetos")
                .param("etapaId", String.valueOf(IDEACAO)).param("pendencia", "AGUARDANDO_TRIAGEM")
                .param("periodoDias", "30")).get("conteudo")));
    }

    @Test
    void aceitaPaginacaoComNomesDoFrontendEOrdenacao() throws Exception {
        JsonNode pagina = listar(get("/api/projetos").param("pagina", "1").param("tamanho", "3"));
        assertEquals(1, pagina.get("pagina").asInt());
        assertEquals(3, pagina.get("tamanho").asInt());
        assertEquals(List.of(pValidacao, pSemPendencia, pArquivado), ids(pagina.get("conteudo")));
        assertEquals(List.of(pAntigo, pConcluido), ids(listar(get("/api/projetos").param("sort", "criadoEm,asc")
                .param("size", "2")).get("conteudo")));
    }

    @Test
    void parametrosInvalidosRespondem400() throws Exception {
        mvc.perform(get("/api/projetos").param("periodoDias", "0").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
        mvc.perform(get("/api/projetos").param("pendencia", "QUALQUER").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
        mvc.perform(get("/api/projetos").param("sort", "campoInexistente").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    private JsonNode pendencia(String estado) throws Exception {
        return listar(get("/api/projetos").param("pendencia", estado)).get("conteudo");
    }

    private JsonNode listar(MockHttpServletRequestBuilder requisicao) throws Exception {
        String corpo = mvc.perform(requisicao.header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(corpo);
    }

    private String login(String nome) throws Exception {
        String corpo = json.writeValueAsString(Map.of("email", nome + "@portfolio.teste", "senha", SENHA));
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(resposta).get("token").asText();
    }

    private long usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome(login);
        usuario.setEmail(login + "@portfolio.teste");
        usuario.setSenhaHash(encoder.encode(SENHA));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarios.save(usuario).getId();
    }

    private long projeto(String titulo, long etapa, LocalDateTime criadoEm, Long acao) {
        return jdbc.queryForObject("""
                insert into projetos(titulo, descricao_dor, area_demandante_id, responsavel_po_id, arena, trl_atual,
                                     etapa_id, origem, acao_mobilizacao_id, demandante_id, analista_responsavel_id,
                                     criado_em, atualizado_em)
                values (?, 'Dor fictícia de teste', 1, ?, 'A_DEFINIR', 2, ?, ?, ?, ?, ?, ?, ?) returning id""",
                Long.class, titulo, ana, etapa, acao == null ? "ESPONTANEA" : "INDUZIDA", acao,
                dem, ana, ts(criadoEm), ts(criadoEm));
    }

    private void qualificar(long projeto) {
        jdbc.update("""
                insert into qualificacoes_demanda(projeto_id, autor_id, aderencia_tese, relevancia, grau_inovacao,
                                                  viabilidade, maturidade_tecnologica, fundamentacao, conclusao, criado_em)
                values (?, ?, 'ALTA', 'ALTA', 'MEDIO', 'ALTA', 'TRL 2', 'Fundamentação', 'Seguir', now())""", projeto, ana);
    }

    private long consulta(long projeto, LocalDateTime em) {
        return jdbc.queryForObject("""
                insert into consultas_especialista(projeto_id, solicitante_id, especialista_id, questao, criado_em)
                values (?, ?, ?, 'Pergunta técnica', ?) returning id""", Long.class, projeto, ana, esp, ts(em));
    }

    private void pedido(long projeto, LocalDateTime em) {
        jdbc.update("""
                insert into solicitacoes_transicao(projeto_id, solicitante_id, transicao_id, origem_id, destino_id,
                                                   exige_governanca, status, justificativa, criado_em)
                values (?, ?, ?, ?, ?, true, 'PENDENTE', 'Justificativa do pedido', ?)""",
                projeto, ana, TRANSICAO_IDEACAO_POC, IDEACAO, POC, ts(em));
    }

    private static Timestamp ts(LocalDateTime valor) {
        return Timestamp.valueOf(valor);
    }

    private static JsonNode projetoDe(JsonNode pagina, long id) {
        for (JsonNode p : pagina.get("conteudo")) {
            if (p.get("id").asLong() == id) {
                return p;
            }
        }
        throw new AssertionError("projeto " + id + " ausente");
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
}
