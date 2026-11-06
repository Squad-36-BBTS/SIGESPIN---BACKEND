package br.com.bbts.sigespin;

import br.com.bbts.sigespin.dto.kanban.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.kanban.EtapaKanbanRepository;
import br.com.bbts.sigespin.repository.evidencia.EvidenciaRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.kanban.SolicitacaoTransicaoRepository;
import br.com.bbts.sigespin.repository.kanban.TransicaoKanbanRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import br.com.bbts.sigespin.service.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static br.com.bbts.sigespin.dto.kanban.DecidirGovernancaRequest.Decisao.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes"
})
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMockUser(username = "analista-kanban@teste.local", roles = "ANALISTA")
class KanbanServiceTests {
    // Histórico imutável exige schema isolado para testes com commit real.
    static final String SCHEMA = "kanban_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) {
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        propriedades.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        propriedades.add("spring.flyway.schemas", () -> SCHEMA);
        propriedades.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Autowired KanbanService kanban;
    @Autowired NotificacaoKanbanService notificacoes;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProjetoRepository projetos;
    @Autowired EtapaKanbanRepository etapas;
    @Autowired TransicaoKanbanRepository transicoes;
    @Autowired EvidenciaRepository evidencias;
    @Autowired SolicitacaoTransicaoRepository solicitacoes;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired JdbcTemplate jdbc;

    TransactionTemplate tx;
    Usuario analista, outroAnalista, especialista, demandante;
    Long origemId, pocId, mvpId, governadaId, livreId;
    Long projetoId, evidenciaId;

    @BeforeAll
    void prepararConfiguracao() {
        tx = new TransactionTemplate(transacoes);
        tx.executeWithoutResult(status -> {
            analista = usuario("analista-kanban@teste.local", PerfilUsuario.ANALISTA);
            outroAnalista = usuario("outro-kanban@teste.local", PerfilUsuario.ANALISTA);
            especialista = usuario("especialista-kanban@teste.local", PerfilUsuario.ESPECIALISTA);
            demandante = usuario("demandante-kanban@teste.local", PerfilUsuario.DEMANDANTE);
            origemId = etapas.findByCodigoAndAtivaTrue("IDEACAO").orElseThrow().getId();
            pocId = etapas.findByCodigoAndAtivaTrue("POC").orElseThrow().getId();
            mvpId = etapas.findByCodigoAndAtivaTrue("MVP").orElseThrow().getId();
            governadaId = transicoes.findByOrigemIdAndAtivaTrue(origemId).stream()
                    .filter(t -> t.getDestino().getId().equals(pocId)).findFirst().orElseThrow().getId();
            var livre = new TransicaoKanban();
            livre.setOrigem(etapas.findById(origemId).orElseThrow());
            livre.setDestino(etapas.findById(mvpId).orElseThrow());
            livre.setAtiva(true);
            livre.setExigeGovernanca(false);
            livreId = transicoes.saveAndFlush(livre).getId();
        });
    }

    @BeforeEach
    void prepararProjeto() {
        tx.executeWithoutResult(status -> {
            var projeto = projeto();
            projetoId = projeto.getId();
            evidenciaId = evidencia(projeto).getId();
        });
    }

    @AfterAll
    void limparSchema() throws Exception {
        try (var c = DriverManager.getConnection(System.getenv("SIGESPIN_TEST_DB_URL"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_PASSWORD", ""));
             var comando = c.createStatement()) {
            comando.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Test
    void analistaMovimentaTransicaoLivreComHistoricoETrlPreservado() {
        var resposta = kanban.movimentar(projetoId, movimento(livreId));
        assertEquals(analista.getId(), resposta.autorId());
        assertEquals(origemId, resposta.origemId());
        assertEquals(mvpId, resposta.destinoId());
        assertNull(resposta.solicitacaoId());
        assertEquals(mvpId, etapaAtual());
        assertEquals(4, projetos.findById(projetoId).orElseThrow().getTrlAtual());
        assertEquals(1, movimentos());
        assertEquals(0, avisos());
    }

    @Test
    void analistaNaoPodeExecutarDiretamenteTransicaoGovernada() {
        assertThrows(RegraNegocioException.class, () -> kanban.movimentar(projetoId, movimento(governadaId)));
        assertEquals(origemId, etapaAtual());
        assertEquals(0, movimentos());
    }

    @Test
    void especialistaMovimentaDiretamenteENotificaTodosOsPerfis() {
        var movimento = como(especialista, () -> kanban.movimentar(projetoId, movimento(governadaId)));
        assertEquals(pocId, etapaAtual());
        assertEquals(1, avisos());
        for (var usuario : List.of(demandante, analista, especialista)) {
            var pagina = como(usuario, () -> notificacoes.listar(0, 100));
            assertTrue(pagina.stream().anyMatch(n -> n.movimentacaoId().equals(movimento.id())
                    && n.mensagem().contains("Justificativa")));
        }
    }

    @Test
    void pedidoPreservaSnapshotEAnexosSemMoverProjeto() {
        var pedido = kanban.solicitar(projetoId, pedido());
        assertEquals(StatusSolicitacao.PENDENTE, pedido.status());
        assertEquals(analista.getId(), pedido.solicitanteId());
        assertEquals(origemId, pedido.origemId());
        assertEquals(pocId, pedido.destinoId());
        assertEquals(origemId, etapaAtual());
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM evidencias_transicao WHERE solicitacao_id=?", Long.class, pedido.id()));
        assertThrows(RegraNegocioException.class, () -> kanban.solicitar(projetoId, pedido()));
        assertEquals(1, solicitacoes.findByProjetoId(projetoId, org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void aprovacaoMoveUmaVezERegistraDecisaoAutorENotificacao() {
        var pedido = kanban.solicitar(projetoId, pedido());
        var resposta = como(especialista, () -> kanban.decidir(pedido.id(), decisao(APROVAR)));
        assertEquals(StatusSolicitacao.APROVADA, resposta.status());
        assertEquals(especialista.getId(), resposta.responsavelEncerramentoId());
        assertNotNull(resposta.encerradoEm());
        assertEquals(pedido.justificativa(), resposta.justificativa());
        assertEquals(pocId, etapaAtual());
        assertEquals(1, movimentos());
        assertEquals(1, avisos());
        assertEquals(pedido.id(), jdbc.queryForObject("SELECT solicitacao_id FROM movimentacoes_kanban WHERE projeto_id=?", Long.class, projetoId));
        assertThrows(RegraNegocioException.class,
                () -> como(especialista, () -> kanban.decidir(pedido.id(), decisao(APROVAR))));
        assertEquals(1, movimentos());
    }

    @Test
    void rejeicaoEncerraPedidoSemMovimento() {
        var pedido = kanban.solicitar(projetoId, pedido());
        var resposta = como(especialista, () -> kanban.decidir(pedido.id(), decisao(REJEITAR)));
        assertEquals(StatusSolicitacao.REJEITADA, resposta.status());
        assertEquals(origemId, etapaAtual());
        assertEquals(0, movimentos());
        assertEquals(0, avisos());
    }

    @Test
    void solicitanteCancelaEPodeAbrirNovoPedido() {
        var pedido = kanban.solicitar(projetoId, pedido());
        var resposta = kanban.cancelar(pedido.id(), new CancelarGovernancaRequest("Revisar evidências"));
        assertEquals(StatusSolicitacao.CANCELADA, resposta.status());
        assertEquals(analista.getId(), resposta.responsavelEncerramentoId());
        assertNotEquals(pedido.id(), kanban.solicitar(projetoId, pedido()).id());
        assertEquals(origemId, etapaAtual());
    }

    @Test
    void outroAnalistaNaoCancelaMasEspecialistaPodeCancelar() {
        var pedido = kanban.solicitar(projetoId, pedido());
        var entrada = new CancelarGovernancaRequest("Revisar pedido");
        assertThrows(AccessDeniedException.class, () -> como(outroAnalista, () -> kanban.cancelar(pedido.id(), entrada)));
        assertEquals(StatusSolicitacao.CANCELADA, como(especialista, () -> kanban.cancelar(pedido.id(), entrada)).status());
    }

    @Test
    void pedidoPendentePrecisaSerEncerradoAntesDeMovimentoDireto() {
        var pedido = kanban.solicitar(projetoId, pedido());
        assertThrows(RegraNegocioException.class,
                () -> como(especialista, () -> kanban.movimentar(projetoId, movimento(livreId))));
        kanban.cancelar(pedido.id(), new CancelarGovernancaRequest("Substituir percurso"));
        como(especialista, () -> kanban.movimentar(projetoId, movimento(livreId)));
        assertEquals(mvpId, etapaAtual());
    }

    @Test
    void falhaAntesDoCommitDesfazDecisaoMovimentoEtapaENotificacao() {
        var pedido = kanban.solicitar(projetoId, pedido());
        assertThrows(IllegalStateException.class, () -> como(especialista, () -> tx.execute(status -> {
            kanban.decidir(pedido.id(), decisao(APROVAR));
            throw new IllegalStateException("Falha simulada antes do commit");
        })));
        assertEquals(StatusSolicitacao.PENDENTE, solicitacoes.findById(pedido.id()).orElseThrow().getStatus());
        assertEquals(origemId, etapaAtual());
        assertEquals(0, movimentos());
        assertEquals(0, avisos());
    }

    @Test
    void validacaoRecusaJustificativaVaziaEEvidenciasInvalidas() {
        assertThrows(ConstraintViolationException.class,
                () -> kanban.movimentar(projetoId, new MovimentarProjetoRequest(livreId, " ")));
        assertThrows(ConstraintViolationException.class,
                () -> kanban.solicitar(projetoId, new SolicitarGovernancaRequest(governadaId, "Justificativa", List.of())));
        Long estrangeira = tx.execute(status -> evidencia(projeto()).getId());
        for (var ids : List.of(List.of(Long.MAX_VALUE), List.of(evidenciaId, evidenciaId), List.of(estrangeira))) {
            assertThrows(RegraNegocioException.class,
                    () -> kanban.solicitar(projetoId, new SolicitarGovernancaRequest(governadaId, "Justificativa", ids)));
        }
        assertFalse(solicitacoes.existsByProjetoIdAndStatus(projetoId, StatusSolicitacao.PENDENTE));
    }

    @Test
    void bloqueiaTransicaoInexistenteOrigemDivergenteEPedidoDesnecessario() {
        assertThrows(RegraNegocioException.class, () -> kanban.movimentar(projetoId, movimento(Long.MAX_VALUE)));
        assertThrows(RegraNegocioException.class, () -> kanban.movimentar(Long.MAX_VALUE, movimento(livreId)));
        assertThrows(RegraNegocioException.class,
                () -> kanban.solicitar(projetoId, new SolicitarGovernancaRequest(livreId, "Justificativa", List.of(evidenciaId))));
        kanban.movimentar(projetoId, movimento(livreId));
        assertThrows(RegraNegocioException.class, () -> kanban.movimentar(projetoId, movimento(livreId)));
        assertEquals(1, movimentos());
    }

    @Test
    void aprovacaoRevalidaConfiguracaoAtualEPermiteRejeitarPedidoObsoleto() {
        var pedido = kanban.solicitar(projetoId, pedido());
        alterarTransicao(governadaId, false);
        try {
            assertThrows(RegraNegocioException.class,
                    () -> como(especialista, () -> kanban.decidir(pedido.id(), decisao(APROVAR))));
            assertEquals(StatusSolicitacao.PENDENTE, solicitacoes.findById(pedido.id()).orElseThrow().getStatus());
            como(especialista, () -> kanban.decidir(pedido.id(), decisao(REJEITAR)));
            assertEquals(origemId, etapaAtual());
        } finally {
            alterarTransicao(governadaId, true);
        }
    }

    @Test
    void etapaInativaImpedeMovimentacao() {
        tx.executeWithoutResult(status -> { var etapa = etapas.findById(mvpId).orElseThrow(); etapa.setAtiva(false); });
        try {
            assertThrows(RegraNegocioException.class, () -> kanban.movimentar(projetoId, movimento(livreId)));
            assertEquals(origemId, etapaAtual());
        } finally {
            tx.executeWithoutResult(status -> { var etapa = etapas.findById(mvpId).orElseThrow(); etapa.setAtiva(true); });
        }
    }

    @Test
    @WithMockUser(username = "demandante-kanban@teste.local", roles = "DEMANDANTE")
    void demandanteNaoExecutaOperacoesDeEscrita() {
        assertThrows(AccessDeniedException.class, () -> kanban.movimentar(projetoId, movimento(livreId)));
        assertThrows(AccessDeniedException.class, () -> kanban.solicitar(projetoId, pedido()));
        assertThrows(AccessDeniedException.class, () -> kanban.decidir(1L, decisao(APROVAR)));
        assertThrows(AccessDeniedException.class, () -> kanban.cancelar(1L, new CancelarGovernancaRequest("Motivo")));
    }

    @Test
    void analistaNaoDecideESpecialistaNaoSolicitaEmNomeDoAnalista() {
        var pedido = kanban.solicitar(projetoId, pedido());
        assertThrows(AccessDeniedException.class, () -> kanban.decidir(pedido.id(), decisao(APROVAR)));
        assertThrows(AccessDeniedException.class, () -> como(especialista, () -> kanban.solicitar(projetoId, pedido())));
    }

    @Test
    void notificacoesValidamPaginacaoEUsuarioAtivo() {
        assertThrows(ConstraintViolationException.class, () -> notificacoes.listar(-1, 10));
        assertThrows(ConstraintViolationException.class, () -> notificacoes.listar(0, 101));
        tx.executeWithoutResult(status -> usuarios.findById(outroAnalista.getId()).orElseThrow().setAtivo(false));
        try {
            assertThrows(AccessDeniedException.class, () -> como(outroAnalista, () -> notificacoes.listar(0, 10)));
        } finally {
            tx.executeWithoutResult(status -> usuarios.findById(outroAnalista.getId()).orElseThrow().setAtivo(true));
        }
    }

    @Test
    void movimentosConcorrentesGravamUmaUnicaMudanca() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(2);
        try {
            Callable<Boolean> operacao = () -> {
                inicio.countDown();
                if (!inicio.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Threads não iniciaram");
                try {
                    como(especialista, () -> kanban.movimentar(projetoId, movimento(governadaId)));
                    return true;
                } catch (RegraNegocioException recusada) {
                    return false;
                }
            };
            var a = executor.submit(operacao);
            var b = executor.submit(operacao);
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(1, movimentos());
            assertEquals(1, avisos());
            assertEquals(pocId, etapaAtual());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void decisoesConcorrentesEncerramUmaUnicaVez() throws Exception {
        var pedido = kanban.solicitar(projetoId, pedido());
        var executor = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(2);
        try {
            var a = executor.submit(() -> decidirEmOutraThread(pedido.id(), APROVAR, inicio));
            var b = executor.submit(() -> decidirEmOutraThread(pedido.id(), REJEITAR, inicio));
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            var status = solicitacoes.findById(pedido.id()).orElseThrow().getStatus();
            assertTrue(status == StatusSolicitacao.APROVADA || status == StatusSolicitacao.REJEITADA);
            assertEquals(status == StatusSolicitacao.APROVADA ? pocId : origemId, etapaAtual());
            assertEquals(status == StatusSolicitacao.APROVADA ? 1 : 0, movimentos());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean decidirEmOutraThread(Long id, DecidirGovernancaRequest.Decisao decisao,
            CountDownLatch inicio) throws Exception {
        inicio.countDown();
        if (!inicio.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Threads não iniciaram");
        try {
            como(especialista, () -> kanban.decidir(id, decisao(decisao)));
            return true;
        } catch (RegraNegocioException recusada) {
            return false;
        }
    }

    private <T> T como(Usuario usuario, Supplier<T> operacao) {
        var anterior = SecurityContextHolder.getContext();
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(usuario.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()))));
        SecurityContextHolder.setContext(contexto);
        try {
            return operacao.get();
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }

    private void alterarTransicao(Long id, boolean ativa) {
        tx.executeWithoutResult(status -> transicoes.findById(id).orElseThrow().setAtiva(ativa));
    }

    private long movimentos() {
        return jdbc.queryForObject("SELECT count(*) FROM movimentacoes_kanban WHERE projeto_id=?", Long.class, projetoId);
    }

    private long avisos() {
        return jdbc.queryForObject("SELECT count(*) FROM notificacoes_kanban n JOIN movimentacoes_kanban m ON m.id=n.movimentacao_id WHERE m.projeto_id=?", Long.class, projetoId);
    }

    private Long etapaAtual() {
        return jdbc.queryForObject("SELECT etapa_id FROM projetos WHERE id=?", Long.class, projetoId);
    }

    private MovimentarProjetoRequest movimento(Long id) {
        return new MovimentarProjetoRequest(id, "Justificativa do movimento");
    }

    private SolicitarGovernancaRequest pedido() {
        return new SolicitarGovernancaRequest(governadaId, "Evidências do piloto", List.of(evidenciaId));
    }

    private DecidirGovernancaRequest decisao(DecidirGovernancaRequest.Decisao decisao) {
        return new DecidirGovernancaRequest(decisao, "Decisão fundamentada");
    }

    private Usuario usuario(String email, PerfilUsuario perfil) {
        var usuario = new Usuario();
        usuario.setNome("Teste Kanban");
        usuario.setEmail(email);
        usuario.setSenhaHash("hash-nao-usado");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarios.saveAndFlush(usuario);
    }

    private Projeto projeto() {
        var projeto = new Projeto();
        projeto.setTitulo("Piloto Kanban");
        projeto.setDescricaoDor("Problema");
        projeto.setAreaDemandanteId(1L);
        projeto.setResponsavelPo(demandante);
        projeto.setDemandante(demandante);
        projeto.setArena(ArenaTipo.A_DEFINIR);
        projeto.setTrlAtual(4);
        projeto.setOrigem(OrigemDemanda.ESPONTANEA);
        projeto.setEtapa(etapas.findById(origemId).orElseThrow());
        return projetos.saveAndFlush(projeto);
    }

    private Evidencia evidencia(Projeto projeto) {
        var evidencia = new Evidencia();
        evidencia.setProjeto(projeto);
        evidencia.setNomeArquivo("piloto.pdf");
        evidencia.setCaminhoStorage("teste/" + UUID.randomUUID());
        evidencia.setTipoArquivo("application/pdf");
        return evidencias.saveAndFlush(evidencia);
    }
}
