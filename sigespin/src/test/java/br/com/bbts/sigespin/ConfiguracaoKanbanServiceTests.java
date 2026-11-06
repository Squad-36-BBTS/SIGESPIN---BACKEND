package br.com.bbts.sigespin;

import br.com.bbts.sigespin.dto.kanban.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.kanban.EtapaKanbanRepository;
import br.com.bbts.sigespin.repository.evidencia.EvidenciaRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.kanban.SolicitacaoConfiguracaoKanbanRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import br.com.bbts.sigespin.service.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WithMockUser(username = "config-especialista@teste.local", roles = "ESPECIALISTA")
class ConfiguracaoKanbanServiceTests {
    // Commits reais validam a criação junto com o encerramento e as proteções SQL da V7.
    static final String SCHEMA = "config_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        registro.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registro.add("spring.flyway.schemas", () -> SCHEMA);
        registro.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Autowired ConfiguracaoKanbanService configuracao;
    @Autowired PedidoConfiguracaoKanbanService pedidos;
    @Autowired SolicitacaoConfiguracaoKanbanRepository repositorioPedidos;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProjetoRepository projetos;
    @Autowired EtapaKanbanRepository etapas;
    @Autowired EvidenciaRepository evidencias;
    @Autowired KanbanService kanban;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transacoes;

    TransactionTemplate tx;
    Usuario especialista, analista, outroAnalista, demandante;
    ColunaConfiguradaResponse origem, destino;

    @BeforeAll
    void prepararUsuarios() {
        tx = new TransactionTemplate(transacoes);
        tx.executeWithoutResult(status -> {
            especialista = usuario("config-especialista@teste.local", PerfilUsuario.ESPECIALISTA);
            analista = usuario("config-analista@teste.local", PerfilUsuario.ANALISTA);
            outroAnalista = usuario("config-outro@teste.local", PerfilUsuario.ANALISTA);
            demandante = usuario("config-demandante@teste.local", PerfilUsuario.DEMANDANTE);
        });
    }

    @BeforeEach
    void prepararColunas() {
        origem = configuracao.criarColuna(coluna());
        destino = configuracao.criarColuna(coluna());
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
    void criaEditaERelistaColunaGlobalComVersaoAtualizada() {
        var editada = configuracao.editarColuna(origem.id(), new EditarColunaRequest("Novo nome", 77, true, origem.versao()));
        assertEquals(origem.codigo(), editada.codigo());
        assertEquals("Novo nome", editada.nome());
        assertTrue(editada.versao() > origem.versao());
        for (var usuario : List.of(demandante, analista, especialista)) {
            assertTrue(como(usuario, configuracao::listarColunas).stream().anyMatch(e -> e.id().equals(origem.id())));
        }
        assertThrows(RegraNegocioException.class, () -> configuracao.editarColuna(origem.id(),
                new EditarColunaRequest("Edição antiga", 1, true, origem.versao())));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE etapas_kanban SET codigo='CODIGO_ALTERADO' WHERE id=?", origem.id()));
    }

    @Test
    void desativaColunaLivreEProtegeInicialColunasOcupadasERotasAtivas() {
        assertFalse(configuracao.editarColuna(destino.id(), new EditarColunaRequest(destino.nome(), 1, false, destino.versao())).ativa());
        var inicial = configuracao.listarColunas().stream().filter(e -> e.codigo().equals("IDEACAO")).findFirst().orElseThrow();
        assertThrows(RegraNegocioException.class, () -> configuracao.editarColuna(inicial.id(),
                new EditarColunaRequest(inicial.nome(), 0, false, inicial.versao())));
        tx.executeWithoutResult(status -> projeto(origem.id()));
        assertThrows(RegraNegocioException.class, () -> configuracao.editarColuna(origem.id(),
                new EditarColunaRequest(origem.nome(), 1, false, origem.versao())));
        var outra = configuracao.criarColuna(coluna());
        configuracao.criarPercurso(new CriarPercursoRequest(origem.id(), outra.id(), false));
        assertThrows(RegraNegocioException.class, () -> configuracao.editarColuna(outra.id(),
                new EditarColunaRequest(outra.nome(), 1, false, outra.versao())));
    }

    @Test
    void criaEditaDesativaPercursoERejeitaVersaoAntiga() {
        var percurso = configuracao.criarPercurso(percurso());
        var editado = configuracao.editarPercurso(percurso.id(), new EditarPercursoRequest(true, true, percurso.versao()));
        assertTrue(editado.exigeGovernanca());
        assertThrows(RegraNegocioException.class, () -> configuracao.editarPercurso(percurso.id(),
                new EditarPercursoRequest(false, true, percurso.versao())));
        var inativo = configuracao.editarPercurso(percurso.id(), new EditarPercursoRequest(true, false, editado.versao()));
        assertFalse(inativo.ativa());
        assertTrue(configuracao.listarPercursos().stream().anyMatch(t -> t.id().equals(percurso.id())));
    }

    @Test
    void recusaDuplicidadesOrigemTerminalEtapasIguaisInativasOuInexistentes() {
        assertThrows(RegraNegocioException.class, () -> configuracao.criarColuna(
                new CriarColunaRequest(origem.codigo(), "Duplicada", 1, null)));
        configuracao.criarPercurso(percurso());
        assertThrows(RegraNegocioException.class, () -> configuracao.criarPercurso(percurso()));
        assertThrows(RegraNegocioException.class, () -> configuracao.criarPercurso(
                new CriarPercursoRequest(origem.id(), origem.id(), false)));
        assertThrows(RegraNegocioException.class, () -> configuracao.criarPercurso(
                new CriarPercursoRequest(origem.id(), Long.MAX_VALUE, false)));
        var terminal = configuracao.criarColuna(new CriarColunaRequest(codigo(), "Fim", 50, TipoTerminal.CONCLUSAO));
        assertThrows(RegraNegocioException.class, () -> configuracao.criarPercurso(
                new CriarPercursoRequest(terminal.id(), origem.id(), false)));
        var inativa = configuracao.criarColuna(coluna());
        configuracao.editarColuna(inativa.id(), new EditarColunaRequest(inativa.nome(), 1, false, inativa.versao()));
        assertThrows(RegraNegocioException.class, () -> configuracao.criarPercurso(
                new CriarPercursoRequest(origem.id(), inativa.id(), false)));
    }

    @Test
    void analistaEDemandanteNaoConfiguramDiretamente() {
        for (var usuario : List.of(analista, demandante)) {
            assertThrows(AccessDeniedException.class, () -> como(usuario, () -> configuracao.criarColuna(coluna())));
            assertThrows(AccessDeniedException.class, () -> como(usuario, () -> configuracao.criarPercurso(percurso())));
            assertThrows(AccessDeniedException.class, () -> como(usuario, () -> configuracao.editarColuna(origem.id(),
                    new EditarColunaRequest("Nome", 1, true, origem.versao()))));
        }
        assertThrows(AccessDeniedException.class, () -> pedidos.solicitarColuna(new SolicitarColunaRequest(coluna(), "Motivo")));
        assertThrows(AccessDeniedException.class, () -> como(demandante,
                () -> pedidos.solicitarPercurso(new SolicitarPercursoRequest(percurso(), "Motivo"))));
    }

    @Test
    void validaCamposDaConfiguracaoEDaProposta() {
        assertThrows(ConstraintViolationException.class, () -> configuracao.criarColuna(
                new CriarColunaRequest("codigo invalido", "", -1, null)));
        assertThrows(ConstraintViolationException.class, () -> configuracao.criarPercurso(
                new CriarPercursoRequest(null, -1L, null)));
        assertThrows(ConstraintViolationException.class, () -> como(analista,
                () -> pedidos.solicitarColuna(new SolicitarColunaRequest(coluna(), " "))));
        assertThrows(ConstraintViolationException.class, () -> pedidos.listar(StatusSolicitacao.PENDENTE, 0, 101));
    }

    @Test
    void aprovaColunaPropostaSemAlterarPropostaOriginal() {
        var proposta = coluna();
        var pedido = como(analista, () -> pedidos.solicitarColuna(new SolicitarColunaRequest(proposta, "Precisa de piloto")));
        assertFalse(etapas.existsByCodigo(proposta.codigo()));
        assertEquals(StatusSolicitacao.PENDENTE, pedido.status());
        var aprovada = pedidos.decidir(pedido.id(), decisao(APROVAR));
        assertEquals(StatusSolicitacao.APROVADA, aprovada.status());
        assertEquals(analista.getId(), aprovada.solicitanteId());
        assertEquals(especialista.getId(), aprovada.responsavelEncerramentoId());
        assertEquals(proposta, aprovada.coluna());
        assertNotNull(aprovada.colunaCriadaId());
        assertTrue(etapas.existsByCodigo(proposta.codigo()));
        assertThrows(RegraNegocioException.class, () -> pedidos.decidir(pedido.id(), decisao(APROVAR)));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE solicitacoes_configuracao_kanban SET nome='Alterada' WHERE id=?", pedido.id()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "DELETE FROM solicitacoes_configuracao_kanban WHERE id=?", pedido.id()));
    }

    @Test
    void percursoAprovadoPodeSerUsadoPeloCasoDeMovimentacao() {
        var pedido = como(analista, () -> pedidos.solicitarPercurso(new SolicitarPercursoRequest(percurso(), "Novo caminho")));
        var aprovado = pedidos.decidir(pedido.id(), decisao(APROVAR));
        assertNotNull(aprovado.transicaoCriadaId());
        Long projetoId = tx.execute(status -> projeto(origem.id()).getId());
        var movimento = como(analista, () -> kanban.movimentar(projetoId,
                new MovimentarProjetoRequest(aprovado.transicaoCriadaId(), "Experimento realizado")));
        assertEquals(destino.id(), movimento.destinoId());
    }

    @Test
    void rejeicaoECancelamentoPreservamPedidoSemCriarConfiguracao() {
        var proposta = coluna();
        var pedido = como(analista, () -> pedidos.solicitarColuna(new SolicitarColunaRequest(proposta, "Motivo")));
        assertThrows(AccessDeniedException.class, () -> como(outroAnalista,
                () -> pedidos.cancelar(pedido.id(), new CancelarGovernancaRequest("Cancelar"))));
        assertThrows(AccessDeniedException.class, () -> como(analista, () -> pedidos.decidir(pedido.id(), decisao(APROVAR))));
        var cancelado = como(analista, () -> pedidos.cancelar(pedido.id(), new CancelarGovernancaRequest("Cancelar")));
        assertEquals(StatusSolicitacao.CANCELADA, cancelado.status());
        assertFalse(etapas.existsByCodigo(proposta.codigo()));
        var segundo = como(analista, () -> pedidos.solicitarColuna(new SolicitarColunaRequest(proposta, "Outro pedido")));
        assertEquals(StatusSolicitacao.REJEITADA, pedidos.decidir(segundo.id(), decisao(REJEITAR)).status());
        assertFalse(etapas.existsByCodigo(proposta.codigo()));
    }

    @Test
    void pedidoObsoletoContinuaPendenteAteRejeicao() {
        var proposta = coluna();
        var pedido = como(analista, () -> pedidos.solicitarColuna(new SolicitarColunaRequest(proposta, "Motivo")));
        configuracao.criarColuna(proposta);
        assertThrows(RegraNegocioException.class, () -> pedidos.decidir(pedido.id(), decisao(APROVAR)));
        assertEquals(StatusSolicitacao.PENDENTE, repositorioPedidos.findById(pedido.id()).orElseThrow().getStatus());
        assertEquals(StatusSolicitacao.REJEITADA, pedidos.decidir(pedido.id(), decisao(REJEITAR)).status());
    }

    @Test
    void aprovacaoRevalidaEtapasDoPercurso() {
        var pedido = como(analista, () -> pedidos.solicitarPercurso(new SolicitarPercursoRequest(percurso(), "Motivo")));
        configuracao.editarColuna(destino.id(), new EditarColunaRequest(destino.nome(), 1, false, destino.versao()));
        assertThrows(RegraNegocioException.class, () -> pedidos.decidir(pedido.id(), decisao(APROVAR)));
        assertEquals(StatusSolicitacao.PENDENTE, repositorioPedidos.findById(pedido.id()).orElseThrow().getStatus());
    }

    @Test
    void falhaAntesDoCommitDesfazCriacaoEEncerramentoJuntos() {
        var proposta = coluna();
        var pedido = como(analista, () -> pedidos.solicitarColuna(new SolicitarColunaRequest(proposta, "Motivo")));
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            pedidos.decidir(pedido.id(), decisao(APROVAR));
            throw new IllegalStateException("Falha antes do commit");
        }));
        assertFalse(etapas.existsByCodigo(proposta.codigo()));
        assertEquals(StatusSolicitacao.PENDENTE, repositorioPedidos.findById(pedido.id()).orElseThrow().getStatus());
    }

    @Test
    void pedidosDeMovimentoPendentesProtegemConfiguracaoDoPercurso() {
        var percurso = configuracao.criarPercurso(new CriarPercursoRequest(origem.id(), destino.id(), true));
        Long projetoId = tx.execute(status -> projeto(origem.id()).getId());
        Long arquivoId = tx.execute(status -> {
            var evidencia = new Evidencia();
            evidencia.setProjeto(projetos.findById(projetoId).orElseThrow());
            evidencia.setNomeArquivo("piloto.pdf");
            evidencia.setCaminhoStorage("teste/" + UUID.randomUUID());
            evidencia.setTipoArquivo("application/pdf");
            return evidencias.saveAndFlush(evidencia).getId();
        });
        var pedido = como(analista, () -> kanban.solicitar(projetoId,
                new SolicitarGovernancaRequest(percurso.id(), "Provas", List.of(arquivoId))));
        assertThrows(RegraNegocioException.class, () -> configuracao.editarPercurso(percurso.id(),
                new EditarPercursoRequest(false, false, percurso.versao())));
        kanban.cancelar(pedido.id(), new CancelarGovernancaRequest("Rever percurso"));
        assertFalse(configuracao.editarPercurso(percurso.id(), new EditarPercursoRequest(false, false, percurso.versao())).ativa());
    }

    @Test
    void todosOsPerfisConsultamPedidosGlobais() {
        var pedido = como(analista, () -> pedidos.solicitarColuna(new SolicitarColunaRequest(coluna(), "Motivo")));
        for (var usuario : List.of(analista, demandante, especialista)) {
            assertTrue(como(usuario, () -> pedidos.listar(StatusSolicitacao.PENDENTE, 0, 100))
                    .stream().anyMatch(p -> p.id().equals(pedido.id())));
        }
    }

    @Test
    void duasDecisoesConcorrentesAplicamUmaUnicaConfiguracao() throws Exception {
        var proposta = coluna();
        var pedido = como(analista, () -> pedidos.solicitarColuna(new SolicitarColunaRequest(proposta, "Motivo")));
        var inicio = new CountDownLatch(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> operacao = () -> executarConcorrente(inicio,
                    () -> pedidos.decidir(pedido.id(), decisao(APROVAR)));
            var a = executor.submit(operacao);
            var b = executor.submit(operacao);
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM etapas_kanban WHERE codigo=?", Long.class, proposta.codigo()));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void duasCriacoesConcorrentesRespeitamCodigoUnico() throws Exception {
        var proposta = coluna();
        var inicio = new CountDownLatch(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> operacao = () -> executarConcorrente(inicio, () -> configuracao.criarColuna(proposta));
            var a = executor.submit(operacao);
            var b = executor.submit(operacao);
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM etapas_kanban WHERE codigo=?", Long.class, proposta.codigo()));
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean executarConcorrente(CountDownLatch inicio, Supplier<?> operacao) throws Exception {
        inicio.countDown();
        if (!inicio.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Threads não iniciaram");
        try {
            como(especialista, operacao);
            return true;
        } catch (RegraNegocioException esperado) {
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

    private CriarColunaRequest coluna() {
        return new CriarColunaRequest(codigo(), "Coluna de teste", 10, null);
    }

    private String codigo() {
        return "COLUNA_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT);
    }

    private CriarPercursoRequest percurso() {
        return new CriarPercursoRequest(origem.id(), destino.id(), false);
    }

    private DecidirGovernancaRequest decisao(DecidirGovernancaRequest.Decisao decisao) {
        return new DecidirGovernancaRequest(decisao, "Decisão fundamentada");
    }

    private Usuario usuario(String email, PerfilUsuario perfil) {
        var usuario = new Usuario();
        usuario.setNome("Teste configuração");
        usuario.setEmail(email);
        usuario.setSenhaHash("hash-nao-usado");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarios.saveAndFlush(usuario);
    }

    private Projeto projeto(Long etapaId) {
        var projeto = new Projeto();
        projeto.setTitulo("Projeto em coluna configurada");
        projeto.setDescricaoDor("Problema");
        projeto.setAreaDemandanteId(1L);
        projeto.setResponsavelPo(demandante);
        projeto.setDemandante(demandante);
        projeto.setArena(ArenaTipo.A_DEFINIR);
        projeto.setOrigem(OrigemDemanda.ESPONTANEA);
        projeto.setTrlAtual(4);
        projeto.setEtapa(etapas.findById(etapaId).orElseThrow());
        return projetos.saveAndFlush(projeto);
    }
}
