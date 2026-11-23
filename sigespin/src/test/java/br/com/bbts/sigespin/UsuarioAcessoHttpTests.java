package br.com.bbts.sigespin;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.DriverManager;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C-01: a lista de usuários traz e-mails e só serve para Analista e Especialista
 * (consulta a especialistas e gestão do fluxo). Testa com a segurança real da aplicação.
 */
@SpringBootTest(properties = {
        "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
        "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.security.jwt.secret=local-test-secret-key-that-is-long-enough-32-bytes"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsuarioAcessoHttpTests {
    private static final String SCHEMA = "usuarios_" + UUID.randomUUID().toString().replace("-", "");

    private static final String SENHA = "Senha-Teste-123";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    @BeforeAll
    void criarUsuarios() {
        for (PerfilUsuario perfil : PerfilUsuario.values()) {
            Usuario usuario = new Usuario();
            usuario.setNome("Teste " + perfil);
            usuario.setEmail(perfil.name().toLowerCase() + "@usuarios.teste");
            usuario.setSenhaHash(encoder.encode(SENHA));
            usuario.setPerfil(perfil);
            usuario.setAtivo(true);
            usuarios.save(usuario);
        }
    }

    private String token(PerfilUsuario perfil) throws Exception {
        String corpo = json.writeValueAsString(java.util.Map.of(
                "email", perfil.name().toLowerCase() + "@usuarios.teste", "senha", SENHA));
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(resposta).get("token").asText();
    }

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        registro.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registro.add("spring.flyway.schemas", () -> SCHEMA);
        registro.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @AfterAll
    void encerrar() throws Exception {
        try (var conexao = DriverManager.getConnection(System.getenv("SIGESPIN_TEST_DB_URL"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("SIGESPIN_TEST_DB_PASSWORD", ""));
             var statement = conexao.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Test
    void analistaListaUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").param("perfil", "ESPECIALISTA").param("ativo", "true")
                        .header("Authorization", token(PerfilUsuario.ANALISTA)))
                .andExpect(status().isOk());
    }

    @Test
    void especialistaListaUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", token(PerfilUsuario.ESPECIALISTA)))
                .andExpect(status().isOk());
    }

    @Test
    void demandanteNaoVeListaDeUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", token(PerfilUsuario.DEMANDANTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void semLoginRecebe401() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }
}
