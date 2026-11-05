package br.com.bbts.sigespin;

import br.com.bbts.sigespin.dto.parecer.*;
import br.com.bbts.sigespin.dto.trl.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.*;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

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
@WithMockUser(username = "analista-doc@teste.local", roles = "ANALISTA")
class ParecerTrlServiceTests {
    // Esta suíte faz commits reais para exercitar os constraint triggers diferidos da V4.
    // Um schema exclusivo permite limpar históricos imutáveis sem remover suas proteções.
    static final String SCHEMA = "documentos_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        registro.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registro.add("spring.flyway.schemas", () -> SCHEMA);
        registro.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @Autowired AvaliacaoTrlService trl;
    @Autowired ParecerTecnicoService pareceres;
    @Autowired ProjetoRepository projetos;
    @Autowired UsuarioRepository usuarios;
    @Autowired EvidenciaRepository evidencias;
    @Autowired EtapaKanbanRepository etapas;
    @Autowired ConsultaEspecialistaRepository consultas;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired JdbcTemplate jdbc;

    TransactionTemplate tx;
    Usuario analista, especialista, outroEspecialista;
    Long projetoId, evidenciaId, consultaId;

    @BeforeAll
    void criarUsuarios() {
        tx = new TransactionTemplate(transacoes);
        tx.executeWithoutResult(status -> {
            analista = usuario("analista-doc@teste.local", PerfilUsuario.ANALISTA);
            especialista = usuario("especialista-doc@teste.local", PerfilUsuario.ESPECIALISTA);
            outroEspecialista = usuario("outro-doc@teste.local", PerfilUsuario.ESPECIALISTA);
        });
    }

    @BeforeEach
    void criarProjetoEConsulta() {
        tx.executeWithoutResult(status -> {
            Projeto projeto = projeto();
            projetoId = projeto.getId();
            evidenciaId = evidencia(projeto).getId();
            var consulta = new ConsultaEspecialista();
            consulta.setProjeto(projeto);
            consulta.setSolicitante(analista);
            consulta.setEspecialista(especialista);
            consulta.setQuestao("Como avaliar o piloto?");
            consulta.setCriadoEm(LocalDateTime.now());
            consultaId = consultas.saveAndFlush(consulta).getId();
        });
    }

    @AfterAll
    void removerSchemaDescartavel() throws Exception {
        try (var c = DriverManager.getConnection(System.getenv("SIGESPIN_TEST_DB_URL"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_PASSWORD", ""));
             var statement = c.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Test
    void analistaAvaliaSubidaReducaoEManutencaoComHistoricoFechado() {
        var subida = trl.registrar(projetoId, avaliacao(6));
        var reducao = trl.registrar(projetoId, avaliacao(3));
        var manutencao = trl.registrar(projetoId, avaliacao(3));
        assertEquals(4, subida.nivelAnterior());
        assertEquals(6, reducao.nivelAnterior());
        assertEquals(3, manutencao.nivelAnterior());
        assertEquals(analista.getId(), subida.autorId());
        assertEquals(List.of(evidenciaId), subida.evidenciaIds());
        assertEquals(3, projetos.findById(projetoId).orElseThrow().getTrlAtual());
        assertEquals(3, contar("avaliacoes_trl"));
        assertEquals(3, contar("evidencias_trl"));
        assertEquals(3L, jdbc.queryForObject("SELECT count(*) FROM avaliacoes_trl WHERE projeto_id=? AND evidencias_fechadas", Long.class, projetoId));
        assertEquals("IDEACAO", jdbc.queryForObject("SELECT e.codigo FROM projetos p JOIN etapas_kanban e ON p.etapa_id=e.id WHERE p.id=?", String.class, projetoId));
    }

    @Test
    @WithMockUser(username = "especialista-doc@teste.local", roles = "ESPECIALISTA")
    void especialistaTambemPodeAvaliarTrl() {
        assertEquals(especialista.getId(), trl.registrar(projetoId, avaliacao(5)).autorId());
    }

    @Test
    @WithMockUser(username = "especialista-doc@teste.local", roles = "ESPECIALISTA")
    void parecerTextualVinculadoAConsultaFechaMesmoSemAnexos() {
        var resposta = pareceres.registrar(projetoId, parecer(consultaId, List.of()));
        assertEquals(consultaId, resposta.consultaId());
        assertEquals(especialista.getId(), resposta.autorId());
        assertTrue(resposta.evidenciaIds().isEmpty());
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT evidencias_fechadas FROM pareceres_tecnicos WHERE id=?", Boolean.class, resposta.id()));
        assertEquals(4, projetos.findById(projetoId).orElseThrow().getTrlAtual());
    }

    @Test
    @WithMockUser(username = "especialista-doc@teste.local", roles = "ESPECIALISTA")
    void pareceresComplementaresPreservamAnterioresEReutilizamArquivo() {
        var primeiro = pareceres.registrar(projetoId, parecer(consultaId, List.of(evidenciaId)));
        var segundo = pareceres.registrar(projetoId, parecer(null, List.of(evidenciaId)));
        assertNotEquals(primeiro.id(), segundo.id());
        assertNull(segundo.consultaId());
        assertEquals(2, contar("pareceres_tecnicos"));
        assertEquals(2, contar("evidencias_parecer"));
    }

    @Test
    void analistaNaoEmiteParecer() {
        assertThrows(AccessDeniedException.class,
                () -> pareceres.registrar(projetoId, parecer(null, List.of())));
    }

    @Test
    @WithMockUser(username = "demandante-doc@teste.local", roles = "DEMANDANTE")
    void demandanteNaoAvaliaTrlNemEmiteParecer() {
        assertThrows(AccessDeniedException.class, () -> trl.registrar(projetoId, avaliacao(5)));
        assertThrows(AccessDeniedException.class,
                () -> pareceres.registrar(projetoId, parecer(null, List.of())));
    }

    @Test
    @WithMockUser(username = "outro-doc@teste.local", roles = "ESPECIALISTA")
    void consultaDirigidaNaoPodeSerRespondidaPorOutroEspecialista() {
        assertThrows(AccessDeniedException.class,
                () -> pareceres.registrar(projetoId, parecer(consultaId, List.of())));
        assertEquals(0, contar("pareceres_tecnicos"));
    }

    @Test
    @WithMockUser(username = "especialista-doc@teste.local", roles = "ESPECIALISTA")
    void rejeitaConsultaDeOutroProjetoOuInexistente() {
        Long outroProjetoId = tx.execute(status -> projeto().getId());
        assertThrows(RegraNegocioException.class,
                () -> pareceres.registrar(outroProjetoId, parecer(consultaId, List.of())));
        assertThrows(RegraNegocioException.class,
                () -> pareceres.registrar(projetoId, parecer(Long.MAX_VALUE, List.of())));
        assertThrows(RegraNegocioException.class,
                () -> pareceres.registrar(Long.MAX_VALUE, parecer(null, List.of())));
    }

    @Test
    void rejeitaEvidenciaAusenteDuplicadaOuDeOutroProjeto() {
        Long estrangeira = tx.execute(status -> evidencia(projeto()).getId());
        for (List<Long> ids : List.of(List.of(Long.MAX_VALUE), List.of(evidenciaId, evidenciaId), List.of(estrangeira))) {
            assertThrows(RegraNegocioException.class, () -> trl.registrar(projetoId,
                    new RegistrarAvaliacaoTrlRequest(6, "Justificativa", ids)));
        }
        assertThrows(RegraNegocioException.class, () -> trl.registrar(Long.MAX_VALUE, avaliacao(6)));
        assertEquals(0, contar("avaliacoes_trl"));
        assertEquals(4, projetos.findById(projetoId).orElseThrow().getTrlAtual());
    }

    @Test
    @WithMockUser(username = "especialista-doc@teste.local", roles = "ESPECIALISTA")
    void parecerTambemRecusaEvidenciasInvalidas() {
        Long estrangeira = tx.execute(status -> evidencia(projeto()).getId());
        for (List<Long> ids : List.of(List.of(Long.MAX_VALUE), List.of(evidenciaId, evidenciaId), List.of(estrangeira))) {
            assertThrows(RegraNegocioException.class, () -> pareceres.registrar(projetoId, parecer(null, ids)));
        }
        assertEquals(0, contar("pareceres_tecnicos"));
    }

    @Test
    void exigeEvidenciaETrlValido() {
        assertThrows(ConstraintViolationException.class, () -> trl.registrar(projetoId,
                new RegistrarAvaliacaoTrlRequest(6, "Justificativa", List.of())));
        assertThrows(ConstraintViolationException.class, () -> trl.registrar(projetoId, avaliacao(10)));
        assertThrows(ConstraintViolationException.class, () -> trl.registrar(projetoId,
                new RegistrarAvaliacaoTrlRequest(6, " ", List.of(evidenciaId))));
    }

    @Test
    void falhaPosteriorDesfazAvaliacaoVinculosEIndicadorJuntos() {
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            trl.registrar(projetoId, avaliacao(7));
            throw new IllegalStateException("Falha simulada antes do commit");
        }));
        assertEquals(0, contar("avaliacoes_trl"));
        assertEquals(0, contar("evidencias_trl"));
        assertEquals(4, projetos.findById(projetoId).orElseThrow().getTrlAtual());
    }

    @Test
    @WithMockUser(username = "especialista-doc@teste.local", roles = "ESPECIALISTA")
    void bancoImpedeEditarRegistroFechadoOuAnexarDepoisDoCommit() {
        var resposta = pareceres.registrar(projetoId, parecer(null, List.of(evidenciaId)));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE pareceres_tecnicos SET conclusao='Alterada' WHERE id=?", resposta.id()));
        Long novaEvidencia = tx.execute(status -> evidencia(projetos.findById(projetoId).orElseThrow()).getId());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO evidencias_parecer(projeto_id,parecer_id,evidencia_id) VALUES (?,?,?)",
                projetoId, resposta.id(), novaEvidencia));
        assertEquals(1, contar("evidencias_parecer"));
    }

    @Test
    void avaliacoesConcorrentesFormamCadeiaDeNiveisCoerente() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var prontas = new CountDownLatch(2);
        try {
            var primeira = executor.submit(() -> avaliarEmOutraThread(6, prontas));
            var segunda = executor.submit(() -> avaliarEmOutraThread(2, prontas));
            var a = primeira.get(15, TimeUnit.SECONDS);
            var b = segunda.get(15, TimeUnit.SECONDS);
            if (a.nivelAnterior() == 4) {
                assertEquals(a.nivel(), b.nivelAnterior());
                assertEquals(b.nivel(), projetos.findById(projetoId).orElseThrow().getTrlAtual());
            } else {
                assertEquals(4, b.nivelAnterior());
                assertEquals(b.nivel(), a.nivelAnterior());
                assertEquals(a.nivel(), projetos.findById(projetoId).orElseThrow().getTrlAtual());
            }
            assertEquals(2, contar("avaliacoes_trl"));
        } finally {
            executor.shutdownNow();
        }
    }

    private AvaliacaoTrlRegistradaResponse avaliarEmOutraThread(int nivel, CountDownLatch prontas) throws Exception {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken("analista-doc@teste.local", null,
                List.of(new SimpleGrantedAuthority("ROLE_ANALISTA"))));
        SecurityContextHolder.setContext(contexto);
        try {
            prontas.countDown();
            if (!prontas.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Threads não iniciaram");
            return trl.registrar(projetoId, avaliacao(nivel));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private long contar(String tabela) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela + " WHERE projeto_id=?", Long.class, projetoId);
    }

    private RegistrarAvaliacaoTrlRequest avaliacao(int nivel) {
        return new RegistrarAvaliacaoTrlRequest(nivel, "Resultado do piloto", List.of(evidenciaId));
    }

    private RegistrarParecerRequest parecer(Long consulta, List<Long> ids) {
        return new RegistrarParecerRequest(consulta, " Fundamentação técnica ", "Conclusão", ids);
    }

    private Usuario usuario(String email, PerfilUsuario perfil) {
        var u = new Usuario();
        u.setNome("Teste documental"); u.setEmail(email); u.setSenhaHash("hash-nao-usado");
        u.setPerfil(perfil); u.setAtivo(true);
        return usuarios.saveAndFlush(u);
    }

    private Projeto projeto() {
        var p = new Projeto();
        p.setTitulo("Piloto documental"); p.setDescricaoDor("Problema"); p.setAreaDemandanteId(1L);
        p.setResponsavelPo(analista); p.setArena(ArenaTipo.A_DEFINIR); p.setTrlAtual(4);
        p.setOrigem(OrigemDemanda.ESPONTANEA);
        p.setEtapa(etapas.findByCodigoAndAtivaTrue("IDEACAO").orElseThrow());
        return projetos.saveAndFlush(p);
    }

    private Evidencia evidencia(Projeto projeto) {
        var e = new Evidencia();
        e.setProjeto(projeto); e.setNomeArquivo("piloto.pdf");
        e.setCaminhoStorage("teste/" + UUID.randomUUID()); e.setTipoArquivo("application/pdf");
        return evidencias.saveAndFlush(e);
    }
}
