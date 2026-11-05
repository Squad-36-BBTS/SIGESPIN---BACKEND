package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.parecer.ParecerTecnicoRequest;
import br.com.bbts.sigespin.dto.parecer.ParecerTecnicoResponse;
import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.service.ParecerTecnicoService;
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

@WebMvcTest(controllers = ParecerTecnicoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParecerTecnicoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParecerTecnicoService parecerService;

    @Test
    @DisplayName("Deve emitir parecer técnico com 201 Created e Location")
    void deveEmitirParecerTecnicoComSucesso() throws Exception {
        ParecerTecnicoRequest request = new ParecerTecnicoRequest(
                12L,
                "A infraestrutura suporta a integração proposta via barramento de eventos.",
                "Parecer favorável à arquitetura proposta.",
                List.of(301L, 302L)
        );

        UsuarioResumoResponse autor = new UsuarioResumoResponse(3L, "Especialista", "esp@bbts.com.br", PerfilUsuario.ESPECIALISTA);
        ParecerTecnicoResponse response = new ParecerTecnicoResponse(
                77L, 6L, autor, 12L, request.fundamentacao(), request.conclusao(), true, LocalDateTime.now()
        );

        when(parecerService.emitirParecer(eq(6L), any(ParecerTecnicoRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/projetos/6/pareceres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/projetos/6/pareceres/77")))
                .andExpect(jsonPath("$.id").value(77))
                .andExpect(jsonPath("$.evidenciasFechadas").value(true));
    }

    @Test
    @DisplayName("Deve rejeitar parecer sem fundamentação com 400 Bad Request")
    void deveRejeitarParecerSemFundamentacao() throws Exception {
        ParecerTecnicoRequest requestInvalido = new ParecerTecnicoRequest(
                12L,
                "",
                "Conclusão sem fundamentação",
                null
        );

        mockMvc.perform(post("/api/projetos/6/pareceres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"));
    }

    @Test
    @DisplayName("Deve listar pareceres técnicos do projeto com sucesso")
    void deveListarPareceres() throws Exception {
        UsuarioResumoResponse autor = new UsuarioResumoResponse(3L, "Especialista", "esp@bbts.com.br", PerfilUsuario.ESPECIALISTA);
        ParecerTecnicoResponse response = new ParecerTecnicoResponse(
                77L, 6L, autor, 12L, "Fundamentação", "Conclusão", true, LocalDateTime.now()
        );

        when(parecerService.listarPorProjeto(eq(6L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/projetos/6/pareceres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(77));
    }
}
