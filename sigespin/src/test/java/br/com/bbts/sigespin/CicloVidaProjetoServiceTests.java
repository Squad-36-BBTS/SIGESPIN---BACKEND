package br.com.bbts.sigespin;

import org.springframework.data.domain.PageRequest;
import br.com.bbts.sigespin.dto.demanda.RegistrarDemandaRequest;
import br.com.bbts.sigespin.dto.entrega.*;
import br.com.bbts.sigespin.dto.gestao.*;
import br.com.bbts.sigespin.dto.kanban.*;
import br.com.bbts.sigespin.dto.consulta.ConsultarEspecialistaRequest;
import br.com.bbts.sigespin.dto.parecer.RegistrarParecerRequest;
import br.com.bbts.sigespin.dto.qualificacao.RegistrarQualificacaoRequest;
import br.com.bbts.sigespin.dto.trl.RegistrarAvaliacaoTrlRequest;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.kanban.EtapaKanbanRepository;
import br.com.bbts.sigespin.repository.evidencia.EvidenciaRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.portfolio.RecomendacaoDemandaRepository;
import br.com.bbts.sigespin.repository.kanban.TransicaoKanbanRepository;
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

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

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
@WithMockUser(username = "ciclo-analista@teste.local", roles = "ANALISTA")
class CicloVidaProjetoServiceTests {
    static final String SCHEMA = "ciclo_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        registro.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registro.add("spring.flyway.schemas", () -> SCHEMA);
        registro.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Autowired EntregaService entregas;
    @Autowired CorrecaoDemandaService correcoes;
    @Autowired GestaoProjetoService gestao;
    @Autowired ConsultaProjetoService portfolio;
    @Autowired RecomendacaoDemandaService recomendacoes;
    @Autowired RecomendacaoDemandaRepository repositorioRecomendacoes;
    @Autowired QualificacaoDemandaService qualificacoes;
    @Autowired ConsultaEspecialistaService consultas;
    @Autowired ParecerTecnicoService pareceres;
    @Autowired AvaliacaoTrlService trl;
    @Autowired KanbanService kanban;
    @Autowired ProjetoRepository projetos;
    @Autowired UsuarioRepository usuarios;
    @Autowired EvidenciaRepository evidencias;
    @Autowired EtapaKanbanRepository etapas;
    @Autowired TransicaoKanbanRepository transicoes;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired JdbcTemplate jdbc;

    TransactionTemplate tx;
    Usuario analista, especialista, dono, outroDemandante;
    Long projetoId, evidenciaId, governadaId;

    @BeforeAll
    void prepararUsuarios() {
        tx = new TransactionTemplate(transacoes);
        tx.executeWithoutResult(status -> {
            analista = usuario("ciclo-analista@teste.local", PerfilUsuario.ANALISTA);
            especialista = usuario("ciclo-especialista@teste.local", PerfilUsuario.ESPECIALISTA);
            dono = usuario("ciclo-dono@teste.local", PerfilUsuario.DEMANDANTE);
            outroDemandante = usuario("ciclo-outro@teste.local", PerfilUsuario.DEMANDANTE);
            var etapa = etapas.findByCodigoAndAtivaTrue("IDEACAO").orElseThrow();
            governadaId = transicoes.findByOrigemIdAndAtivaTrue(etapa.getId()).stream()
                    .filter(t -> t.getDestino().getCodigo().equals("POC")).findFirst().orElseThrow().getId();
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
    void analistaEEspecialistaSubmetemNovasEntregas() {
        var primeira = entregas.submeterEntrega(projetoId, new EntregaProjetoRequest(" Piloto inicial "));
        var segunda = como(especialista, () -> entregas.submeterEntrega(projetoId, new EntregaProjetoRequest("MVP revisado")));
        assertNotEquals(primeira.id(), segunda.id());
        assertEquals("Piloto inicial", primeira.descricao());
        assertEquals(analista.getId(), primeira.autor().id());
        assertEquals(especialista.getId(), segunda.autor().id());
        assertEquals(2, entregas.listarEntregasPorProjeto(projetoId, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(4, portfolio.detalhar(projetoId).trlAtual());
    }

    @Test
    void donoValidaSucessivamenteSemApagarResultados() {
        var entrega = entregas.submeterEntrega(projetoId, new EntregaProjetoRequest("MVP"));
        for (var resultado : ResultadoValidacao.values()) {
            var validacao = como(dono, () -> entregas.registrarValidacao(projetoId, entrega.id(),
                    new ValidacaoEntregaRequest(resultado, "Observação do dono do problema")));
            assertEquals(dono.getId(), validacao.demandante().id());
        }
        for (var usuario : List.of(dono, analista, especialista)) {
            assertEquals(3, como(usuario, () -> entregas.listarValidacoesPorProjeto(projetoId, PageRequest.of(0, 10))).getTotalElements());
        }
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE validacoes_entrega SET observacoes='Apagada' WHERE projeto_id=?", projetoId));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "DELETE FROM entregas_projeto WHERE id=?", entrega.id()));
    }

    @Test
    void validaResponsavelEProjetoDaEntrega() {
        var entrega = entregas.submeterEntrega(projetoId, new EntregaProjetoRequest("MVP"));
        var entrada = new ValidacaoEntregaRequest(ResultadoValidacao.VALIDADA, "Aceito");
        assertThrows(AccessDeniedException.class, () -> como(outroDemandante,
                () -> entregas.registrarValidacao(projetoId, entrega.id(), entrada)));
        assertThrows(AccessDeniedException.class, () -> entregas.registrarValidacao(projetoId, entrega.id(), entrada));
        Long outroProjetoId = tx.execute(status -> projeto().getId());
        assertThrows(RegraNegocioException.class, () -> como(dono,
                () -> entregas.registrarValidacao(outroProjetoId, entrega.id(), entrada)));
        assertThrows(AccessDeniedException.class, () -> como(dono,
                () -> entregas.submeterEntrega(projetoId, new EntregaProjetoRequest("MVP"))));
    }

    @Test
    void validaCamposObrigatoriosELimitesDePaginacao() {
        assertThrows(ConstraintViolationException.class, () -> entregas.submeterEntrega(projetoId, new EntregaProjetoRequest(" ")));
        assertThrows(ConstraintViolationException.class, () -> como(dono,
                () -> correcoes.registrar(projetoId, new RegistrarCorrecaoRequest(" ", null, null))));
        assertThrows(ConstraintViolationException.class, () -> portfolio.listar(null, null, null, null, -1, 10));
        assertEquals(100, entregas.listarEntregasPorProjeto(projetoId, PageRequest.of(0, 101)).getSize(),
                "página acima de 100 é limitada a 100");
    }

    @Test
    void correcaoAcrescentaHistoricoEPreservaArquivoReferenciado() {
        var tituloAntes = portfolio.detalhar(projetoId).dados().titulo();
        var correcao = como(dono, () -> correcoes.registrar(projetoId,
                new RegistrarCorrecaoRequest("Documento complementar", "Evidência do piloto", evidenciaId)));
        assertEquals(dono.getId(), correcao.autorId());
        assertEquals(evidenciaId, correcao.evidenciaId());
        assertEquals(tituloAntes, portfolio.detalhar(projetoId).dados().titulo());
        assertEquals(1, correcoes.listar(projetoId, 0, 10).getTotalElements());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE correcoes_demanda SET descricao='Outra' WHERE id=?", correcao.id()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE evidencias SET nome_arquivo='trocado.pdf' WHERE id=?", evidenciaId));
    }

    @Test
    void correcaoRecusaOutroDonoEArquivoEstrangeiro() {
        assertThrows(AccessDeniedException.class, () -> como(outroDemandante,
                () -> correcoes.registrar(projetoId, new RegistrarCorrecaoRequest("Correção", null, null))));
        assertThrows(AccessDeniedException.class,
                () -> correcoes.registrar(projetoId, new RegistrarCorrecaoRequest("Correção", null, null)));
        Long estrangeira = tx.execute(status -> evidencia(projeto()).getId());
        assertThrows(RegraNegocioException.class, () -> como(dono,
                () -> correcoes.registrar(projetoId, new RegistrarCorrecaoRequest("Correção", null, estrangeira))));
    }

    @Test
    void especialistaEditaComAuditoriaEDispendioVisivelATodos() {
        var anterior = portfolio.detalhar(projetoId);
        var atualizado = como(especialista, () -> gestao.atualizar(projetoId,
                new AtualizarProjetoRequest(dados("Título corrigido", analista.getId()), anterior.versao(), "Revisão cadastral")));
        assertEquals("Título corrigido", atualizado.dados().titulo());
        assertTrue(atualizado.versao() > anterior.versao());
        assertEquals(analista.getId(), atualizado.dados().analistaResponsavelId());
        var historico = gestao.historico(projetoId, 0, 10).getContent().getFirst();
        assertEquals(TipoAlteracaoProjeto.CADASTRO, historico.tipo());
        assertEquals(especialista.getId(), historico.autorId());
        assertNotEquals(historico.anterior(), historico.atual());
        for (var usuario : List.of(dono, outroDemandante, analista, especialista)) {
            assertEquals(new BigDecimal("123.45"), como(usuario, () -> portfolio.detalhar(projetoId)).dados().dispendioEstimado());
        }
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "DELETE FROM alteracoes_projeto WHERE projeto_id=?", projetoId));
    }

    @Test
    void edicaoRecusaVersaoAntigaResponsavelInvalidoEPerfilSemAlcada() {
        var anterior = portfolio.detalhar(projetoId);
        var entrada = new AtualizarProjetoRequest(dados("Novo título", analista.getId()), anterior.versao(), "Ajuste");
        assertThrows(AccessDeniedException.class, () -> gestao.atualizar(projetoId, entrada));
        como(especialista, () -> gestao.atualizar(projetoId, entrada));
        assertThrows(RegraNegocioException.class, () -> como(especialista, () -> gestao.atualizar(projetoId, entrada)));
        var atual = portfolio.detalhar(projetoId);
        assertThrows(RegraNegocioException.class, () -> como(especialista, () -> gestao.atualizar(projetoId,
                new AtualizarProjetoRequest(dados("Outro título", dono.getId()), atual.versao(), "Ajuste"))));
    }

    @Test
    void arquivamentoBloqueiaTodosOsFluxosDeEscritaEPreservaLeitura() {
        var entrega = entregas.submeterEntrega(projetoId, new EntregaProjetoRequest("MVP"));
        var arquivado = arquivar(true);
        assertTrue(arquivado.arquivado());
        assertNotNull(arquivado.arquivadoEm());
        assertEquals(especialista.getId(), arquivado.arquivadoPorId());
        assertEquals(1, portfolio.listar(null, null, arquivado.dados().titulo(), true, 0, 10).getTotalElements());
        assertEquals(0, portfolio.listar(null, null, arquivado.dados().titulo(), false, 0, 10).getTotalElements());
        assertThrows(RegraNegocioException.class, () -> entregas.submeterEntrega(projetoId, new EntregaProjetoRequest("Outro MVP")));
        assertThrows(RegraNegocioException.class, () -> qualificacoes.registrar(projetoId,
                new RegistrarQualificacaoRequest("Tese", "Relevância", "Inovação", "Viável", "TRL", "Fundamento", "Conclusão")));
        assertThrows(RegraNegocioException.class, () -> consultas.consultar(projetoId,
                new ConsultarEspecialistaRequest(especialista.getId(), "Questão")));
        assertThrows(RegraNegocioException.class, () -> como(especialista, () -> pareceres.registrar(projetoId,
                new RegistrarParecerRequest(null, "Fundamento", "Conclusão", List.of()))));
        assertThrows(RegraNegocioException.class, () -> trl.registrar(projetoId,
                new RegistrarAvaliacaoTrlRequest(5, "Piloto", List.of(evidenciaId))));
        assertThrows(RegraNegocioException.class, () -> como(especialista, () -> kanban.movimentar(projetoId,
                new MovimentarProjetoRequest(governadaId, "Avanço"))));
        assertThrows(RegraNegocioException.class, () -> como(dono, () -> correcoes.registrar(projetoId,
                new RegistrarCorrecaoRequest("Correção", null, null))));
        assertThrows(RegraNegocioException.class, () -> como(dono, () -> entregas.registrarValidacao(projetoId, entrega.id(),
                new ValidacaoEntregaRequest(ResultadoValidacao.VALIDADA, "Aceito"))));
        assertEquals(1, como(outroDemandante, () -> entregas.listarEntregasPorProjeto(projetoId, PageRequest.of(0, 10))).getTotalElements());
        var reativado = arquivar(false);
        assertFalse(reativado.arquivado());
        assertNull(reativado.arquivadoPorId());
        assertEquals(2, gestao.historico(projetoId, 0, 10).getTotalElements());
        assertNotNull(entregas.submeterEntrega(projetoId, new EntregaProjetoRequest("MVP após reativação")).id());
    }

    @Test
    void arquivarExigeEspecialistaVersaoAtualEEncerramentoDaGovernanca() {
        var pedido = kanban.solicitar(projetoId,
                new SolicitarGovernancaRequest(governadaId, "Evidências", List.of(evidenciaId)));
        assertThrows(RegraNegocioException.class, () -> arquivar(true));
        var versao = portfolio.detalhar(projetoId).versao();
        assertThrows(AccessDeniedException.class, () -> gestao.alterarArquivamento(projetoId,
                new ArquivarProjetoRequest(true, versao, "Encerrar")));
        kanban.cancelar(pedido.id(), new CancelarGovernancaRequest("Desistência"));
        arquivar(true);
        assertThrows(RegraNegocioException.class, () -> como(especialista, () -> gestao.alterarArquivamento(projetoId,
                new ArquivarProjetoRequest(false, versao, "Reabrir com versão antiga"))));
    }

    @Test
    void falhaDesfazEdicaoEAuditoriaJuntas() {
        var antes = portfolio.detalhar(projetoId);
        assertThrows(IllegalStateException.class, () -> como(especialista, () -> tx.execute(status -> {
            gestao.atualizar(projetoId, new AtualizarProjetoRequest(dados("Título desfeito", null), antes.versao(), "Ajuste"));
            throw new IllegalStateException("Falha simulada");
        })));
        assertEquals(antes.dados().titulo(), portfolio.detalhar(projetoId).dados().titulo());
        assertEquals(0, gestao.historico(projetoId, 0, 10).getTotalElements());
    }

    @Test
    void legadoSemDemandantePodeSerAtribuidoPeloEspecialista() {
        tx.executeWithoutResult(status -> projetos.findById(projetoId).orElseThrow().setDemandante(null));
        var entrega = entregas.submeterEntrega(projetoId, new EntregaProjetoRequest("MVP legado"));
        assertThrows(AccessDeniedException.class, () -> como(dono, () -> entregas.registrarValidacao(projetoId, entrega.id(),
                new ValidacaoEntregaRequest(ResultadoValidacao.VALIDADA, "Aceito"))));
        var versao = portfolio.detalhar(projetoId).versao();
        como(especialista, () -> gestao.atualizar(projetoId,
                new AtualizarProjetoRequest(dados("Projeto atribuído", analista.getId()), versao, "Atribuir dono")));
        assertNotNull(como(dono, () -> entregas.registrarValidacao(projetoId, entrega.id(),
                new ValidacaoEntregaRequest(ResultadoValidacao.VALIDADA, "Aceito"))).id());
    }

    @Test
    void analistaEEspecialistaRecomendamSemCriarProjeto() {
        long antes = projetos.count();
        assertEquals(analista.getId(), recomendar().autorId());
        assertEquals(especialista.getId(), como(especialista, this::recomendar).autorId());
        assertEquals(antes, projetos.count());
        assertThrows(AccessDeniedException.class, () -> como(dono, this::recomendar));
    }

    @Test
    void destinatarioConverteUmaUnicaDemandaEspontanea() {
        var recomendacao = recomendar();
        long antes = projetos.count();
        assertThrows(AccessDeniedException.class, () -> como(outroDemandante,
                () -> recomendacoes.converter(recomendacao.id(), intake())));
        var convertida = como(dono, () -> recomendacoes.converter(recomendacao.id(), intake()));
        assertEquals(StatusRecomendacao.CONVERTIDA, convertida.status());
        assertEquals(recomendacao.descricao(), convertida.descricao());
        assertEquals(antes + 1, projetos.count());
        assertEquals(OrigemDemanda.ESPONTANEA, portfolio.detalhar(convertida.projetoId()).origem());
        assertEquals(dono.getId(), portfolio.detalhar(convertida.projetoId()).dados().demandanteId());
        assertThrows(RegraNegocioException.class, () -> como(dono,
                () -> recomendacoes.converter(recomendacao.id(), intake())));
    }

    @Test
    void recusaPreservaPropostaESemCriarProjeto() {
        var recomendacao = recomendar();
        long antes = projetos.count();
        var recusada = como(dono, () -> recomendacoes.recusar(recomendacao.id(), new RecusarRecomendacaoRequest("Não priorizar")));
        assertEquals(StatusRecomendacao.RECUSADA, recusada.status());
        assertEquals(antes, projetos.count());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE recomendacoes_demanda SET descricao='Alterada' WHERE id=?", recomendacao.id()));
        assertThrows(RegraNegocioException.class, () -> como(dono,
                () -> recomendacoes.converter(recomendacao.id(), intake())));
    }

    @Test
    void recomendacaoRecusaDestinatarioInvalidoInativoEDemandaInduzida() {
        assertThrows(RegraNegocioException.class, () -> recomendacoes.recomendar(
                new RecomendarDemandaRequest(analista.getId(), "Ideia", "Problema")));
        assertThrows(RegraNegocioException.class, () -> recomendacoes.recomendar(
                new RecomendarDemandaRequest(Long.MAX_VALUE, "Ideia", "Problema")));
        tx.executeWithoutResult(status -> usuarios.findById(outroDemandante.getId()).orElseThrow().setAtivo(false));
        try {
            assertThrows(RegraNegocioException.class, () -> recomendacoes.recomendar(
                    new RecomendarDemandaRequest(outroDemandante.getId(), "Ideia", "Problema")));
        } finally {
            tx.executeWithoutResult(status -> usuarios.findById(outroDemandante.getId()).orElseThrow().setAtivo(true));
        }
        var recomendacao = recomendar();
        assertThrows(RegraNegocioException.class, () -> como(dono, () -> recomendacoes.converter(recomendacao.id(),
                new RegistrarDemandaRequest("Ideia", "Problema", 1L, OrigemDemanda.INDUZIDA, 1L, 1, null))));
    }

    @Test
    void recomendacaoTemVisibilidadeDosEnvolvidosEGestao() {
        var recomendacao = recomendar();
        for (var usuario : List.of(analista, dono, especialista)) {
            assertTrue(como(usuario, () -> recomendacoes.listar(null, 0, 100)).stream()
                    .anyMatch(r -> r.id().equals(recomendacao.id())));
        }
        assertTrue(como(outroDemandante, () -> recomendacoes.listar(null, 0, 100)).isEmpty());
        var convertida = como(dono, () -> recomendacoes.converter(recomendacao.id(), intake()));
        assertNotNull(como(outroDemandante, () -> portfolio.detalhar(convertida.projetoId())));
    }

    @Test
    void falhaNaConversaoDesfazProjetoEEncerramento() {
        var recomendacao = recomendar();
        long antes = projetos.count();
        assertThrows(IllegalStateException.class, () -> como(dono, () -> tx.execute(status -> {
            recomendacoes.converter(recomendacao.id(), intake());
            throw new IllegalStateException("Falha simulada");
        })));
        assertEquals(antes, projetos.count());
        assertEquals(StatusRecomendacao.PENDENTE, repositorioRecomendacoes.findById(recomendacao.id()).orElseThrow().getStatus());
    }

    @Test
    void conversoesConcorrentesGeramSomenteUmProjeto() throws Exception {
        var recomendacao = recomendar();
        long antes = projetos.count();
        var inicio = new CountDownLatch(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> operacao = () -> {
                inicio.countDown();
                if (!inicio.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Threads não iniciaram");
                try {
                    como(dono, () -> recomendacoes.converter(recomendacao.id(), intake()));
                    return true;
                } catch (RegraNegocioException encerrada) {
                    return false;
                }
            };
            var a = executor.submit(operacao);
            var b = executor.submit(operacao);
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(antes + 1, projetos.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void arquivamentoConcorrenteComEntregaMantemEstadoConsistente() throws Exception {
        var versao = portfolio.detalhar(projetoId).versao();
        var inicio = new CountDownLatch(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var arquivo = executor.submit(() -> {
                inicio.countDown();
                if (!inicio.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Threads não iniciaram");
                return como(especialista, () -> gestao.alterarArquivamento(projetoId,
                        new ArquivarProjetoRequest(true, versao, "Arquivar")));
            });
            var entrega = executor.submit(() -> {
                inicio.countDown();
                if (!inicio.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Threads não iniciaram");
                try {
                    como(analista, () -> entregas.submeterEntrega(projetoId, new EntregaProjetoRequest("MVP")));
                    return true;
                } catch (RegraNegocioException arquivado) {
                    return false;
                }
            });
            assertTrue(arquivo.get(15, TimeUnit.SECONDS).arquivado());
            assertEquals(entrega.get(15, TimeUnit.SECONDS) ? 1 : 0, entregas.listarEntregasPorProjeto(projetoId, PageRequest.of(0, 10)).getTotalElements());
            assertThrows(RegraNegocioException.class, () -> entregas.submeterEntrega(projetoId, new EntregaProjetoRequest("Tardia")));
        } finally {
            executor.shutdownNow();
        }
    }

    private RecomendacaoRegistradaResponse recomendar() {
        return recomendacoes.recomendar(new RecomendarDemandaRequest(dono.getId(), "Ideia sugerida", "Problema a investigar"));
    }

    private RegistrarDemandaRequest intake() {
        return new RegistrarDemandaRequest("Demanda da recomendação", "Problema", 1L, OrigemDemanda.ESPONTANEA, null, 2, null);
    }

    private ProjetoConsultadoResponse arquivar(boolean arquivado) {
        var versao = portfolio.detalhar(projetoId).versao();
        return como(especialista, () -> gestao.alterarArquivamento(projetoId,
                new ArquivarProjetoRequest(arquivado, versao, "Revisão do portfólio")));
    }

    private DadosProjetoRequest dados(String titulo, Long analistaId) {
        return new DadosProjetoRequest(titulo, "Problema revisto", 2L, "Hipótese", ArenaTipo.NEGOCIOS_DIGITAIS,
                true, new BigDecimal("123.45"), dono.getId(), analistaId);
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

    private Usuario usuario(String email, PerfilUsuario perfil) {
        var usuario = new Usuario();
        usuario.setNome("Teste ciclo de vida");
        usuario.setEmail(email);
        usuario.setSenhaHash("hash-nao-usado");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarios.saveAndFlush(usuario);
    }

    private Projeto projeto() {
        var projeto = new Projeto();
        projeto.setTitulo("Projeto " + UUID.randomUUID());
        projeto.setDescricaoDor("Problema");
        projeto.setAreaDemandanteId(1L);
        projeto.setResponsavelPo(dono);
        projeto.setDemandante(dono);
        projeto.setArena(ArenaTipo.A_DEFINIR);
        projeto.setOrigem(OrigemDemanda.ESPONTANEA);
        projeto.setTrlAtual(4);
        projeto.setEtapa(etapas.findByCodigoAndAtivaTrue("IDEACAO").orElseThrow());
        return projetos.saveAndFlush(projeto);
    }

    private Evidencia evidencia(Projeto projeto) {
        var evidencia = new Evidencia();
        evidencia.setProjeto(projeto);
        evidencia.setNomeArquivo("correcao.pdf");
        evidencia.setCaminhoStorage("teste/" + UUID.randomUUID());
        evidencia.setTipoArquivo("application/pdf");
        return evidencias.saveAndFlush(evidencia);
    }
}
