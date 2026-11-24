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

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BE-13: métricas do painel contra o PostgreSQL real, com valores calculados à mão.
 *
 * <pre>
 * Projeto  Etapa      Origem      Arena                  TRL  Registro  Situação
 * A        IDEACAO    ESPONTANEA  EFICIENCIA_OPERACIONAL  2    10 dias   sem qualificação (triagem)
 * B        POC        INDUZIDA    NEGOCIOS_DIGITAIS       4    20 dias   consulta sem parecer + pedido pendente
 * C        MVP        ESPONTANEA  A_DEFINIR               6    100 dias  entrega sem validação
 * D        IDEACAO    ESPONTANEA  EFICIENCIA_OPERACIONAL  9    5 dias    arquivado (só conta em "arquivados")
 * E        CONCLUIDO  ESPONTANEA  SEGURANCA_INTEGRADA     8    30 dias   terminal: não entra na triagem
 *
 * Radar: A = INICIAL(2,1,1,1), ATUAL(3,2,2,2) há 5 dias, ATUAL(4,3,3,3) há 1 dia
 *        B = só INICIAL(1,1,1,1)      D (arquivado) = INICIAL(4,4,4,4), ignorado
 * </pre>
 */
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
class MetricasDashboardHttpTests {
    private static final String SCHEMA = "metricas_" + UUID.randomUUID().toString().replace("-", "");
    private static final String SENHA = "Senha-Teste-123";
    private static final long IDEACAO = 1, POC = 2, MVP = 3, CONCLUIDO = 4;

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private long dem, ana, esp;

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
    void montarCenario() {
        dem = usuario("dem", PerfilUsuario.DEMANDANTE);
        ana = usuario("ana", PerfilUsuario.ANALISTA);
        esp = usuario("esp", PerfilUsuario.ESPECIALISTA);
        LocalDateTime agora = LocalDateTime.now();
        long campanha = jdbc.queryForObject("insert into acoes_mobilizacao(nome) values ('Desafio') returning id", Long.class);

        long a = projeto("A", IDEACAO, null, "EFICIENCIA_OPERACIONAL", 2, agora.minusDays(10));
        radar(a, "INICIAL", "2.00", "1.00", agora.minusDays(10));
        radar(a, "ATUAL", "3.00", "2.00", agora.minusDays(5));
        radar(a, "ATUAL", "4.00", "3.00", agora.minusDays(1));

        long b = projeto("B", POC, campanha, "NEGOCIOS_DIGITAIS", 4, agora.minusDays(20));
        qualificar(b);
        radar(b, "INICIAL", "1.00", "1.00", agora.minusDays(20));
        jdbc.update("""
                insert into consultas_especialista(projeto_id, solicitante_id, especialista_id, questao, criado_em)
                values (?, ?, ?, 'Pergunta sem resposta', now())""", b, ana, esp);
        jdbc.update("""
                insert into solicitacoes_transicao(projeto_id, solicitante_id, transicao_id, origem_id, destino_id,
                                                   exige_governanca, status, justificativa, criado_em)
                values (?, ?, 3, ?, ?, true, 'PENDENTE', 'Pedido de avanço', now())""", b, ana, POC, MVP);

        long c = projeto("C", MVP, null, "A_DEFINIR", 6, agora.minusDays(100));
        qualificar(c);
        jdbc.update("insert into entregas_projeto(projeto_id, autor_id, descricao, submetido_em) values (?, ?, 'MVP', now())", c, ana);

        long d = projeto("D", IDEACAO, null, "EFICIENCIA_OPERACIONAL", 9, agora.minusDays(5));
        radar(d, "INICIAL", "4.00", "4.00", agora.minusDays(5));
        jdbc.update("update projetos set arquivado = true, arquivado_em = now(), arquivado_por_id = ?, motivo_arquivamento = 'teste' where id = ?", esp, d);

        projeto("E", CONCLUIDO, null, "SEGURANCA_INTEGRADA", 8, agora.minusDays(30));
    }

    @Test
    void semFiltroCalculaTodosOsIndicadores() throws Exception {
        JsonNode m = metricas(dem, "");

        assertEquals(4, m.get("totalProjetos").asLong());
        assertEquals(1, m.get("arquivados").asLong());
        assertEquals(5.0, m.get("trlMedio").asDouble(), 0.001);

        assertEquals(List.of("IDEACAO=1", "POC=1", "MVP=1", "CONCLUIDO=1", "DESCONTINUADO=0"), etapas(m));
        assertEquals("#F59E0B", m.get("porEtapa").get(0).get("etapa").get("cor").asText());

        assertEquals(3, m.get("porOrigem").get("ESPONTANEA").asLong());
        assertEquals(1, m.get("porOrigem").get("INDUZIDA").asLong());

        assertEquals(1, m.get("porArena").get("EFICIENCIA_OPERACIONAL").asLong());
        assertEquals(1, m.get("porArena").get("NEGOCIOS_DIGITAIS").asLong());
        assertEquals(1, m.get("porArena").get("SEGURANCA_INTEGRADA").asLong());
        assertEquals(1, m.get("porArena").get("A_DEFINIR").asLong());

        assertEquals(1, m.get("pendencias").get("AGUARDANDO_TRIAGEM").asLong(), "só A: E é terminal e D está arquivado");
        assertEquals(1, m.get("pendencias").get("AGUARDANDO_PARECER").asLong());
        assertEquals(1, m.get("pendencias").get("AGUARDANDO_GOVERNANCA").asLong());
        assertEquals(1, m.get("pendencias").get("AGUARDANDO_VALIDACAO").asLong());

        // Inicial: média de A(2,1,1,1) e B(1,1,1,1). Atual: A usa a ATUAL mais recente (4,3,3,3); B usa a INICIAL.
        assertRadar(m.get("radarMedioInicial"), 1.5, 1.0);
        assertRadar(m.get("radarMedioAtual"), 2.5, 2.0);
    }

    @Test
    void filtroPorPeriodoUsaADataDeRegistro() throws Exception {
        JsonNode m = metricas(ana, "periodoDias=60");
        assertEquals(3, m.get("totalProjetos").asLong(), "C foi registrado há 100 dias");
        assertEquals(1, m.get("arquivados").asLong());
        assertEquals(4.67, m.get("trlMedio").asDouble(), 0.001);
        assertEquals(0, m.get("pendencias").get("AGUARDANDO_VALIDACAO").asLong());
    }

    @Test
    void filtroPorEtapaEOrigem() throws Exception {
        JsonNode porEtapa = metricas(esp, "etapaId=" + POC);
        assertEquals(1, porEtapa.get("totalProjetos").asLong());
        assertEquals(4.0, porEtapa.get("trlMedio").asDouble(), 0.001);
        assertEquals(List.of("IDEACAO=0", "POC=1", "MVP=0", "CONCLUIDO=0", "DESCONTINUADO=0"), etapas(porEtapa));

        JsonNode induzidas = metricas(esp, "origem=INDUZIDA");
        assertEquals(1, induzidas.get("totalProjetos").asLong());
        assertEquals(0, induzidas.get("porOrigem").get("ESPONTANEA").asLong());
    }

    @Test
    void filtrosVaziosValemComoTodos() throws Exception {
        JsonNode m = metricas(dem, "periodoDias=&etapaId=&origem=");
        assertEquals(4, m.get("totalProjetos").asLong());
    }

    @Test
    void recorteSemProjetosDevolveZeros() throws Exception {
        JsonNode m = metricas(dem, "etapaId=5");
        assertEquals(0, m.get("totalProjetos").asLong());
        assertEquals(0.0, m.get("trlMedio").asDouble());
        assertRadar(m.get("radarMedioAtual"), 0.0, 0.0);
    }

    @Test
    void periodoInvalidoRecebe400() throws Exception {
        mvc.perform(get("/api/dashboard/metricas?periodoDias=0").header("Authorization", token(dem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    void semLoginRecebe401() throws Exception {
        mvc.perform(get("/api/dashboard/metricas")).andExpect(status().isUnauthorized());
    }

    // ---------- Apoio ----------

    private JsonNode metricas(long usuario, String filtros) throws Exception {
        String corpo = mvc.perform(get("/api/dashboard/metricas?" + filtros).header("Authorization", token(usuario)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(corpo);
    }

    private String token(long usuario) throws Exception {
        String email = usuarios.findById(usuario).orElseThrow().getEmail();
        String corpo = json.writeValueAsString(Map.of("email", email, "senha", SENHA));
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return "Bearer " + json.readTree(resposta).get("token").asText();
    }

    private long usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome(login);
        usuario.setEmail(login + "@metricas.teste");
        usuario.setSenhaHash(encoder.encode(SENHA));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarios.save(usuario).getId();
    }

    private long projeto(String titulo, long etapa, Long campanha, String arena, int trl, LocalDateTime criadoEm) {
        return jdbc.queryForObject("""
                insert into projetos(titulo, descricao_dor, area_demandante_id, responsavel_po_id, arena, trl_atual,
                                     etapa_id, origem, acao_mobilizacao_id, demandante_id, criado_em, atualizado_em)
                values (?, 'Dor fictícia', 1, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id""",
                Long.class, titulo, ana, arena, trl, etapa, campanha == null ? "ESPONTANEA" : "INDUZIDA", campanha,
                dem, Timestamp.valueOf(criadoEm), Timestamp.valueOf(criadoEm));
    }

    private void qualificar(long projeto) {
        jdbc.update("""
                insert into qualificacoes_demanda(projeto_id, autor_id, aderencia_tese, relevancia, grau_inovacao,
                                                  viabilidade, maturidade_tecnologica, fundamentacao, conclusao, criado_em)
                values (?, ?, 'ALTA', 'ALTA', 'MEDIO', 'ALTA', 'TRL 2', 'Fundamentação', 'Seguir', now())""", projeto, ana);
    }

    /** Leitura com {@code negocio} próprio e o mesmo valor para produto, time e receita. */
    private void radar(long projeto, String tipo, String negocio, String demais, LocalDateTime em) {
        jdbc.update("""
                insert into avaliacao_radar(projeto_id, tipo_leitura, score_negocio, score_produto, score_time,
                                            score_receita, data_avaliacao)
                values (?, ?, ?::numeric, ?::numeric, ?::numeric, ?::numeric, ?)""",
                projeto, tipo, negocio, demais, demais, demais, Timestamp.valueOf(em));
    }

    private static void assertRadar(JsonNode radar, double negocio, double demais) {
        assertEquals(negocio, radar.get("negocio").asDouble(), 0.001);
        assertEquals(demais, radar.get("produto").asDouble(), 0.001);
        assertEquals(demais, radar.get("time").asDouble(), 0.001);
        assertEquals(demais, radar.get("receita").asDouble(), 0.001);
    }

    private static List<String> etapas(JsonNode metricas) {
        List<String> lista = new ArrayList<>();
        metricas.get("porEtapa").forEach(e -> lista.add(e.get("etapa").get("codigo").asText() + "=" + e.get("total").asLong()));
        return lista;
    }
}
