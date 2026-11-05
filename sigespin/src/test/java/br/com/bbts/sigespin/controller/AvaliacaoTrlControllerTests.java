package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.dto.trl.AvaliacaoTrlRequest;
import br.com.bbts.sigespin.dto.trl.AvaliacaoTrlResponse;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.service.AvaliacaoTrlService;
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

@WebMvcTest(controllers = AvaliacaoTrlController.class)
@AutoConfigureMockMvc(addFilters = false)
class AvaliacaoTrlControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AvaliacaoTrlService avaliacaoTrlService;

    @Test
    @DisplayName("Deve registrar avaliação de TRL com 201 Created e Location")
    void deveRegistrarAvaliacaoTrlComSucesso() throws Exception {
        AvaliacaoTrlRequest request = new AvaliacaoTrlRequest(
                4,
                "Protótipo funcional validado em ambiente de laboratório",
                List.of(201L, 202L)
        );

        UsuarioResumoResponse autor = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        AvaliacaoTrlResponse resposta = new AvaliacaoTrlResponse(
                15L, 8L, autor, 3, 4, request.justificativa(), LocalDateTime.now(), true
        );

        when(avaliacaoTrlService.registrarAvaliacao(eq(8L), any(AvaliacaoTrlRequest.class), any())).thenReturn(resposta);

        mockMvc.perform(post("/api/projetos/8/avaliacoes-trl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/projetos/8/avaliacoes-trl/15")))
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.nivelAnterior").value(3))
                .andExpect(jsonPath("$.nivel").value(4))
                .andExpect(jsonPath("$.evidenciasFechadas").value(true));
    }

    @Test
    @DisplayName("Deve rejeitar nível TRL menor que 1 com 400 Bad Request")
    void deveRejeitarTrlAbaixoDoMinimo() throws Exception {
        AvaliacaoTrlRequest requestInvalido = new AvaliacaoTrlRequest(
                0,
                "Justificativa qualquer",
                null
        );

        mockMvc.perform(post("/api/projetos/8/avaliacoes-trl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                .andExpect(jsonPath("$.detalhes[0]").value(org.hamcrest.Matchers.containsString("O nível TRL mínimo é 1")));
    }

    @Test
    @DisplayName("Deve rejeitar nível TRL maior que 9 com 400 Bad Request")
    void deveRejeitarTrlAcimaDoMaximo() throws Exception {
        AvaliacaoTrlRequest requestInvalido = new AvaliacaoTrlRequest(
                10,
                "Justificativa qualquer",
                null
        );

        mockMvc.perform(post("/api/projetos/8/avaliacoes-trl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                .andExpect(jsonPath("$.detalhes[0]").value(org.hamcrest.Matchers.containsString("O nível TRL máximo é 9")));
    }

    @Test
    @DisplayName("Deve rejeitar justificativa em branco com 400 Bad Request")
    void deveRejeitarJustificativaEmBranco() throws Exception {
        AvaliacaoTrlRequest requestInvalido = new AvaliacaoTrlRequest(
                5,
                "   ",
                null
        );

        mockMvc.perform(post("/api/projetos/8/avaliacoes-trl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"));
    }

    @Test
    @DisplayName("Deve listar histórico de avaliações de TRL com sucesso")
    void deveListarHistoricoAvaliacoesTrl() throws Exception {
        UsuarioResumoResponse autor = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        AvaliacaoTrlResponse resposta = new AvaliacaoTrlResponse(
                15L, 8L, autor, 3, 4, "Justificativa", LocalDateTime.now(), true
        );

        when(avaliacaoTrlService.listarPorProjeto(eq(8L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resposta)));

        mockMvc.perform(get("/api/projetos/8/avaliacoes-trl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(15))
                .andExpect(jsonPath("$.conteudo[0].nivel").value(4));
    }
}
