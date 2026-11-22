package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.gestao.RecomendacaoRegistradaResponse;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.model.enums.StatusRecomendacao;
import br.com.bbts.sigespin.service.RecomendacaoDemandaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecomendacaoDemandaController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecomendacaoDemandaControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecomendacaoDemandaService recomendacoesService;

    @Test
    @DisplayName("BE-08: Deve retornar recomendação por ID com sucesso (HTTP 200)")
    void deveBuscarRecomendacaoPorIdComSucesso() throws Exception {
        RecomendacaoRegistradaResponse response = new RecomendacaoRegistradaResponse(
                15L,
                2L,
                1L,
                "Recomendação de Automação",
                "Descrição detalhada da recomendação de melhoria.",
                StatusRecomendacao.PENDENTE,
                null,
                LocalDateTime.now(),
                null,
                null
        );

        when(recomendacoesService.buscarPorId(15L)).thenReturn(response);

        mockMvc.perform(get("/api/recomendacoes/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.autorId").value(2))
                .andExpect(jsonPath("$.destinatarioId").value(1))
                .andExpect(jsonPath("$.titulo").value("Recomendação de Automação"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        verify(recomendacoesService).buscarPorId(15L);
    }

    @Test
    @DisplayName("BE-08: Deve retornar HTTP 404 quando recomendação não for encontrada")
    void deveRetornar404QuandoRecomendacaoNaoExiste() throws Exception {
        when(recomendacoesService.buscarPorId(999L))
                .thenThrow(new RecursoNaoEncontradoException("Recomendação não encontrada com id 999"));

        mockMvc.perform(get("/api/recomendacoes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").value("Recomendação não encontrada com id 999"));

        verify(recomendacoesService).buscarPorId(999L);
    }
}
