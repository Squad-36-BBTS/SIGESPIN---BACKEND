package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.entrega.EntregaProjetoRequest;
import br.com.bbts.sigespin.dto.entrega.EntregaProjetoResponse;
import br.com.bbts.sigespin.dto.entrega.ValidacaoEntregaRequest;
import br.com.bbts.sigespin.dto.entrega.ValidacaoEntregaResponse;
import br.com.bbts.sigespin.dto.projeto.UsuarioResumoResponse;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.model.enums.ResultadoValidacao;
import br.com.bbts.sigespin.service.EntregaService;
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

@WebMvcTest(controllers = EntregaController.class)
@AutoConfigureMockMvc(addFilters = false)
class EntregaControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EntregaService entregaService;

    @Test
    @DisplayName("Deve submeter entrega com 201 Created e Location")
    void deveSubmeterEntregaComSucesso() throws Exception {
        EntregaProjetoRequest request = new EntregaProjetoRequest("MVP da funcionalidade de conciliação automática");
        UsuarioResumoResponse autor = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        EntregaProjetoResponse response = new EntregaProjetoResponse(
                31L, 9L, autor, request.descricao(), LocalDateTime.now()
        );

        when(entregaService.submeterEntrega(eq(9L), any(EntregaProjetoRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/projetos/9/entregas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/projetos/9/entregas/31")))
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.descricao").value(request.descricao()));
    }

    @Test
    @DisplayName("Deve validar entrega pelo demandante com 201 Created e Location")
    void deveRegistrarValidacaoComSucesso() throws Exception {
        ValidacaoEntregaRequest request = new ValidacaoEntregaRequest(
                ResultadoValidacao.VALIDADA,
                "Resultados aderentes às necessidades da área demandante."
        );

        UsuarioResumoResponse demandante = new UsuarioResumoResponse(4L, "Demandante", "demandante@bbts.com.br", PerfilUsuario.DEMANDANTE);
        ValidacaoEntregaResponse response = new ValidacaoEntregaResponse(
                88L, 9L, 31L, demandante, request.resultado(), request.observacoes(), LocalDateTime.now()
        );

        when(entregaService.registrarValidacao(eq(9L), eq(31L), any(ValidacaoEntregaRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/projetos/9/entregas/31/validacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/projetos/9/entregas/31/validacoes/88")))
                .andExpect(jsonPath("$.id").value(88))
                .andExpect(jsonPath("$.resultado").value("VALIDADA"));
    }

    @Test
    @DisplayName("Deve rejeitar validação sem resultado com 400 Bad Request")
    void deveRejeitarValidacaoSemResultado() throws Exception {
        ValidacaoEntregaRequest requestInvalido = new ValidacaoEntregaRequest(
                null,
                "Observação sem resultado preenchido"
        );

        mockMvc.perform(post("/api/projetos/9/entregas/31/validacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"));
    }

    @Test
    @DisplayName("Deve listar entregas do projeto com sucesso")
    void deveListarEntregas() throws Exception {
        UsuarioResumoResponse autor = new UsuarioResumoResponse(2L, "Analista", "analista@bbts.com.br", PerfilUsuario.ANALISTA);
        EntregaProjetoResponse response = new EntregaProjetoResponse(
                31L, 9L, autor, "Descrição", LocalDateTime.now()
        );

        when(entregaService.listarEntregasPorProjeto(eq(9L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/projetos/9/entregas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(31));
    }
}
