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
import org.springframework.transaction.support.TransactionTemplate;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BE-12: Minha fila por perfil, contra o PostgreSQL e a segurança reais.
 *
 * <p>O cenário imita as telas do protótipo e inclui casos que NÃO devem aparecer (projeto arquivado, etapa
 * terminal, consulta de outro Especialista, retornos com mais de 30 dias, recomendação recusada). Os registros
 * seguem o ciclo de vida que os gatilhos do banco exigem: pedidos e recomendações nascem pendentes e são
 * encerrados depois; pareceres fecham o conjunto de evidências na mesma transação.</p>
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
class MinhaFilaHttpTests {
    private static final String SCHEMA = "fila_" + UUID.randomUUID().toString().replace("-", "");
    private static final String SENHA = "Senha-Teste-123";
    private static final long IDEACAO = 1, POC = 2, MVP = 3, CONCLUIDO = 4;
    private static final long TRANSICAO_IDEACAO_POC = 1, TRANSICAO_POC_MVP = 3;

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transacao;

    private long dem, dem2, ana, ana2, esp, esp2;
    private long pConferencia, pIot, pTokenizacao, pBiometria, pNumerario, pAnomalias;

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
        dem2 = usuario("dem2", PerfilUsuario.DEMANDANTE);
        ana = usuario("ana", PerfilUsuario.ANALISTA);
        ana2 = usuario("ana2", PerfilUsuario.ANALISTA);
        esp = usuario("esp", PerfilUsuario.ESPECIALISTA);
        esp2 = usuario("esp2", PerfilUsuario.ESPECIALISTA);
        LocalDateTime agora = LocalDateTime.now();

        // Qualificado, com consulta sem parecer → AGUARDANDO_PARECER
        pConferencia = projeto("Automatizar conferência de documentos", dem, ana, IDEACAO, agora.minusDays(20), null);
        qualificar(pConferencia);
        consulta(pConferencia, ana, esp, "Qual OCR usar?", agora.minusDays(5));

        // MVP com entrega sem validação → AGUARDANDO_VALIDACAO e fila do Demandante
        pIot = projeto("Plataforma IoT de eficiência energética", dem, ana, MVP, agora.minusDays(19), null);
        qualificar(pIot);
        entrega(pIot, ana, "Painel de consumo das 10 agências piloto", agora.minusDays(11));

        // Consulta sem parecer + pedido de avanço pendente com evidência → PARECER e GOVERNANÇA
        pTokenizacao = projeto("Tokenização de garantias do agronegócio", dem, ana, IDEACAO, agora.minusDays(18), null);
        qualificar(pTokenizacao);
        consulta(pTokenizacao, ana, esp, "Quais riscos de custódia de chaves?", agora.minusDays(13));
        long pedidoTokenizacao = pedido(pTokenizacao, ana, TRANSICAO_IDEACAO_POC, IDEACAO, POC, agora.minusDays(14));
        long evidencia = jdbc.queryForObject("""
                insert into evidencias(projeto_id, nome_arquivo, caminho_storage, tipo_arquivo, data_upload)
                values (?, 'proposta-poc.pdf', 'legado/proposta-poc.pdf', 'application/pdf', ?) returning id""",
                Long.class, pTokenizacao, ts(agora.minusDays(14)));
        jdbc.update("insert into evidencias_transicao(projeto_id, solicitacao_id, evidencia_id) values (?, ?, ?)",
                pTokenizacao, pedidoTokenizacao, evidencia);

        // Entrega validada há 3 dias → "Validações recebidas" do Analista; nenhuma pendência
        pBiometria = projeto("Biometria comportamental em canais digitais", dem, ana, MVP, agora.minusDays(30), null);
        qualificar(pBiometria);
        long entregaBiometria = entrega(pBiometria, ana, "Entrega #2", agora.minusDays(6));
        jdbc.update("""
                insert into validacoes_entrega(projeto_id, entrega_id, demandante_id, resultado, observacoes, validado_em)
                values (?, ?, ?, 'VALIDADA_COM_RESSALVAS', 'Ajustar alertas', ?)""",
                pBiometria, entregaBiometria, dem, ts(agora.minusDays(3)));

        // Sem qualificação, sem Demandante, induzida → triagem (com a campanha)
        long campanha = jdbc.queryForObject(
                "insert into acoes_mobilizacao(nome) values ('Hackathon de inovação') returning id", Long.class);
        pNumerario = projeto("Gestão inteligente de numerário", null, null, IDEACAO, agora.minusDays(60), campanha);

        // Não entram na triagem: arquivado e etapa terminal
        long arquivado = projeto("Projeto arquivado", dem2, null, IDEACAO, agora.minusDays(70), null);
        jdbc.update("update projetos set arquivado = true, arquivado_em = ?, arquivado_por_id = ?, motivo_arquivamento = 'teste' where id = ?",
                ts(agora.minusDays(1)), esp, arquivado);
        projeto("Projeto concluído", dem2, null, CONCLUIDO, agora.minusDays(80), null);
        consulta(arquivado, ana, esp, "Consulta em projeto arquivado", agora.minusDays(2));

        // Pedidos encerrados: rejeitado e cancelado recentes entram; aprovado há 40 dias não
        pAnomalias = projeto("Detecção de anomalias em rede corporativa", dem, ana, POC, agora.minusDays(50), null);
        qualificar(pAnomalias);
        encerrar(pedido(pAnomalias, ana, TRANSICAO_POC_MVP, POC, MVP, agora.minusDays(10)), "REJEITADA", esp,
                "Reapresentar após teste em rede segregada", agora.minusDays(2));
        encerrar(pedido(pAnomalias, ana, TRANSICAO_POC_MVP, POC, MVP, agora.minusDays(9)), "CANCELADA", ana,
                "Pedido duplicado", agora.minusDays(1));
        encerrar(pedido(pAnomalias, ana, TRANSICAO_POC_MVP, POC, MVP, agora.minusDays(45)), "APROVADA", esp,
                "Aprovado", agora.minusDays(40));
        // Pedido pendente de outro Analista: o Especialista vê, a Ana não
        pedido(pConferencia, ana2, TRANSICAO_IDEACAO_POC, IDEACAO, POC, agora.minusDays(4));

        // Pareceres: um recente às consultas da Ana, um antigo (fora da janela)
        long consultaRespondida = consulta(pAnomalias, ana, esp, "A rede segregada é obrigatória?", agora.minusDays(8));
        parecer(pAnomalias, esp, consultaRespondida, agora.minusDays(4));
        long consultaAntiga = consulta(pAnomalias, ana, esp, "Pergunta antiga", agora.minusDays(60));
        parecer(pAnomalias, esp, consultaAntiga, agora.minusDays(50));
        // Consulta dirigida a outro Especialista
        consulta(pConferencia, ana, esp2, "Para o outro especialista", agora.minusDays(3));

        // Propostas de configuração: uma da Ana e uma da Ana2
        jdbc.update("""
                insert into solicitacoes_configuracao_kanban(tipo, solicitante_id, codigo, nome, ordem, justificativa, status, criado_em)
                values ('COLUNA', ?, 'PILOTO', 'Piloto', 3, 'Etapa de piloto controlado', 'PENDENTE', ?)""",
                ana, ts(agora.minusDays(11)));
        jdbc.update("""
                insert into solicitacoes_configuracao_kanban(tipo, solicitante_id, origem_id, destino_id, exige_governanca, justificativa, status, criado_em)
                values ('TRANSICAO', ?, ?, ?, true, 'Atalho de ideação para MVP', 'PENDENTE', ?)""",
                ana2, IDEACAO, MVP, ts(agora.minusDays(7)));

        // Recomendações: uma pendente para o Demandante, uma recusada (não entra)
        recomendacao(esp, dem, "Automatizar triagem de chamados", agora.minusDays(2));
        long recusada = recomendacao(esp, dem, "Recomendação recusada", agora.minusDays(9));
        jdbc.update("update recomendacoes_demanda set status = 'RECUSADA', encerrado_em = ?, justificativa_encerramento = 'Fora do escopo' where id = ?",
                ts(agora.minusDays(8)), recusada);
    }

    @Test
    void demandanteVeEntregasRecomendacoesEDemandasComPendencias() throws Exception {
        JsonNode fila = fila("dem");

        JsonNode entregas = fila.get("entregasParaValidar");
        assertEquals(1, entregas.size());
        assertEquals(pIot, entregas.get(0).get("projetoId").asLong());
        assertEquals("Plataforma IoT de eficiência energética", entregas.get(0).get("projetoTitulo").asText());
        assertEquals("ana", entregas.get(0).get("registradaPor").get("nome").asText());
        assertEquals(0, entregas.get(0).get("validacoes").size());

        JsonNode recomendacoes = fila.get("recomendacoesPendentes");
        assertEquals(1, recomendacoes.size());
        assertEquals("Automatizar triagem de chamados", recomendacoes.get(0).get("titulo").asText());
        assertEquals("ESPECIALISTA", recomendacoes.get(0).get("autorPerfil").asText());

        Map<Long, List<String>> pendencias = pendenciasPorProjeto(fila.get("minhasDemandas"));
        assertEquals(5, pendencias.size(), "só os projetos em que é o Demandante");
        assertEquals(List.of("AGUARDANDO_PARECER", "AGUARDANDO_GOVERNANCA"), pendencias.get(pConferencia),
                "consulta sem parecer e pedido pendente de outra Analista");
        assertEquals(List.of("AGUARDANDO_VALIDACAO"), pendencias.get(pIot));
        assertEquals(List.of("AGUARDANDO_PARECER", "AGUARDANDO_GOVERNANCA"), pendencias.get(pTokenizacao));
        assertEquals(List.of(), pendencias.get(pBiometria));
        assertEquals(List.of(), pendencias.get(pAnomalias));

        JsonNode tokenizacao = projetoDe(fila.get("minhasDemandas"), pTokenizacao);
        assertEquals("IDEACAO", tokenizacao.get("etapa").get("codigo").asText());
        assertEquals("#F59E0B", tokenizacao.get("etapa").get("cor").asText());
        assertEquals(false, tokenizacao.get("etapa").get("terminal").asBoolean());

        assertVazias(fila, "triagem", "consultasPendentes", "solicitacoesPendentes", "solicitacoesEncerradas",
                "pareceresRecebidos", "propostasPendentes", "entregasValidadas");
    }

    @Test
    void analistaVeTriagemPedidosPareceresValidacoesEPropostas() throws Exception {
        JsonNode fila = fila("ana");

        JsonNode triagem = fila.get("triagem");
        assertEquals(1, triagem.size(), "arquivado e etapa terminal ficam fora");
        assertEquals(pNumerario, triagem.get(0).get("id").asLong());
        assertTrue(triagem.get(0).get("demandante").isNull());
        assertEquals("Hackathon de inovação", triagem.get(0).get("campanha").get("titulo").asText());
        assertEquals(List.of("AGUARDANDO_TRIAGEM"), textos(triagem.get(0).get("pendencias")));

        JsonNode pendentes = fila.get("solicitacoesPendentes");
        assertEquals(1, pendentes.size(), "só os pedidos da própria Analista");
        assertEquals(pTokenizacao, pendentes.get(0).get("projetoId").asLong());
        assertEquals("IDEACAO", pendentes.get(0).get("origem").get("codigo").asText());
        assertEquals("POC", pendentes.get(0).get("destino").get("codigo").asText());
        assertEquals(1, pendentes.get(0).get("evidencias").size());
        assertEquals("proposta-poc.pdf", pendentes.get(0).get("evidencias").get(0).get("nomeArquivo").asText());

        JsonNode encerradas = fila.get("solicitacoesEncerradas");
        assertEquals(List.of("CANCELADA", "REJEITADA"), textos(encerradas, "status"),
                "mais recente primeiro; aprovado há 40 dias fica fora");
        assertEquals("esp", encerradas.get(1).get("encerradaPor").get("nome").asText());
        assertEquals("Reapresentar após teste em rede segregada", encerradas.get(1).get("justificativaEncerramento").asText());

        JsonNode pareceres = fila.get("pareceresRecebidos");
        assertEquals(1, pareceres.size(), "parecer de 50 dias atrás fica fora");
        assertEquals("A rede segregada é obrigatória?", pareceres.get(0).get("questao").asText());
        assertEquals("esp", pareceres.get(0).get("especialista").get("nome").asText());

        JsonNode validadas = fila.get("entregasValidadas");
        assertEquals(1, validadas.size());
        assertEquals("Entrega #2", validadas.get(0).get("descricao").asText());
        assertEquals("VALIDADA_COM_RESSALVAS", validadas.get(0).get("validacoes").get(0).get("resultado").asText());

        JsonNode propostas = fila.get("propostasPendentes");
        assertEquals(1, propostas.size(), "só as propostas da própria Analista");
        assertEquals("COLUNA", propostas.get(0).get("tipo").asText());
        assertEquals("Piloto", propostas.get(0).get("proposta").get("nome").asText());

        assertVazias(fila, "consultasPendentes", "entregasParaValidar", "minhasDemandas", "recomendacoesPendentes");
    }

    @Test
    void especialistaVeConsultasDirigidasEDecisoesPendentes() throws Exception {
        JsonNode fila = fila("esp");

        JsonNode consultas = fila.get("consultasPendentes");
        assertEquals(List.of("Quais riscos de custódia de chaves?", "Qual OCR usar?"), textos(consultas, "questao"),
                "mais antiga primeiro; sem parecer, só as dirigidas a ele e fora de projeto arquivado");
        assertEquals("PENDENTE", consultas.get(0).get("status").asText());
        assertEquals("Tokenização de garantias do agronegócio", consultas.get(0).get("projetoTitulo").asText());

        JsonNode pedidos = fila.get("solicitacoesPendentes");
        assertEquals(List.of(pTokenizacao, pConferencia), ids(pedidos, "projetoId"), "todos os pendentes, mais antigo primeiro");

        JsonNode propostas = fila.get("propostasPendentes");
        assertEquals(List.of("COLUNA", "TRANSICAO"), textos(propostas, "tipo"));
        assertEquals(MVP, propostas.get(1).get("proposta").get("destinoId").asLong());

        assertVazias(fila, "triagem", "solicitacoesEncerradas", "pareceresRecebidos", "entregasParaValidar",
                "entregasValidadas", "minhasDemandas", "recomendacoesPendentes");
    }

    @Test
    void outroEspecialistaSoVeASuaConsulta() throws Exception {
        assertEquals(List.of("Para o outro especialista"), textos(fila("esp2").get("consultasPendentes"), "questao"));
    }

    @Test
    void semLoginRecebe401() throws Exception {
        mvc.perform(get("/api/minha-fila")).andExpect(status().isUnauthorized());
    }

    // ---------- Apoio ----------

    private JsonNode fila(String login) throws Exception {
        String corpo = json.writeValueAsString(Map.of("email", login + "@fila.teste", "senha", SENHA));
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String token = json.readTree(resposta).get("token").asText();
        String fila = mvc.perform(get("/api/minha-fila").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return json.readTree(fila);
    }

    private long usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome(login);
        usuario.setEmail(login + "@fila.teste");
        usuario.setSenhaHash(encoder.encode(SENHA));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarios.save(usuario).getId();
    }

    private long projeto(String titulo, Long demandante, Long analista, long etapa, LocalDateTime criadoEm, Long campanha) {
        return jdbc.queryForObject("""
                insert into projetos(titulo, descricao_dor, area_demandante_id, responsavel_po_id, arena, trl_atual,
                                     etapa_id, origem, acao_mobilizacao_id, demandante_id, analista_responsavel_id,
                                     criado_em, atualizado_em)
                values (?, 'Dor fictícia de teste', 1, ?, 'A_DEFINIR', 2, ?, ?, ?, ?, ?, ?, ?) returning id""",
                Long.class, titulo, ana, etapa, campanha == null ? "ESPONTANEA" : "INDUZIDA", campanha,
                demandante, analista, ts(criadoEm), ts(criadoEm));
    }

    private void qualificar(long projeto) {
        jdbc.update("""
                insert into qualificacoes_demanda(projeto_id, autor_id, aderencia_tese, relevancia, grau_inovacao,
                                                  viabilidade, maturidade_tecnologica, fundamentacao, conclusao, criado_em)
                values (?, ?, 'ALTA', 'ALTA', 'MEDIO', 'ALTA', 'TRL 2', 'Fundamentação', 'Seguir', now())""", projeto, ana);
    }

    private long consulta(long projeto, long solicitante, long especialista, String questao, LocalDateTime em) {
        return jdbc.queryForObject("""
                insert into consultas_especialista(projeto_id, solicitante_id, especialista_id, questao, criado_em)
                values (?, ?, ?, ?, ?) returning id""", Long.class, projeto, solicitante, especialista, questao, ts(em));
    }

    private void parecer(long projeto, long autor, long consulta, LocalDateTime em) {
        transacao.executeWithoutResult(t -> {
            long id = jdbc.queryForObject("""
                    insert into pareceres_tecnicos(projeto_id, autor_id, consulta_id, fundamentacao, conclusao, criado_em, evidencias_fechadas)
                    values (?, ?, ?, 'Fundamentação do parecer', 'Recomendado', ?, false) returning id""",
                    Long.class, projeto, autor, consulta, ts(em));
            jdbc.update("update pareceres_tecnicos set evidencias_fechadas = true where id = ?", id);
        });
    }

    private long entrega(long projeto, long autor, String descricao, LocalDateTime em) {
        return jdbc.queryForObject("""
                insert into entregas_projeto(projeto_id, autor_id, descricao, submetido_em)
                values (?, ?, ?, ?) returning id""", Long.class, projeto, autor, descricao, ts(em));
    }

    private long pedido(long projeto, long solicitante, long transicao, long origem, long destino, LocalDateTime em) {
        return jdbc.queryForObject("""
                insert into solicitacoes_transicao(projeto_id, solicitante_id, transicao_id, origem_id, destino_id,
                                                   exige_governanca, status, justificativa, criado_em)
                values (?, ?, ?, ?, ?, true, 'PENDENTE', 'Justificativa do pedido', ?) returning id""",
                Long.class, projeto, solicitante, transicao, origem, destino, ts(em));
    }

    private void encerrar(long pedido, String status, long responsavel, String justificativa, LocalDateTime em) {
        jdbc.update("""
                update solicitacoes_transicao set status = ?, responsavel_encerramento_id = ?,
                       justificativa_encerramento = ?, encerrado_em = ? where id = ?""",
                status, responsavel, justificativa, ts(em), pedido);
    }

    private long recomendacao(long autor, long destinatario, String titulo, LocalDateTime em) {
        return jdbc.queryForObject("""
                insert into recomendacoes_demanda(autor_id, destinatario_id, titulo, descricao, status, criado_em)
                values (?, ?, ?, 'Descrição fictícia', 'PENDENTE', ?) returning id""",
                Long.class, autor, destinatario, titulo, ts(em));
    }

    private static Timestamp ts(LocalDateTime valor) {
        return Timestamp.valueOf(valor);
    }

    private static void assertVazias(JsonNode fila, String... listas) {
        for (String lista : listas) {
            assertEquals(0, fila.get(lista).size(), lista + " deveria estar vazia");
        }
    }

    private static Map<Long, List<String>> pendenciasPorProjeto(JsonNode projetos) {
        Map<Long, List<String>> mapa = new java.util.HashMap<>();
        projetos.forEach(p -> mapa.put(p.get("id").asLong(), textos(p.get("pendencias"))));
        return mapa;
    }

    private static JsonNode projetoDe(JsonNode projetos, long id) {
        for (JsonNode p : projetos) {
            if (p.get("id").asLong() == id) {
                return p;
            }
        }
        throw new AssertionError("projeto " + id + " ausente");
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

    private static List<Long> ids(JsonNode itens, String campo) {
        List<Long> lista = new ArrayList<>();
        itens.forEach(i -> lista.add(i.get(campo).asLong()));
        return lista;
    }
}
