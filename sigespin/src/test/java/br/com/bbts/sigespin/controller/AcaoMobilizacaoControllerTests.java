package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.mobilizacao.AcaoMobilizacaoResponse;
import br.com.bbts.sigespin.service.AcaoMobilizacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AcaoMobilizacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
class AcaoMobilizacaoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AcaoMobilizacaoService acaoMobilizacaoService;

    @Test
    @DisplayName("BE-09: Deve retornar lista de ações de mobilização com sucesso")
    void deveRetornarListaDeAcoesMobilizacao() throws Exception {
        AcaoMobilizacaoResponse a1 = new AcaoMobilizacaoResponse(1L, "Hackathon Inovação", "Maratona de desenvolvimento interno");
        AcaoMobilizacaoResponse a2 = new AcaoMobilizacaoResponse(2L, "Programa Ideias BBTS", "Captação de ideias dos colaboradores");

        when(acaoMobilizacaoService.listar()).thenReturn(List.of(a1, a2));

        mockMvc.perform(get("/api/acoes-mobilizacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Hackathon Inovação"))
                .andExpect(jsonPath("$[0].descricao").value("Maratona de desenvolvimento interno"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].nome").value("Programa Ideias BBTS"));

        verify(acaoMobilizacaoService).listar();
    }

    @Test
    @DisplayName("BE-09: Deve retornar lista vazia quando não houver ações de mobilização")
    void deveRetornarListaVaziaQuandoNaoHouverAcoes() throws Exception {
        when(acaoMobilizacaoService.listar()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/acoes-mobilizacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(acaoMobilizacaoService).listar();
    }
}
