package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.exception.ConflitoEstadoException;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.exception.ServicoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorTesteExcecoes())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Deve tratar RecursoNaoEncontradoException com status 404 e código RECURSO_NAO_ENCONTRADO")
    void deveTratarNaoEncontrado() throws Exception {
        mockMvc.perform(get("/teste/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").value("Recurso teste não encontrado."));
    }

    @Test
    @DisplayName("Deve tratar ConflitoEstadoException com status 409 e código CONFLITO_ESTADO")
    void deveTratarConflitoEstado() throws Exception {
        mockMvc.perform(get("/teste/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("CONFLITO_ESTADO"))
                .andExpect(jsonPath("$.mensagem").value("Transição inválida para a etapa atual."));
    }

    @Test
    @DisplayName("Deve tratar ServicoIndisponivelException com status 501 e código SERVICO_NAO_DISPONIVEL")
    void deveTratarServicoIndisponivel() throws Exception {
        mockMvc.perform(get("/teste/servico-indisponivel"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status").value(501))
                .andExpect(jsonPath("$.codigo").value("SERVICO_NAO_DISPONIVEL"))
                .andExpect(jsonPath("$.mensagem").value("Serviço pendente de implementação pela Fase 3."));
    }

    @Test
    @DisplayName("Deve tratar AccessDeniedException com status 403 e código ACESSO_NEGADO")
    void deveTratarAcessoNegado() throws Exception {
        mockMvc.perform(get("/teste/acesso-negado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.mensagem").value("Seu perfil não tem permissão para esta ação."));
    }

    @Test
    @DisplayName("Deve tratar AuthenticationException com status 401 e código NAO_AUTENTICADO")
    void deveTratarNaoAutenticado() throws Exception {
        mockMvc.perform(get("/teste/nao-autenticado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Deve tratar concorrência otimista com status 409 e código CONFLITO_CONCORRENCIA")
    void deveTratarConcorrenciaOtimista() throws Exception {
        mockMvc.perform(get("/teste/concorrencia"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("CONFLITO_CONCORRENCIA"));
    }

    @Test
    @DisplayName("Deve tratar parâmetros inválidos com status 400 e código REQUISICAO_INVALIDA")
    void deveTratarRequisicaoInvalida() throws Exception {
        mockMvc.perform(get("/teste/requisicao-invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @RestController
    static class ControladorTesteExcecoes {

        @GetMapping("/teste/nao-encontrado")
        void naoEncontrado() {
            throw new RecursoNaoEncontradoException("Recurso teste não encontrado.");
        }

        @GetMapping("/teste/conflito")
        void conflito() {
            throw new ConflitoEstadoException("Transição inválida para a etapa atual.");
        }

        @GetMapping("/teste/servico-indisponivel")
        void servicoIndisponivel() {
            throw new ServicoIndisponivelException("Serviço pendente de implementação pela Fase 3.");
        }

        @GetMapping("/teste/acesso-negado")
        void acessoNegado() {
            throw new AccessDeniedException("Acesso negado");
        }

        @GetMapping("/teste/nao-autenticado")
        void naoAutenticado() {
            throw new BadCredentialsException("Falha de autenticação");
        }

        @GetMapping("/teste/concorrencia")
        void concorrencia() {
            throw new ObjectOptimisticLockingFailureException("Projeto", 1L);
        }

        @GetMapping("/teste/requisicao-invalida")
        void requisicaoInvalida() {
            throw new IllegalArgumentException("Parâmetro inválido");
        }
    }
}
