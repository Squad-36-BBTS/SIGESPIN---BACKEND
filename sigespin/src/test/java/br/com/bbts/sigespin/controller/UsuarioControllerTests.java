package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.usuario.UsuarioResponse;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    @DisplayName("BE-07: Deve listar todos os usuários quando nenhum filtro for informado")
    void deveListarUsuariosSemFiltros() throws Exception {
        UsuarioResponse u1 = new UsuarioResponse(1L, "Demandante Silva", "demandante@bbts.com.br", PerfilUsuario.DEMANDANTE, true);
        UsuarioResponse u2 = new UsuarioResponse(2L, "Analista Santos", "analista@bbts.com.br", PerfilUsuario.ANALISTA, true);
        UsuarioResponse u3 = new UsuarioResponse(3L, "Especialista Costa", "especialista@bbts.com.br", PerfilUsuario.ESPECIALISTA, false);

        when(usuarioService.listar(null, null)).thenReturn(List.of(u1, u2, u3));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Demandante Silva"))
                .andExpect(jsonPath("$[0].email").value("demandante@bbts.com.br"))
                .andExpect(jsonPath("$[0].perfil").value("DEMANDANTE"))
                .andExpect(jsonPath("$[0].ativo").value(true))
                .andExpect(jsonPath("$[0].senhaHash").doesNotExist())
                .andExpect(jsonPath("$[1].perfil").value("ANALISTA"))
                .andExpect(jsonPath("$[2].perfil").value("ESPECIALISTA"))
                .andExpect(jsonPath("$[2].ativo").value(false));

        verify(usuarioService).listar(null, null);
    }

    @Test
    @DisplayName("BE-07: Deve filtrar usuários por perfil e situação ativa")
    void deveFiltrarUsuariosPorPerfilEAtivo() throws Exception {
        UsuarioResponse u1 = new UsuarioResponse(2L, "Analista Santos", "analista@bbts.com.br", PerfilUsuario.ANALISTA, true);

        when(usuarioService.listar(eq(PerfilUsuario.ANALISTA), eq(true))).thenReturn(List.of(u1));

        mockMvc.perform(get("/api/usuarios")
                        .param("perfil", "ANALISTA")
                        .param("ativo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].nome").value("Analista Santos"))
                .andExpect(jsonPath("$[0].perfil").value("ANALISTA"))
                .andExpect(jsonPath("$[0].ativo").value(true))
                .andExpect(jsonPath("$[0].senhaHash").doesNotExist());

        verify(usuarioService).listar(eq(PerfilUsuario.ANALISTA), eq(true));
    }
}
