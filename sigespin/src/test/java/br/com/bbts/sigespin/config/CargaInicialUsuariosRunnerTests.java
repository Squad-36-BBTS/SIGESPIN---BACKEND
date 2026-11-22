package br.com.bbts.sigespin.config;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CargaInicialUsuariosRunnerTests {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    @Mock
    private ApplicationArguments applicationArguments;

    private CargaInicialUsuariosRunner runner;

    @BeforeEach
    void setUp() {
        runner = new CargaInicialUsuariosRunner(usuarioRepository, passwordEncoder, environment);
    }

    @Test
    @DisplayName("BE-10: Deve criar os três usuários iniciais quando variáveis estão presentes e não existem no banco")
    void deveCriarUsuariosIniciaisQuandoVariaveisPresentesENaoExistem() {
        when(environment.getProperty("CARGA_DEMANDANTE_NOME")).thenReturn("Demandante Teste");
        when(environment.getProperty("CARGA_DEMANDANTE_EMAIL")).thenReturn("demandante@bbts.com.br");
        when(environment.getProperty("CARGA_DEMANDANTE_SENHA")).thenReturn("Segredo123!");

        when(environment.getProperty("CARGA_ANALISTA_NOME")).thenReturn("Analista Teste");
        when(environment.getProperty("CARGA_ANALISTA_EMAIL")).thenReturn("analista@bbts.com.br");
        when(environment.getProperty("CARGA_ANALISTA_SENHA")).thenReturn("Segredo456!");

        when(environment.getProperty("CARGA_ESPECIALISTA_NOME")).thenReturn("Especialista Teste");
        when(environment.getProperty("CARGA_ESPECIALISTA_EMAIL")).thenReturn("especialista@bbts.com.br");
        when(environment.getProperty("CARGA_ESPECIALISTA_SENHA")).thenReturn("Segredo789!");

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash_" + invocation.getArgument(0));

        runner.run(applicationArguments);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(3)).save(captor.capture());

        var salvos = captor.getAllValues();
        assertEquals(3, salvos.size());

        Usuario u1 = salvos.get(0);
        assertEquals("Demandante Teste", u1.getNome());
        assertEquals("demandante@bbts.com.br", u1.getEmail());
        assertEquals("hash_Segredo123!", u1.getSenhaHash());
        assertEquals(PerfilUsuario.DEMANDANTE, u1.getPerfil());
        assertTrue(u1.getAtivo());

        Usuario u2 = salvos.get(1);
        assertEquals(PerfilUsuario.ANALISTA, u2.getPerfil());

        Usuario u3 = salvos.get(2);
        assertEquals(PerfilUsuario.ESPECIALISTA, u3.getPerfil());
    }

    @Test
    @DisplayName("BE-10: Deve garantir idempotência e não recriar usuário se já existir no banco")
    void deveGarantirIdempotenciaQuandoUsuarioJaExiste() {
        when(environment.getProperty("CARGA_DEMANDANTE_NOME")).thenReturn("Demandante Teste");
        when(environment.getProperty("CARGA_DEMANDANTE_EMAIL")).thenReturn("demandante@bbts.com.br");
        when(environment.getProperty("CARGA_DEMANDANTE_SENHA")).thenReturn("Segredo123!");

        when(usuarioRepository.existsByEmail("demandante@bbts.com.br")).thenReturn(true);

        runner.run(applicationArguments);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("BE-10: Não deve criar usuário se qualquer variável de ambiente estiver ausente")
    void naoDeveCriarUsuarioComVariaveisAusentes() {
        when(environment.getProperty("CARGA_DEMANDANTE_NOME")).thenReturn("Demandante Teste");
        when(environment.getProperty("CARGA_DEMANDANTE_EMAIL")).thenReturn(null); // Ausente
        when(environment.getProperty("CARGA_DEMANDANTE_SENHA")).thenReturn("Segredo123!");

        runner.run(applicationArguments);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
