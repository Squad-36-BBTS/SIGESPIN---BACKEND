package br.com.bbts.sigespin;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.TokenJwtRevogadoRepository;
import br.com.bbts.sigespin.repository.UsuarioRepository;
import br.com.bbts.sigespin.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=${SIGESPIN_TEST_DB_URL}",
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes"
})
@AutoConfigureMockMvc
@Import(AutenticacaoSegurancaTests.ControladorProtegidoTeste.class)
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
class AutenticacaoSegurancaTests {

    private static final String SENHA = "SenhaForte123!";

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired TokenJwtRevogadoRepository tokenRevogadoRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper objectMapper;

    private final List<Long> usuariosCriados = new ArrayList<>();
    private final List<String> tokensCriados = new ArrayList<>();

    @AfterEach
    void limparDadosDeTeste() {
        tokensCriados.forEach(token -> {
            try {
                tokenRevogadoRepository.deleteById(jwtService.validarEObterClaims(token).getId());
            } catch (RuntimeException ignored) {
            }
        });
        usuariosCriados.forEach(usuarioRepository::deleteById);
    }

    @Test
    void autenticaEDevolveApenasDadosPublicosDoUsuario() throws Exception {
        Usuario usuario = criarUsuario(PerfilUsuario.DEMANDANTE, true);
        String token = login(usuario.getEmail());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(usuario.getEmail()))
                .andExpect(jsonPath("$.perfil").value("DEMANDANTE"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void rejeitaCredenciaisInvalidasEUsuariosInativos() throws Exception {
        Usuario ativo = criarUsuario(PerfilUsuario.ANALISTA, true);
        Usuario inativo = criarUsuario(PerfilUsuario.DEMANDANTE, false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ativo.getEmail() + "\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + inativo.getEmail() + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevogaTokenERecursosProtegidosExigemAutenticacao() throws Exception {
        Usuario usuario = criarUsuario(PerfilUsuario.DEMANDANTE, true);
        String token = login(usuario.getEmail());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void autoridadeEspecialistaEAplicadaPorMethodSecurity() throws Exception {
        Usuario demandante = criarUsuario(PerfilUsuario.DEMANDANTE, true);
        Usuario especialista = criarUsuario(PerfilUsuario.ESPECIALISTA, true);

        mockMvc.perform(get("/api/teste-seguranca/especialista")
                        .header("Authorization", "Bearer " + login(demandante.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(get("/api/teste-seguranca/especialista")
                        .header("Authorization", "Bearer " + login(especialista.getEmail())))
                .andExpect(status().isOk());
    }

    @Test
    void corsPermiteOrigemConfiguradaDoFrontEnd() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    private Usuario criarUsuario(PerfilUsuario perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Teste Segurança");
        usuario.setEmail(perfil.name().toLowerCase() + "-" + UUID.randomUUID() + "@teste.local");
        usuario.setSenhaHash(passwordEncoder.encode(SENHA));
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        usuario = usuarioRepository.saveAndFlush(usuario);
        usuariosCriados.add(usuario.getId());
        return usuario;
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(body);
        String token = response.get("token").asText();
        tokensCriados.add(token);
        return token;
    }

    @RestController
    static class ControladorProtegidoTeste {
        @GetMapping("/api/teste-seguranca/especialista")
        @PreAuthorize("hasRole('ESPECIALISTA')")
        String somenteEspecialista() {
            return "ok";
        }
    }
}
