package br.com.bbts.sigespin;

import br.com.bbts.sigespin.model.entity.Evidencia;
import br.com.bbts.sigespin.model.entity.TransicaoKanban;
import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.evidencia.EvidenciaRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.kanban.EtapaKanbanRepository;
import br.com.bbts.sigespin.repository.kanban.TransicaoKanbanRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercita HTTP, JWT, services, Hibernate e PostgreSQL no mesmo fluxo. */
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
class IntegracaoFases34Tests {
    static final String SCHEMA = "api34_" + UUID.randomUUID().toString().replace("-", "");
    static final java.nio.file.Path ARQUIVOS = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), SCHEMA);

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        registro.add("app.arquivos.diretorio", ARQUIVOS::toString);
        registro.add("app.arquivos.tipo", () -> "LOCAL");
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        registro.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registro.add("spring.flyway.schemas", () -> SCHEMA);
        registro.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProjetoRepository projetos;
    @Autowired EvidenciaRepository evidencias;
    @Autowired EtapaKanbanRepository etapas;
    @Autowired TransicaoKanbanRepository transicoes;
    @Autowired PasswordEncoder senhas;
    @Autowired br.com.bbts.sigespin.service.ArquivoEvidenciaService arquivos;
    @Autowired org.springframework.transaction.PlatformTransactionManager transacoes;

    Usuario demandante, outroDemandante, analista, especialista, outroEspecialista;
    String tokenD, tokenOutroD, tokenA, tokenE, tokenOutroE;

    @BeforeEach
    void preparar() throws Exception {
        demandante = usuario(PerfilUsuario.DEMANDANTE);
        outroDemandante = usuario(PerfilUsuario.DEMANDANTE);
        analista = usuario(PerfilUsuario.ANALISTA);
        especialista = usuario(PerfilUsuario.ESPECIALISTA);
        outroEspecialista = usuario(PerfilUsuario.ESPECIALISTA);
        tokenD = login(demandante);
        tokenOutroD = login(outroDemandante);
        tokenA = login(analista);
        tokenE = login(especialista);
        tokenOutroE = login(outroEspecialista);
    }

    @AfterAll
    void removerSchemaDescartavel() throws Exception {
        try (var conexao = DriverManager.getConnection(System.getenv("SIGESPIN_TEST_DB_URL"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_PASSWORD", ""));
             var statement = conexao.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
        if (java.nio.file.Files.exists(ARQUIVOS)) {
            try (var caminhos = java.nio.file.Files.walk(ARQUIVOS)) {
                for (var caminho : caminhos.sorted(java.util.Comparator.reverseOrder()).toList()) java.nio.file.Files.delete(caminho);
            }
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void uploadRealPreservaBytesMetadadosEDownloadCompartilhado() throws Exception {
        long id = cadastrar();
        var comprovante = enviarArquivo(id, tokenD, "comprovação.pdf", "%PDF-1.7\n%%EOF".getBytes(), 201);
        assertEquals(demandante.getId().longValue(), comprovante.path("enviadaPorId").asLong());
        assertEquals(64, comprovante.path("sha256").asText().length());
        assertFalse(comprovante.has("caminhoStorage"));
        var download = mvc.perform(request(HttpMethod.GET, comprovante.path("downloadUrl").asText())
                .header("Authorization", "Bearer " + tokenOutroD)).andExpect(status().isOk()).andReturn().getResponse();
        assertArrayEquals("%PDF-1.7\n%%EOF".getBytes(), download.getContentAsByteArray());
        assertTrue(download.getHeader("Content-Disposition").startsWith("attachment;"));
        assertEquals("nosniff", download.getHeader("X-Content-Type-Options"));
        assertEquals(1, api(HttpMethod.GET, projeto(id)+"/evidencias", tokenE, null, 200).size());
        long outro = cadastrar();
        api(HttpMethod.GET, projeto(outro)+"/evidencias/"+comprovante.path("id").asLong()+"/conteudo", tokenA, null, 404);
    }

    @Test
    void uploadRespeitaTitularidadePerfilEAutenticacao() throws Exception {
        long id = cadastrar();
        enviarArquivo(id, tokenOutroD, "prova.pdf", "%PDF-1.7".getBytes(), 403);
        enviarArquivo(id, null, "prova.pdf", "%PDF-1.7".getBytes(), 401);
        enviarArquivo(id, tokenA, "prova.pdf", "%PDF-1.7".getBytes(), 201);
        enviarArquivo(id, tokenE, "prova.pdf", "%PDF-1.7".getBytes(), 201);
        assertEquals(2, evidencias.findByProjetoIdOrderByDataUploadDesc(id).size());
    }

    @Test
    void arquivoEnviadoPodeFundamentarAvaliacaoTrl() throws Exception {
        long id = cadastrar();
        long arquivoId = enviarArquivo(id, tokenA, "teste.pdf", "%PDF-1.7".getBytes(), 201).path("id").asLong();
        api(HttpMethod.POST, projeto(id)+"/avaliacoes-trl", tokenA, avaliacao(3, arquivoId), 201);
        assertEquals(3, api(HttpMethod.GET, projeto(id), tokenD, null, 200).path("trlAtual").asInt());
    }

    @Test
    void arquivarBloqueiaNovosArquivosEPreservaLeituraDosAnteriores() throws Exception {
        long id = cadastrar();
        var arquivo = enviarArquivo(id, tokenD, "prova.pdf", "%PDF-1.7".getBytes(), 201);
        api(HttpMethod.PUT, projeto(id)+"/arquivamento", tokenE, Map.of("arquivado", true, "justificativa", "Encerrado", "versao", 0), 200);
        enviarArquivo(id, tokenE, "outro.pdf", "%PDF-1.7".getBytes(), 422);
        api(HttpMethod.GET, projeto(id)+"/evidencias", tokenOutroD, null, 200);
        mvc.perform(request(HttpMethod.GET, arquivo.path("downloadUrl").asText()).header("Authorization", "Bearer "+tokenD))
                .andExpect(status().isOk());
    }

    @Test
    void uploadInvalidoRetorna400EGrandeRetorna413SemCriarRegistro() throws Exception {
        long id = cadastrar();
        enviarArquivo(id, tokenD, "falso.pdf", "texto".getBytes(), 400);
        enviarArquivo(id, tokenD, "planilha.xlsx", new byte[]{1}, 400);
        enviarArquivo(id, tokenD, "vazio.pdf", new byte[0], 400);
        enviarArquivo(id, tokenD, "grande.pdf", new byte[10*1024*1024+1], 413);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(projeto(id)+"/evidencias")
                .header("Authorization", "Bearer "+tokenD)).andExpect(status().isBadRequest());
        assertTrue(evidencias.findByProjetoIdOrderByDataUploadDesc(id).isEmpty());
    }

    @Test
    void evidenciasLegadasPermanecemConsultaveisSemInventarConteudo() throws Exception {
        long id = cadastrar();
        long legado = evidencia(id);
        var lista = api(HttpMethod.GET, projeto(id)+"/evidencias", tokenD, null, 200);
        assertTrue(lista.path(0).path("sha256").isNull());
        api(HttpMethod.GET, projeto(id)+"/evidencias/"+legado+"/conteudo", tokenD, null, 404);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollbackRemoveObjetoFisicoSemPreservarMetadados() throws Exception {
        long id = cadastrar();
        var transacao = new org.springframework.transaction.support.TransactionTemplate(transacoes);
        var contexto = org.springframework.security.core.context.SecurityContextHolder.getContext();
        contexto.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                demandante.getEmail(), null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_DEMANDANTE"))));
        var chave = new java.util.concurrent.atomic.AtomicReference<String>();
        try {
            assertThrows(IllegalStateException.class, () -> transacao.execute(status -> {
                try {
                    var dto = arquivos.enviar(id, new org.springframework.mock.web.MockMultipartFile(
                            "arquivo", "prova.pdf", "application/pdf", "%PDF-1.7".getBytes()));
                    chave.set(evidencias.findById(dto.id()).orElseThrow().getCaminhoStorage());
                    assertTrue(java.nio.file.Files.exists(ARQUIVOS.resolve(chave.get())));
                    throw new IllegalStateException("Falha após gravação");
                } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            }));
            assertNotNull(chave.get());
            assertFalse(java.nio.file.Files.exists(ARQUIVOS.resolve(chave.get())));
            assertTrue(evidencias.findByProjetoIdOrderByDataUploadDesc(id).isEmpty());
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    private JsonNode enviarArquivo(long id, String token, String nome, byte[] bytes, int esperado) throws Exception {
        var req = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(projeto(id)+"/evidencias")
                .file(new org.springframework.mock.web.MockMultipartFile("arquivo", nome, "application/octet-stream", bytes));
        if (token != null) req.header("Authorization", "Bearer "+token);
        var resposta = mvc.perform(req).andExpect(status().is(esperado)).andReturn().getResponse();
        return resposta.getContentAsByteArray().length == 0 ? json.nullNode() : json.readTree(resposta.getContentAsByteArray());
    }

    @Test
    void cadastraDemandaComTrlDeclaradoEAutoriaDaSessao() throws Exception {
        var entrada = demanda().put("trlInicialDeclarado", 9).put("autorId", especialista.getId());
        var criada = api(HttpMethod.POST, "/api/projetos", tokenD, entrada, 201);
        assertEquals(9, criada.path("trlAtual").asInt());
        assertEquals("A_DEFINIR", criada.path("arena").asText());
        assertEquals("IDEACAO", criada.path("etapa").path("codigo").asText());
        assertEquals(demandante.getId(), criada.path("demandante").path("id").asLong());
    }

    @Test
    void exigeTrlECombinacaoValidaDeOrigemNoCadastro() throws Exception {
        var semTrl = demanda();
        semTrl.remove("trlInicialDeclarado");
        api(HttpMethod.POST, "/api/projetos", tokenD, semTrl, 400);
        api(HttpMethod.POST, "/api/projetos", tokenD, demanda().put("trlInicialDeclarado", 10), 400);
        api(HttpMethod.POST, "/api/projetos", tokenD, demanda().put("origem", "INDUZIDA"), 400);
        api(HttpMethod.POST, "/api/projetos", tokenD, demanda().put("acaoMobilizacaoId", 999999), 400);
    }

    @Test
    void cadastroRestritoAoDemandanteEConsultaExigeJwt() throws Exception {
        api(HttpMethod.POST, "/api/projetos", tokenA, demanda(), 403);
        api(HttpMethod.POST, "/api/projetos", tokenE, demanda(), 403);
        api(HttpMethod.GET, "/api/projetos", null, null, 401);
        api(HttpMethod.GET, "/api/projetos", "token-invalido", null, 401);
    }

    @Test
    void qualificaPreservandoHistoricoEPermiteConsultaGlobal() throws Exception {
        long id = cadastrar();
        var campos = Map.of("aderenciaTese", "Aderente", "relevancia", "Alta", "grauInovacao", "Incremental",
                "viabilidade", "Viável", "maturidadeTecnologica", "TRL declarado", "fundamentacao", "Análise técnica", "conclusao", "Prosseguir");
        var criada = api(HttpMethod.POST, projeto(id) + "/qualificacoes", tokenA, campos, 201);
        assertEquals(analista.getId(), criada.path("autor").path("id").asLong());
        api(HttpMethod.POST, projeto(id) + "/qualificacoes", tokenA, campos, 201);
        var lista = api(HttpMethod.GET, projeto(id) + "/qualificacoes", tokenOutroD, null, 200);
        assertEquals(2, lista.path("totalElementos").asInt());
        assertEquals(2, api(HttpMethod.GET, projeto(id), tokenE, null, 200).path("trlAtual").asInt());
        api(HttpMethod.POST, projeto(id) + "/qualificacoes", tokenE, campos, 403);
    }

    @Test
    void consultaDirigidaAceitaParecerTextualSomenteDoDestinatario() throws Exception {
        long id = cadastrar();
        var consulta = api(HttpMethod.POST, projeto(id) + "/consultas", tokenA,
                Map.of("especialistaId", especialista.getId(), "questao", "Avaliar os riscos"), 201);
        var parecer = Map.of("consultaId", consulta.path("id").asLong(), "fundamentacao", "Análise suficiente", "conclusao", "Prosseguir");
        api(HttpMethod.POST, projeto(id) + "/pareceres", tokenOutroE, parecer, 403);
        var resposta = api(HttpMethod.POST, projeto(id) + "/pareceres", tokenE, parecer, 201);
        assertTrue(resposta.path("evidenciasFechadas").asBoolean());
        assertEquals(especialista.getId(), resposta.path("autor").path("id").asLong());
        api(HttpMethod.POST, projeto(id) + "/pareceres", tokenE, parecer, 201);
        assertEquals(2, api(HttpMethod.GET, projeto(id) + "/pareceres", tokenD, null, 200).path("totalElementos").asInt());
        api(HttpMethod.GET, projeto(id) + "/consultas", tokenD, null, 200);
    }

    @Test
    void especialistaPodeEmitirParecerIndependente() throws Exception {
        long id = cadastrar();
        api(HttpMethod.POST, projeto(id) + "/pareceres", tokenE,
                Map.of("fundamentacao", "Conhecimento técnico", "conclusao", "Favorável", "evidenciaIds", List.of()), 201);
        assertEquals(1, api(HttpMethod.GET, projeto(id) + "/pareceres", tokenA, null, 200).path("totalElementos").asInt());
    }

    @Test
    void trlPermiteSubidaReducaoEManutencaoPorAnalistaEEspecialista() throws Exception {
        long id = cadastrar();
        long evidencia = evidencia(id);
        var subida = api(HttpMethod.POST, projeto(id) + "/avaliacoes-trl", tokenE, avaliacao(6, evidencia), 201);
        assertEquals(2, subida.path("nivelAnterior").asInt());
        var reducao = api(HttpMethod.POST, projeto(id) + "/avaliacoes-trl", tokenA, avaliacao(3, evidencia), 201);
        assertEquals(6, reducao.path("nivelAnterior").asInt());
        api(HttpMethod.POST, projeto(id) + "/avaliacoes-trl", tokenE, avaliacao(3, evidencia), 201);
        var atual = api(HttpMethod.GET, projeto(id), tokenOutroD, null, 200);
        assertEquals(3, atual.path("trlAtual").asInt());
        assertEquals("IDEACAO", atual.path("etapa").path("codigo").asText());
        assertEquals(3, api(HttpMethod.GET, projeto(id) + "/avaliacoes-trl", tokenD, null, 200).path("totalElementos").asInt());
        api(HttpMethod.POST, projeto(id) + "/avaliacoes-trl", tokenD, avaliacao(4, evidencia), 403);
    }

    @Test
    void rejeitaTrlSemEvidenciaEComEvidenciaDeOutroProjeto() throws Exception {
        long id = cadastrar();
        api(HttpMethod.POST, projeto(id) + "/avaliacoes-trl", tokenA,
                Map.of("nivel", 4, "justificativa", "Testes", "evidenciaIds", List.of()), 400);
        api(HttpMethod.POST, projeto(id) + "/avaliacoes-trl", tokenE, avaliacao(4, evidencia(cadastrar())), 422);
        assertEquals(2, api(HttpMethod.GET, projeto(id), tokenD, null, 200).path("trlAtual").asInt());
    }

    @Test
    void governancaExigeComprovanteMantemOrigemEAprovaComHistorico() throws Exception {
        long id = cadastrar();
        long transicao = percursoGovernado();
        var semEvidencia = Map.of("transicaoId", transicao, "justificativa", "Testes concluídos");
        api(HttpMethod.POST, projeto(id) + "/solicitacoes-transicao", tokenA, semEvidencia, 400);
        var pedido = api(HttpMethod.POST, projeto(id) + "/solicitacoes-transicao", tokenA, pedido(transicao, evidencia(id)), 201);
        assertEquals("IDEACAO", api(HttpMethod.GET, projeto(id), tokenD, null, 200).path("etapa").path("codigo").asText());
        api(HttpMethod.POST, projeto(id) + "/movimentacoes", tokenE, Map.of("transicaoId", transicao, "justificativa", "Mover"), 422);
        var aprovado = api(HttpMethod.PATCH, decisao(pedido.path("id").asLong()), tokenE,
                Map.of("aprovada", true, "justificativa", "Evidências suficientes"), 200);
        assertEquals("APROVADA", aprovado.path("status").asText());
        assertEquals("POC", api(HttpMethod.GET, projeto(id), tokenD, null, 200).path("etapa").path("codigo").asText());
        assertEquals(1, api(HttpMethod.GET, projeto(id) + "/movimentacoes", tokenD, null, 200).path("totalElementos").asInt());
        assertEquals(1, api(HttpMethod.GET, "/api/notificacoes", tokenOutroD, null, 200).path("totalElementos").asInt());
    }

    @Test
    void rejeicaoECancelamentoPreservamEtapa() throws Exception {
        long id = cadastrar();
        var pedido = solicitar(id);
        api(HttpMethod.PATCH, decisao(pedido.path("id").asLong()), tokenE,
                Map.of("aprovada", false, "justificativa", "Revisar testes"), 200);
        var segundo = solicitar(id);
        api(HttpMethod.POST, "/api/kanban/solicitacoes-transicao/" + segundo.path("id").asLong() + "/cancelamento", tokenA,
                Map.of("justificativa", "Escopo alterado"), 200);
        assertEquals("IDEACAO", api(HttpMethod.GET, projeto(id), tokenD, null, 200).path("etapa").path("codigo").asText());
        assertEquals(0, api(HttpMethod.GET, projeto(id) + "/movimentacoes", tokenD, null, 200).path("totalElementos").asInt());
    }

    @Test
    void especialistaMovePercursoGovernadoEAnalistaMovePercursoLivre() throws Exception {
        long id = cadastrar();
        var corpo = Map.of("transicaoId", percursoGovernado(), "justificativa", "Autorização direta");
        api(HttpMethod.POST, projeto(id) + "/movimentacoes", tokenA, corpo, 422);
        api(HttpMethod.POST, projeto(id) + "/movimentacoes", tokenE, corpo, 201);
        var retorno = new TransicaoKanban();
        retorno.setOrigem(etapas.findByCodigoAndAtivaTrue("POC").orElseThrow());
        retorno.setDestino(etapas.findByCodigoAndAtivaTrue("IDEACAO").orElseThrow());
        retorno.setExigeGovernanca(false);
        retorno.setAtiva(true);
        transicoes.saveAndFlush(retorno);
        api(HttpMethod.POST, projeto(id) + "/movimentacoes", tokenA,
                Map.of("transicaoId", retorno.getId(), "justificativa", "Rever hipótese"), 201);
        assertEquals(1, api(HttpMethod.GET, "/api/notificacoes", tokenD, null, 200).path("totalElementos").asInt());
    }

    @Test
    void entregaDoEspecialistaRecebeValidacoesHistoricasDoDemandanteResponsavel() throws Exception {
        long id = cadastrar();
        var entrega = api(HttpMethod.POST, projeto(id) + "/entregas", tokenE, Map.of("descricao", "MVP do piloto"), 201);
        String rota = projeto(id) + "/entregas/" + entrega.path("id").asLong() + "/validacoes";
        var validacao = Map.of("resultado", "VALIDADA_COM_RESSALVAS", "observacoes", "Ajustar o painel");
        api(HttpMethod.POST, rota, tokenOutroD, validacao, 403);
        api(HttpMethod.POST, rota, tokenD, validacao, 201);
        api(HttpMethod.POST, rota, tokenD, Map.of("resultado", "NAO_VALIDADA", "observacoes", "Novo teste falhou"), 201);
        assertEquals(2, api(HttpMethod.GET, projeto(id) + "/validacoes", tokenA, null, 200).path("totalElementos").asInt());
        api(HttpMethod.POST, projeto(id) + "/entregas", tokenD, Map.of("descricao", "MVP"), 403);
    }

    @Test
    void correcaoPreservaAutoriaEExigeDemandanteResponsavel() throws Exception {
        long id = cadastrar();
        var entrada = Map.of("descricao", "Corrigir o contexto", "referencia", "Cadastro", "evidenciaId", evidencia(id));
        api(HttpMethod.POST, projeto(id) + "/correcoes", tokenOutroD, entrada, 403);
        var correcao = api(HttpMethod.POST, projeto(id) + "/correcoes", tokenD, entrada, 201);
        assertEquals(demandante.getId(), correcao.path("autorId").asLong());
        assertEquals(1, api(HttpMethod.GET, projeto(id) + "/correcoes", tokenOutroD, null, 200).path("totalElementos").asInt());
    }

    @Test
    void edicaoArquivamentoEReativacaoPreservamHistoricoEDispendioGlobal() throws Exception {
        long id = cadastrar();
        var consulta = api(HttpMethod.GET, projeto(id), tokenE, null, 200);
        var dados = Map.of("titulo", "Projeto revisado", "descricaoDor", "Dor revisada", "areaDemandanteId", 1,
                "arena", "EFICIENCIA_OPERACIONAL", "dispendioEstimado", 12500.50, "demandanteId", demandante.getId());
        var atualizado = api(HttpMethod.PUT, projeto(id), tokenE,
                Map.of("dados", dados, "versao", consulta.path("versao").asLong(), "justificativa", "Revisão do cadastro"), 200);
        assertEquals(12500.50, api(HttpMethod.GET, projeto(id), tokenOutroD, null, 200).path("dispendioEstimado").asDouble());
        var arquivado = api(HttpMethod.PUT, projeto(id) + "/arquivamento", tokenE,
                Map.of("arquivado", true, "versao", atualizado.path("versao").asLong(), "justificativa", "Pausa"), 200);
        api(HttpMethod.POST, projeto(id) + "/entregas", tokenA, Map.of("descricao", "Entrega"), 422);
        assertTrue(api(HttpMethod.GET, projeto(id), tokenD, null, 200).path("arquivado").asBoolean());
        var arquivados = api(HttpMethod.GET, "/api/projetos?arquivado=true", tokenOutroD, null, 200);
        assertEquals(1, arquivados.path("totalElementos").asInt());
        assertTrue(arquivados.path("conteudo").get(0).path("arquivado").asBoolean());
        api(HttpMethod.PUT, projeto(id) + "/arquivamento", tokenE,
                Map.of("arquivado", false, "versao", arquivado.path("versao").asLong(), "justificativa", "Retomada"), 200);
        assertEquals(3, api(HttpMethod.GET, projeto(id) + "/historico-gestao", tokenD, null, 200).path("totalElementos").asInt());
    }

    @Test
    void arquivamentoExigeVersaoAtualEFechamentoDeGovernanca() throws Exception {
        long id = cadastrar();
        long versao = api(HttpMethod.GET, projeto(id), tokenE, null, 200).path("versao").asLong();
        api(HttpMethod.PUT, projeto(id) + "/arquivamento", tokenE,
                Map.of("arquivado", true, "versao", versao + 1, "justificativa", "Pausa"), 409);
        solicitar(id);
        api(HttpMethod.PUT, projeto(id) + "/arquivamento", tokenE,
                Map.of("arquivado", true, "versao", versao, "justificativa", "Pausa"), 422);
    }

    @Test
    void recomendaConverteUmaVezEImpedeOutroDestinatario() throws Exception {
        var rec = api(HttpMethod.POST, "/api/recomendacoes", tokenA,
                Map.of("destinatarioId", demandante.getId(), "titulo", "Ideia", "descricao", "Problema observado"), 201);
        String rota = "/api/recomendacoes/" + rec.path("id").asLong() + "/conversao";
        api(HttpMethod.POST, rota, tokenOutroD, demanda(), 403);
        var convertida = api(HttpMethod.POST, rota, tokenD, demanda(), 200);
        assertEquals("CONVERTIDA", convertida.path("status").asText());
        long id = convertida.path("projetoId").asLong();
        assertEquals("ESPONTANEA", api(HttpMethod.GET, projeto(id), tokenD, null, 200).path("origem").asText());
        api(HttpMethod.POST, rota, tokenD, demanda(), 422);
    }

    @Test
    void recusaRecomendacaoComJustificativaEPreservaVisibilidadeDosEnvolvidos() throws Exception {
        var rec = api(HttpMethod.POST, "/api/recomendacoes", tokenE,
                Map.of("destinatarioId", demandante.getId(), "titulo", "Ideia", "descricao", "Problema"), 201);
        api(HttpMethod.POST, "/api/recomendacoes/" + rec.path("id").asLong() + "/recusa", tokenD,
                Map.of("justificativa", "Iniciativa já existente"), 200);
        assertEquals(1, api(HttpMethod.GET, "/api/recomendacoes?status=RECUSADA", tokenD, null, 200).path("totalElementos").asInt());
        assertEquals(0, api(HttpMethod.GET, "/api/recomendacoes", tokenOutroD, null, 200).path("totalElementos").asInt());
    }

    @Test
    void especialistaConfiguraColunaEPercursoEProtegeColunaInicial() throws Exception {
        var entrada = Map.of("codigo", "PILOTO_API", "nome", "Piloto", "ordem", 25);
        api(HttpMethod.POST, "/api/kanban/etapas", tokenA, entrada, 403);
        var coluna = api(HttpMethod.POST, "/api/kanban/etapas", tokenE, entrada, 201);
        long ideacao = etapas.findByCodigoAndAtivaTrue("IDEACAO").orElseThrow().getId();
        api(HttpMethod.POST, "/api/kanban/transicoes", tokenE,
                Map.of("origemId", ideacao, "destinoId", coluna.path("id").asLong(), "exigeGovernanca", false), 201);
        api(HttpMethod.PUT, "/api/kanban/etapas/" + ideacao, tokenE,
                Map.of("nome", "Ideação", "ordem", 0, "ativa", false, "versao", 0), 422);
        var consulta = api(HttpMethod.GET, "/api/kanban/configuracao", tokenOutroD, null, 200);
        assertTrue(consulta.path("etapas").isArray());
        assertTrue(consulta.path("transicoes").isArray());
    }

    @Test
    void propostaAprovadaCriaColunaEPropostaCanceladaPermaneceHistorica() throws Exception {
        var entrada = Map.of("proposta", Map.of("codigo", "PROPOSTA_API", "nome", "Piloto proposto", "ordem", 25),
                "justificativa", "Separar o piloto");
        var proposta = api(HttpMethod.POST, "/api/kanban/propostas/colunas", tokenA, entrada, 201);
        api(HttpMethod.POST, "/api/kanban/propostas/" + proposta.path("id").asLong() + "/decisao", tokenE,
                Map.of("decisao", "APROVAR", "justificativa", "Adequado à jornada"), 200);
        assertTrue(etapas.existsByCodigo("PROPOSTA_API"));
        var segunda = api(HttpMethod.POST, "/api/kanban/propostas/colunas", tokenA,
                Map.of("proposta", Map.of("codigo", "CANCELADA_API", "nome", "Coluna cancelada", "ordem", 26), "justificativa", "Revisão"), 201);
        api(HttpMethod.POST, "/api/kanban/propostas/" + segunda.path("id").asLong() + "/cancelamento", tokenA,
                Map.of("justificativa", "Revisar proposta"), 200);
        assertFalse(etapas.existsByCodigo("CANCELADA_API"));
        assertEquals(1, api(HttpMethod.GET, "/api/kanban/propostas?status=CANCELADA", tokenD, null, 200).path("totalElementos").asInt());
    }

    @Test
    void paginacaoERecursoInexistentePossuemContratoHttp() throws Exception {
        String termo = UUID.randomUUID().toString();
        api(HttpMethod.POST, "/api/projetos", tokenD, demanda().put("titulo", "Demanda " + termo), 201);
        var lista = api(HttpMethod.GET, "/api/projetos?page=0&size=250", tokenD, null, 200);
        assertEquals(100, lista.path("tamanho").asInt());
        assertTrue(lista.path("conteudo").isArray());
        assertEquals(1, api(HttpMethod.GET, "/api/projetos?busca=" + termo, tokenE, null, 200).path("totalElementos").asInt());
        assertEquals(0, api(HttpMethod.GET, "/api/projetos?busca=inexistente", tokenD, null, 200).path("totalElementos").asInt());
        api(HttpMethod.GET, "/api/projetos/999999999", tokenD, null, 404);
        api(HttpMethod.GET, "/api/projetos/999999999/qualificacoes", tokenA, null, 404);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void confirmaPersistenciaAposCommitERevogacaoRealDoJwt() throws Exception {
        long id = cadastrar();
        assertTrue(projetos.existsById(id));
        api(HttpMethod.GET, projeto(id), tokenOutroD, null, 200);
        api(HttpMethod.POST, "/api/auth/logout", tokenD, null, 204);
        api(HttpMethod.GET, projeto(id), tokenD, null, 401);
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

    private ObjectNode demanda() {
        return json.createObjectNode().put("titulo", "Demanda API").put("descricaoDor", "Problema da área")
                .put("areaDemandanteId", 1).put("origem", "ESPONTANEA").put("trlInicialDeclarado", 2);
    }

    private long cadastrar() throws Exception {
        return api(HttpMethod.POST, "/api/projetos", tokenD, demanda(), 201).path("id").asLong();
    }

    private long evidencia(long projetoId) {
        var evidencia = new Evidencia();
        evidencia.setProjeto(projetos.findById(projetoId).orElseThrow());
        evidencia.setNomeArquivo("comprovante.pdf");
        evidencia.setTipoArquivo("application/pdf");
        evidencia.setCaminhoStorage("teste/" + UUID.randomUUID());
        return evidencias.saveAndFlush(evidencia).getId();
    }

    private long percursoGovernado() {
        long origem = etapas.findByCodigoAndAtivaTrue("IDEACAO").orElseThrow().getId();
        return transicoes.findByOrigemIdAndAtivaTrue(origem).stream()
                .filter(t -> "POC".equals(t.getDestino().getCodigo())).findFirst().orElseThrow().getId();
    }

    private Map<String, Object> avaliacao(int nivel, long evidencia) {
        return Map.of("nivel", nivel, "justificativa", "Resultados documentados", "evidenciaIds", List.of(evidencia));
    }

    private Map<String, Object> pedido(long transicao, long evidencia) {
        return Map.of("transicaoId", transicao, "justificativa", "Testes concluídos", "evidenciaIds", List.of(evidencia));
    }

    private JsonNode solicitar(long id) throws Exception {
        return api(HttpMethod.POST, projeto(id) + "/solicitacoes-transicao", tokenA, pedido(percursoGovernado(), evidencia(id)), 201);
    }

    private String projeto(long id) { return "/api/projetos/" + id; }
    private String decisao(long id) { return "/api/kanban/solicitacoes-transicao/" + id + "/decisao"; }

    private JsonNode api(HttpMethod metodo, String caminho, String token, Object corpo, int esperado) throws Exception {
        var requisicao = request(metodo, caminho);
        if (token != null) requisicao.header("Authorization", "Bearer " + token);
        if (corpo != null) requisicao.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(corpo));
        var resultado = mvc.perform(requisicao).andExpect(status().is(esperado)).andReturn();
        byte[] resposta = resultado.getResponse().getContentAsByteArray();
        return resposta.length == 0 ? json.nullNode() : json.readTree(resposta);
    }
}
