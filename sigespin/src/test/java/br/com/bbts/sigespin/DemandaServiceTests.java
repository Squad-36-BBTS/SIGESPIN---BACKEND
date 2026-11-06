package br.com.bbts.sigespin;

import br.com.bbts.sigespin.dto.demanda.RegistrarDemandaRequest;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.portfolio.AcaoMobilizacaoRepository;
import br.com.bbts.sigespin.repository.kanban.EtapaKanbanRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import br.com.bbts.sigespin.service.DemandaService;
import br.com.bbts.sigespin.service.RegraNegocioException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${SIGESPIN_TEST_DB_URL}",
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes"
})
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
@Transactional // Cada cenário desfaz seus registros e configurações ao terminar.
@WithMockUser(username = "intake@teste.local", roles = "DEMANDANTE")
class DemandaServiceTests {
    @Autowired DemandaService demandas;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProjetoRepository projetos;
    @Autowired EtapaKanbanRepository etapas;
    @Autowired AcaoMobilizacaoRepository acoes;

    Usuario demandante;
    AcaoMobilizacao campanha;

    @BeforeEach
    void preparar() {
        demandante = new Usuario();
        demandante.setNome("Demandante de teste");
        demandante.setEmail("intake@teste.local");
        demandante.setSenhaHash("nao-utilizado-neste-teste-de-servico");
        demandante.setPerfil(PerfilUsuario.DEMANDANTE);
        demandante.setAtivo(true);
        usuarios.saveAndFlush(demandante);
        campanha = new AcaoMobilizacao();
        campanha.setNome("Campanha de teste");
        acoes.saveAndFlush(campanha);
    }

    @Test
    void registraEspontaneaComAutoriaDaSessaoETrlDeclarado() {
        var resposta = demandas.registrar(entrada(OrigemDemanda.ESPONTANEA, null, 6));
        Projeto salvo = projetos.findById(resposta.id()).orElseThrow();
        assertEquals(demandante.getId(), resposta.demandanteId());
        assertEquals(demandante.getId(), salvo.getResponsavelPo().getId());
        assertEquals("Problema de energia", salvo.getTitulo());
        assertEquals("IDEACAO", salvo.getEtapa().getCodigo());
        assertEquals(6, salvo.getTrlAtual());
        assertEquals(ArenaTipo.A_DEFINIR, salvo.getArena());
        assertNull(salvo.getAnalistaResponsavel());
        assertNull(salvo.getAcaoMobilizacao());
        assertNotNull(resposta.criadoEm());
        assertNotNull(salvo.getVersao());
    }

    @Test
    void registraInduzidaVinculadaACampanhaExistente() {
        var resposta = demandas.registrar(entrada(OrigemDemanda.INDUZIDA, campanha.getId(), 1));
        assertEquals(campanha.getId(), resposta.acaoMobilizacaoId());
        assertEquals(OrigemDemanda.INDUZIDA, resposta.origem());
    }

    @Test
    void rejeitaCombinacoesDeOrigemInvalidasSemGravarProjeto() {
        long antes = projetos.count();
        assertThrows(RegraNegocioException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.ESPONTANEA, campanha.getId(), 1)));
        assertThrows(RegraNegocioException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.INDUZIDA, null, 1)));
        assertThrows(RegraNegocioException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.INDUZIDA, Long.MAX_VALUE, 1)));
        assertEquals(antes, projetos.count());
    }

    @Test
    @WithMockUser(username = "intake@teste.local", roles = "ANALISTA")
    void analistaNaoPodeRegistrarDemanda() {
        assertThrows(AccessDeniedException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.ESPONTANEA, null, 1)));
    }

    @Test
    @WithMockUser(username = "intake@teste.local", roles = "ESPECIALISTA")
    void especialistaNaoPodeRegistrarDemanda() {
        assertThrows(AccessDeniedException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.ESPONTANEA, null, 1)));
    }

    @Test
    @WithAnonymousUser
    void usuarioAnonimoNaoPodeRegistrarDemanda() {
        assertThrows(AccessDeniedException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.ESPONTANEA, null, 1)));
    }

    @Test
    void reconfereContaInativaEPerfilPersistido() {
        demandante.setAtivo(false);
        usuarios.saveAndFlush(demandante);
        assertThrows(AccessDeniedException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.ESPONTANEA, null, 1)));
        demandante.setAtivo(true);
        demandante.setPerfil(PerfilUsuario.ANALISTA);
        usuarios.saveAndFlush(demandante);
        assertThrows(AccessDeniedException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.ESPONTANEA, null, 1)));
    }

    @Test
    void validaCamposEIntervaloDoTrlAntesDeGravar() {
        long antes = projetos.count();
        assertThrows(ConstraintViolationException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.ESPONTANEA, null, 0)));
        assertThrows(ConstraintViolationException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.ESPONTANEA, null, 10)));
        assertThrows(ConstraintViolationException.class,
                () -> demandas.registrar(new RegistrarDemandaRequest(" ", "", -1L, null, null, null, null)));
        assertEquals(antes, projetos.count());
    }

    @Test
    void etapaInicialInativaImpedeRegistro() {
        var etapa = etapas.findByCodigoAndAtivaTrue("IDEACAO").orElseThrow();
        etapa.setAtiva(false);
        etapas.saveAndFlush(etapa);
        long antes = projetos.count();
        assertThrows(RegraNegocioException.class,
                () -> demandas.registrar(entrada(OrigemDemanda.ESPONTANEA, null, 1)));
        assertEquals(antes, projetos.count());
    }

    private RegistrarDemandaRequest entrada(OrigemDemanda origem, Long acaoId, Integer trl) {
        return new RegistrarDemandaRequest("  Problema de energia  ", " Consumo sem monitoramento ",
                1L, origem, acaoId, trl, " Monitorar consumo ");
    }
}
