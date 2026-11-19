package br.com.bbts.sigespin;

import br.com.bbts.sigespin.dto.consulta.ConsultarEspecialistaRequest;
import br.com.bbts.sigespin.dto.qualificacao.RegistrarQualificacaoRequest;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.analise.ConsultaEspecialistaRepository;
import br.com.bbts.sigespin.repository.kanban.EtapaKanbanRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.analise.QualificacaoDemandaRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import br.com.bbts.sigespin.service.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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
@Transactional
@WithMockUser(username = "analise@teste.local", roles = "ANALISTA")
class QualificacaoConsultaServiceTests {
    @Autowired QualificacaoDemandaService qualificacaoService;
    @Autowired ConsultaEspecialistaService consultaService;
    @Autowired QualificacaoDemandaRepository qualificacoes;
    @Autowired ConsultaEspecialistaRepository consultas;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProjetoRepository projetos;
    @Autowired EtapaKanbanRepository etapas;
    @Autowired EntityManager em;

    Usuario analista;
    Usuario especialista;
    Projeto projeto;

    @BeforeEach
    void preparar() {
        analista = criarUsuario("analise@teste.local", PerfilUsuario.ANALISTA);
        especialista = criarUsuario("parecer@teste.local", PerfilUsuario.ESPECIALISTA);
        Usuario dono = criarUsuario("dono@teste.local", PerfilUsuario.DEMANDANTE);
        projeto = new Projeto();
        projeto.setTitulo("Demanda em análise");
        projeto.setDescricaoDor("Problema a qualificar");
        projeto.setAreaDemandanteId(1L);
        projeto.setResponsavelPo(dono);
        projeto.setDemandante(dono);
        projeto.setArena(ArenaTipo.A_DEFINIR);
        projeto.setOrigem(OrigemDemanda.ESPONTANEA);
        projeto.setTrlAtual(4);
        projeto.setEtapa(etapas.findByCodigoAndAtivaTrue("IDEACAO").orElseThrow());
        projetos.saveAndFlush(projeto);
    }

    @Test
    void acrescentaQualificacoesComAutoriaSemSobrescreverHistorico() {
        var primeira = qualificacaoService.registrar(projeto.getId(), avaliacao("Primeira conclusão"));
        var segunda = qualificacaoService.registrar(projeto.getId(), avaliacao("Conclusão complementar"));
        assertNotEquals(primeira.id(), segunda.id());
        assertEquals(analista.getId(), primeira.autorId());
        assertNotNull(primeira.criadoEm());
        assertEquals("Alinhada à tese", primeira.aderenciaTese());
        em.clear(); // Relê do banco, evitando verificar somente os objetos em memória.
        assertEquals("Primeira conclusão", qualificacoes.findById(primeira.id()).orElseThrow().getConclusao());
        assertEquals(2, qualificacoes.findByProjetoId(projeto.getId(), PageRequest.of(0, 10)).getTotalElements());
        Projeto atual = projetos.findById(projeto.getId()).orElseThrow();
        assertEquals(4, atual.getTrlAtual());
        assertEquals("IDEACAO", atual.getEtapa().getCodigo());
        assertNull(atual.getAnalistaResponsavel());
    }

    @Test
    void encaminhaConsultaAntesDaQualificacaoEPreservaConsultasAnteriores() {
        var primeira = consultaService.consultar(projeto.getId(), pergunta(especialista.getId()));
        var segunda = consultaService.consultar(projeto.getId(),
                new ConsultarEspecialistaRequest(especialista.getId(), "Uma nova dúvida"));
        assertNotEquals(primeira.id(), segunda.id());
        assertEquals(analista.getId(), primeira.solicitanteId());
        assertEquals(especialista.getId(), primeira.especialistaId());
        assertEquals("Qual a viabilidade técnica?", primeira.questao());
        assertNotNull(primeira.criadoEm());
        em.clear();
        assertEquals(0, qualificacoes.findByProjetoId(projeto.getId(), PageRequest.of(0, 10)).getTotalElements());
        assertEquals(2, consultas.findByProjetoId(projeto.getId(), PageRequest.of(0, 10)).getTotalElements());
        assertEquals(4, projetos.findById(projeto.getId()).orElseThrow().getTrlAtual());
    }

    @Test
    void rejeitaDestinatarioInexistenteInativoOuDeOutroPerfil() {
        long antes = consultas.count();
        assertThrows(RegraNegocioException.class,
                () -> consultaService.consultar(projeto.getId(), pergunta(Long.MAX_VALUE)));
        assertThrows(RegraNegocioException.class,
                () -> consultaService.consultar(projeto.getId(), pergunta(analista.getId())));
        especialista.setAtivo(false);
        usuarios.saveAndFlush(especialista);
        assertThrows(RegraNegocioException.class,
                () -> consultaService.consultar(projeto.getId(), pergunta(especialista.getId())));
        assertEquals(antes, consultas.count());
    }

    @Test
    void rejeitaProjetoInexistenteSemCriarRegistros() {
        long antesQualificacoes = qualificacoes.count();
        long antesConsultas = consultas.count();
        assertThrows(RegraNegocioException.class,
                () -> qualificacaoService.registrar(Long.MAX_VALUE, avaliacao("Conclusão")));
        assertThrows(RegraNegocioException.class,
                () -> consultaService.consultar(Long.MAX_VALUE, pergunta(especialista.getId())));
        assertEquals(antesQualificacoes, qualificacoes.count());
        assertEquals(antesConsultas, consultas.count());
    }

    @Test
    void validaParametrosETextosObrigatorios() {
        assertThrows(ConstraintViolationException.class,
                () -> qualificacaoService.registrar(projeto.getId(),
                        new RegistrarQualificacaoRequest(" ", "", null, "", "", "", "")));
        assertThrows(ConstraintViolationException.class,
                () -> consultaService.consultar(projeto.getId(), new ConsultarEspecialistaRequest(null, " ")));
        assertThrows(ConstraintViolationException.class,
                () -> qualificacaoService.registrar(0L, avaliacao("Conclusão")));
        assertThrows(ConstraintViolationException.class,
                () -> consultaService.consultar(-1L, pergunta(especialista.getId())));
    }

    @Test
    @WithMockUser(username = "dono@teste.local", roles = "DEMANDANTE")
    void demandanteNaoPodeQualificarOuConsultar() {
        verificarBloqueio();
    }

    @Test
    @WithMockUser(username = "parecer@teste.local", roles = "ESPECIALISTA")
    void especialistaNaoPodeQualificarOuAbrirConsulta() {
        verificarBloqueio();
    }

    @Test
    @WithAnonymousUser
    void anonimoNaoPodeQualificarOuConsultar() {
        verificarBloqueio();
    }

    @Test
    void reconfereAnalistaInativoOuComPerfilAlterado() {
        analista.setAtivo(false);
        usuarios.saveAndFlush(analista);
        verificarBloqueio();
        analista.setAtivo(true);
        analista.setPerfil(PerfilUsuario.ESPECIALISTA);
        usuarios.saveAndFlush(analista);
        verificarBloqueio();
    }

    private void verificarBloqueio() {
        assertThrows(AccessDeniedException.class,
                () -> qualificacaoService.registrar(projeto.getId(), avaliacao("Conclusão")));
        assertThrows(AccessDeniedException.class,
                () -> consultaService.consultar(projeto.getId(), pergunta(especialista.getId())));
    }

    private Usuario criarUsuario(String email, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Teste");
        usuario.setEmail(email);
        usuario.setSenhaHash("nao-utilizado-neste-teste-de-servico");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarios.saveAndFlush(usuario);
    }

    private RegistrarQualificacaoRequest avaliacao(String conclusao) {
        return new RegistrarQualificacaoRequest(" Alinhada à tese ", "Problema relevante",
                "Inovação incremental", "Viável com piloto", "Necessita investigação",
                "Justificativa técnica inicial", conclusao);
    }

    private ConsultarEspecialistaRequest pergunta(Long destinatarioId) {
        return new ConsultarEspecialistaRequest(destinatarioId, " Qual a viabilidade técnica? ");
    }
}
