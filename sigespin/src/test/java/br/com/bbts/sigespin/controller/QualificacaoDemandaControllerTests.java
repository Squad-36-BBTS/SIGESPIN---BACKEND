package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.dto.qualificacao.QualificacaoDemandaRequest;
import br.com.bbts.sigespin.dto.qualificacao.QualificacaoDemandaResponse;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.service.QualificacaoDemandaService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QualificacaoDemandaController.class)
@AutoConfigureMockMvc(addFilters = false)
class QualificacaoDemandaControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QualificacaoDemandaService qualificacaoService;

    @Test
    @DisplayName("Deve registrar qualificação com 201 Created e Location")
    void deveRegistrarQualificacaoComSucesso() throws Exception {
        QualificacaoDemandaRequest request = new QualificacaoDemandaRequest(
                "Alta aderência à estratégia",
                "Relevância crítica para a BBTS",
                "Inovação incremental no processo",
                "Tecnicamente e financeiramente viável",
                "TRL atual 2 estimado",
                "Pesquisa preliminar e benchmarks",
                "Qualificada para avanço à ideação"
        );

        UsuarioResumoResponse autor = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        QualificacaoDemandaResponse response = new QualificacaoDemandaResponse(
                101L, 5L, autor, request.aderenciaTese(), request.relevancia(),
                request.grauInovacao(), request.viabilidade(), request.maturidadeTecnologica(),
                request.fundamentacao(), request.conclusao(), LocalDateTime.now()
        );

        when(qualificacaoService.registrarQualificacao(eq(5L), any(QualificacaoDemandaRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/projetos/5/qualificacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/projetos/5/qualificacoes/101")))
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.projetoId").value(5))
                .andExpect(jsonPath("$.conclusao").value(request.conclusao()));
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios da qualificação com 400 Bad Request")
    void deveValidarCamposObrigatorios() throws Exception {
        QualificacaoDemandaRequest requestInvalido = new QualificacaoDemandaRequest(
                "", "", "", "", "", "", ""
        );

        mockMvc.perform(post("/api/projetos/5/qualificacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"));
    }

    @Test
    @DisplayName("Deve listar histórico de qualificações do projeto")
    void deveListarQualificacoes() throws Exception {
        UsuarioResumoResponse autor = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        QualificacaoDemandaResponse response = new QualificacaoDemandaResponse(
                101L, 5L, autor, "Aderência", "Relevância", "Inovação", "Viabilidade",
                "Maturidade", "Fundamentação", "Conclusão", LocalDateTime.now()
        );

        when(qualificacaoService.listarPorProjeto(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/projetos/5/qualificacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(101));
    }
}
