package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.projeto.DemandaCreateRequest;
import br.com.bbts.sigespin.dto.projeto.EtapaResumoResponse;
import br.com.bbts.sigespin.dto.projeto.ProjetoDetalheResponse;
import br.com.bbts.sigespin.dto.projeto.ProjetoResponse;
import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.service.ProjetoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProjetoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjetoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjetoService projetoService;

    @Test
    @DisplayName("Deve listar projetos com paginação e limitar tamanho máximo a 100")
    void deveListarProjetosComLimiteDePaginacao() throws Exception {
        UsuarioResumoResponse po = new UsuarioResumoResponse(1L, "PO", "po@bbts.com.br", PerfilUsuario.ANALISTA);
        EtapaResumoResponse etapa = new EtapaResumoResponse(1L, "IDEACAO", "Ideação", 0, null);
        ProjetoResponse projeto = new ProjetoResponse(
                10L, "Demanda IA", ArenaTipo.NEGOCIOS_DIGITAIS, etapa,
                OrigemDemanda.ESPONTANEA, 1, po, po, po, LocalDateTime.now()
        );

        when(projetoService.listarProjetos(eq(ArenaTipo.NEGOCIOS_DIGITAIS), eq(1L), eq("IA"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projeto)));

        mockMvc.perform(get("/api/projetos")
                        .param("arena", "NEGOCIOS_DIGITAIS")
                        .param("etapaId", "1")
                        .param("busca", "IA")
                        .param("page", "0")
                        .param("size", "250"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(10))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("Demanda IA"))
                .andExpect(jsonPath("$.conteudo[0].arena").value("NEGOCIOS_DIGITAIS"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(projetoService).listarProjetos(eq(ArenaTipo.NEGOCIOS_DIGITAIS), eq(1L), eq("IA"), captor.capture());
        assertEquals(100, captor.getValue().getPageSize(), "O tamanho da página deve ser limitado a 100 no servidor");
    }

    @Test
    @DisplayName("Deve buscar projeto por ID com sucesso")
    void deveBuscarProjetoPorIdComSucesso() throws Exception {
        UsuarioResumoResponse po = new UsuarioResumoResponse(1L, "PO", "po@bbts.com.br", PerfilUsuario.ANALISTA);
        EtapaResumoResponse etapa = new EtapaResumoResponse(1L, "IDEACAO", "Ideação", 0, null);
        ProjetoDetalheResponse detalhe = new ProjetoDetalheResponse(
                10L, "Demanda IA", "Descrição da dor", 100L, ArenaTipo.NEGOCIOS_DIGITAIS,
                etapa, OrigemDemanda.ESPONTANEA, null, 1, "Hipótese", false,
                BigDecimal.valueOf(15000), po, po, po, 0L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(projetoService.buscarPorId(10L)).thenReturn(detalhe);

        mockMvc.perform(get("/api/projetos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.titulo").value("Demanda IA"))
                .andExpect(jsonPath("$.dispendioEstimado").value(15000));
    }

    @Test
    @DisplayName("Deve retornar 404 quando o projeto não for encontrado")
    void deveRetornar404QuandoNaoEncontrado() throws Exception {
        when(projetoService.buscarPorId(999L))
                .thenThrow(new RecursoNaoEncontradoException("Projeto não encontrado com id 999"));

        mockMvc.perform(get("/api/projetos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").value("Projeto não encontrado com id 999"));
    }

    @Test
    @DisplayName("Deve criar demanda espontânea com 201 Created e Location")
    void deveCriarDemandaEspontaneaComSucesso() throws Exception {
        DemandaCreateRequest request = new DemandaCreateRequest(
                "Nova Demanda Espontânea",
                "Descrição detalhada da necessidade",
                50L,
                ArenaTipo.EFICIENCIA_OPERACIONAL,
                OrigemDemanda.ESPONTANEA,
                null,
                "Possível solução em software",
                false,
                BigDecimal.valueOf(25000)
        );

        UsuarioResumoResponse po = new UsuarioResumoResponse(2L, "Autor", "autor@bbts.com.br", PerfilUsuario.DEMANDANTE);
        EtapaResumoResponse etapa = new EtapaResumoResponse(1L, "IDEACAO", "Ideação", 0, null);
        ProjetoDetalheResponse criado = new ProjetoDetalheResponse(
                42L, request.titulo(), request.descricaoDor(), request.areaDemandanteId(), request.arena(),
                etapa, request.origem(), null, 1, request.hipoteseSolucao(), request.potencialLeiDoBem(),
                request.dispendioEstimado(), po, po, null, 0L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(projetoService.criarDemanda(any(DemandaCreateRequest.class), any())).thenReturn(criado);

        mockMvc.perform(post("/api/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/projetos/42")))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.titulo").value("Nova Demanda Espontânea"))
                .andExpect(jsonPath("$.origem").value("ESPONTANEA"));
    }

    @Test
    @DisplayName("Deve rejeitar demanda induzida sem ação de mobilização com 400 Bad Request")
    void deveRejeitarDemandaInduzidaSemAcaoMobilizacao() throws Exception {
        DemandaCreateRequest requestInvalido = new DemandaCreateRequest(
                "Demanda Hackathon",
                "Descrição da dor",
                10L,
                ArenaTipo.SEGURANCA_INTEGRADA,
                OrigemDemanda.INDUZIDA,
                null, // Ação obrigatória para induzida!
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                .andExpect(jsonPath("$.detalhes[0]").value(org.hamcrest.Matchers.containsString("Ação de mobilização é obrigatória")));
    }

    @Test
    @DisplayName("Deve rejeitar demanda espontânea com ação de mobilização com 400 Bad Request")
    void deveRejeitarDemandaEspontaneaComAcaoMobilizacao() throws Exception {
        DemandaCreateRequest requestInvalido = new DemandaCreateRequest(
                "Demanda Espontânea Incorreta",
                "Descrição da dor",
                10L,
                ArenaTipo.EFICIENCIA_OPERACIONAL,
                OrigemDemanda.ESPONTANEA,
                99L, // Espontânea NÃO pode ter ação!
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                .andExpect(jsonPath("$.detalhes[0]").value(org.hamcrest.Matchers.containsString("não devem possuir ação de mobilização")));
    }

    @Test
    @DisplayName("Deve aceitar demanda induzida com ação de mobilização")
    void deveAceitarDemandaInduzidaComAcaoMobilizacao() throws Exception {
        DemandaCreateRequest requestValido = new DemandaCreateRequest(
                "Demanda Hackathon",
                "Descrição da dor",
                10L,
                ArenaTipo.SEGURANCA_INTEGRADA,
                OrigemDemanda.INDUZIDA,
                5L,
                null,
                null,
                null
        );

        UsuarioResumoResponse po = new UsuarioResumoResponse(2L, "Autor", "autor@bbts.com.br", PerfilUsuario.ANALISTA);
        EtapaResumoResponse etapa = new EtapaResumoResponse(1L, "IDEACAO", "Ideação", 0, null);
        ProjetoDetalheResponse criado = new ProjetoDetalheResponse(
                43L, requestValido.titulo(), requestValido.descricaoDor(), requestValido.areaDemandanteId(), requestValido.arena(),
                etapa, requestValido.origem(), 5L, 1, null, null, null,
                po, po, po, 0L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(projetoService.criarDemanda(any(DemandaCreateRequest.class), any())).thenReturn(criado);

        mockMvc.perform(post("/api/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/projetos/43")))
                .andExpect(jsonPath("$.id").value(43))
                .andExpect(jsonPath("$.acaoMobilizacaoId").value(5));
    }
}
