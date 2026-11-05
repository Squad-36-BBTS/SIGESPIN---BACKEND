package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.kanban.DecisaoTransicaoRequest;
import br.com.bbts.sigespin.dto.kanban.EtapaKanbanResponse;
import br.com.bbts.sigespin.dto.kanban.MovimentacaoKanbanResponse;
import br.com.bbts.sigespin.dto.kanban.SolicitacaoTransicaoRequest;
import br.com.bbts.sigespin.dto.kanban.SolicitacaoTransicaoResponse;
import br.com.bbts.sigespin.dto.kanban.TransicaoKanbanResponse;
import br.com.bbts.sigespin.dto.projeto.EtapaResumoResponse;
import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import br.com.bbts.sigespin.service.KanbanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KanbanController.class)
@AutoConfigureMockMvc(addFilters = false)
class KanbanControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KanbanService kanbanService;

    @Test
    @DisplayName("Deve listar etapas ativas com sucesso")
    void deveListarEtapasAtivas() throws Exception {
        EtapaKanbanResponse etapa1 = new EtapaKanbanResponse(1L, "IDEACAO", "Ideação", 0, true, null, 0L);
        EtapaKanbanResponse etapa2 = new EtapaKanbanResponse(2L, "POC", "POC", 1, true, null, 0L);

        when(kanbanService.listarEtapasAtivas()).thenReturn(List.of(etapa1, etapa2));

        mockMvc.perform(get("/api/kanban/etapas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("IDEACAO"))
                .andExpect(jsonPath("$[1].codigo").value("POC"));
    }

    @Test
    @DisplayName("Deve listar transições filtrando por etapa de origem")
    void deveListarTransicoesFiltradasPorOrigem() throws Exception {
        EtapaResumoResponse ideacao = new EtapaResumoResponse(1L, "IDEACAO", "Ideação", 0, null);
        EtapaResumoResponse poc = new EtapaResumoResponse(2L, "POC", "POC", 1, null);
        TransicaoKanbanResponse transicao = new TransicaoKanbanResponse(10L, ideacao, poc, true, true, 0L);

        when(kanbanService.listarTransicoesPorOrigem(1L)).thenReturn(List.of(transicao));

        mockMvc.perform(get("/api/kanban/transicoes").param("origemId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].origem.id").value(1))
                .andExpect(jsonPath("$[0].destino.id").value(2))
                .andExpect(jsonPath("$[0].exigeGovernanca").value(true));
    }

    @Test
    @DisplayName("Deve propor transição com 201 Created e Location")
    void deveProporTransicaoComSucesso() throws Exception {
        SolicitacaoTransicaoRequest request = new SolicitacaoTransicaoRequest(
                10L,
                "Resultados preliminares aprovados para avanço à POC",
                List.of(101L, 102L)
        );

        UsuarioResumoResponse autor = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        EtapaResumoResponse ideacao = new EtapaResumoResponse(1L, "IDEACAO", "Ideação", 0, null);
        EtapaResumoResponse poc = new EtapaResumoResponse(2L, "POC", "POC", 1, null);

        SolicitacaoTransicaoResponse criada = new SolicitacaoTransicaoResponse(
                55L, 7L, autor, 10L, ideacao, poc, true, StatusSolicitacao.PENDENTE,
                request.justificativa(), LocalDateTime.now(), null, null, null, 0L
        );

        when(kanbanService.proporTransicao(eq(7L), any(SolicitacaoTransicaoRequest.class), any())).thenReturn(criada);

        mockMvc.perform(post("/api/projetos/7/solicitacoes-transicao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/projetos/7/solicitacoes-transicao/55")))
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("Deve registrar decisão da governança com 200 OK")
    void deveRegistrarDecisaoComSucesso() throws Exception {
        DecisaoTransicaoRequest request = new DecisaoTransicaoRequest(
                true,
                "Critérios técnicos e de governança atendidos para aprovação"
        );

        UsuarioResumoResponse autor = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        UsuarioResumoResponse especialista = new UsuarioResumoResponse(3L, "Especialista", "esp@bbts.com.br", PerfilUsuario.ESPECIALISTA);
        EtapaResumoResponse ideacao = new EtapaResumoResponse(1L, "IDEACAO", "Ideação", 0, null);
        EtapaResumoResponse poc = new EtapaResumoResponse(2L, "POC", "POC", 1, null);

        SolicitacaoTransicaoResponse decidida = new SolicitacaoTransicaoResponse(
                55L, 7L, autor, 10L, ideacao, poc, true, StatusSolicitacao.APROVADA,
                "Justificativa original", LocalDateTime.now(), especialista, request.justificativa(), LocalDateTime.now(), 1L
        );

        when(kanbanService.decidirSolicitacao(eq(55L), any(DecisaoTransicaoRequest.class), any())).thenReturn(decidida);

        mockMvc.perform(patch("/api/kanban/solicitacoes-transicao/55/decisao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.responsavelEncerramento.nome").value("Especialista"));
    }

    @Test
    @DisplayName("Deve listar movimentações com sucesso")
    void deveListarMovimentacoes() throws Exception {
        UsuarioResumoResponse autor = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        EtapaResumoResponse ideacao = new EtapaResumoResponse(1L, "IDEACAO", "Ideação", 0, null);
        EtapaResumoResponse poc = new EtapaResumoResponse(2L, "POC", "POC", 1, null);

        MovimentacaoKanbanResponse mov = new MovimentacaoKanbanResponse(
                1L, 7L, autor, ideacao, poc, 55L, "Transição aprovada pela governança", LocalDateTime.now()
        );

        when(kanbanService.listarMovimentacoesPorProjeto(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mov)));

        mockMvc.perform(get("/api/projetos/7/movimentacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(1))
                .andExpect(jsonPath("$.conteudo[0].origem.codigo").value("IDEACAO"))
                .andExpect(jsonPath("$.conteudo[0].destino.codigo").value("POC"));
    }
}
