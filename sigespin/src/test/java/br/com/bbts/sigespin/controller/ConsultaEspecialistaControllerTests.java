package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.parecer.ConsultaEspecialistaRequest;
import br.com.bbts.sigespin.dto.parecer.ConsultaEspecialistaResponse;
import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.service.ConsultaEspecialistaService;
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

@WebMvcTest(controllers = ConsultaEspecialistaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConsultaEspecialistaControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsultaEspecialistaService consultaService;

    @Test
    @DisplayName("Deve encaminhar consulta a especialista com 201 Created e Location")
    void deveEncaminharConsultaComSucesso() throws Exception {
        ConsultaEspecialistaRequest request = new ConsultaEspecialistaRequest(
                3L,
                "Qual a viabilidade de integração com a infraestrutura legado da BBTS?"
        );

        UsuarioResumoResponse solicitante = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        UsuarioResumoResponse especialista = new UsuarioResumoResponse(3L, "Especialista", "esp@bbts.com.br", PerfilUsuario.ESPECIALISTA);
        ConsultaEspecialistaResponse response = new ConsultaEspecialistaResponse(
                12L, 6L, solicitante, especialista, request.questao(), LocalDateTime.now()
        );

        when(consultaService.encaminharConsulta(eq(6L), any(ConsultaEspecialistaRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/projetos/6/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/projetos/6/consultas/12")))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.questao").value(request.questao()));
    }

    @Test
    @DisplayName("Deve rejeitar consulta sem especialista informado com 400 Bad Request")
    void deveRejeitarConsultaSemEspecialista() throws Exception {
        ConsultaEspecialistaRequest requestInvalido = new ConsultaEspecialistaRequest(null, "Questão sem destinatário");

        mockMvc.perform(post("/api/projetos/6/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"));
    }

    @Test
    @DisplayName("Deve listar histórico de consultas do projeto com sucesso")
    void deveListarConsultas() throws Exception {
        UsuarioResumoResponse solicitante = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        UsuarioResumoResponse especialista = new UsuarioResumoResponse(3L, "Especialista", "esp@bbts.com.br", PerfilUsuario.ESPECIALISTA);
        ConsultaEspecialistaResponse response = new ConsultaEspecialistaResponse(
                12L, 6L, solicitante, especialista, "Questão", LocalDateTime.now()
        );

        when(consultaService.listarPorProjeto(eq(6L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/projetos/6/consultas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(12));
    }
}
