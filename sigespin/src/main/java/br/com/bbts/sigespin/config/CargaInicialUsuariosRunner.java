package br.com.bbts.sigespin.config;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * BE-10 — Carga inicial automatizada e idempotente de usuários para cada perfil:
 * DEMANDANTE, ANALISTA e ESPECIALISTA.
 * As credenciais são lidas exclusivamente de variáveis de ambiente.
 * Nenhuma credencial padrão fixa ou log sensível é emitido.
 */
@Component
public class CargaInicialUsuariosRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialUsuariosRunner.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public CargaInicialUsuariosRunner(UsuarioRepository usuarioRepository,
                                      PasswordEncoder passwordEncoder,
                                      Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Verificando necessidade de carga inicial de usuários...");
        inicializarUsuario(PerfilUsuario.DEMANDANTE, "CARGA_DEMANDANTE");
        inicializarUsuario(PerfilUsuario.ANALISTA, "CARGA_ANALISTA");
        inicializarUsuario(PerfilUsuario.ESPECIALISTA, "CARGA_ESPECIALISTA");
    }

    private void inicializarUsuario(PerfilUsuario perfil, String prefixoEnv) {
        String nomeEnv = prefixoEnv + "_NOME";
        String emailEnv = prefixoEnv + "_EMAIL";
        String senhaEnv = prefixoEnv + "_SENHA";

        String nome = environment.getProperty(nomeEnv);
        String email = environment.getProperty(emailEnv);
        String senha = environment.getProperty(senhaEnv);

        if (nome == null || nome.isBlank() || email == null || email.isBlank() || senha == null || senha.isBlank()) {
            log.warn("Variáveis de ambiente incompletas para carga do perfil {} ({}, {}, {}). Inicialização deste usuário ignorada.",
                    perfil, nomeEnv, emailEnv, senhaEnv);
            return;
        }

        String emailTratado = email.trim();
        if (usuarioRepository.existsByEmail(emailTratado)) {
            log.info("Usuário inicial para o perfil {} com email [{}] já existe. Ignorando criação para garantir idempotência.",
                    perfil, emailTratado);
            return;
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(nome.trim());
        novoUsuario.setEmail(emailTratado);
        novoUsuario.setSenhaHash(passwordEncoder.encode(senha));
        novoUsuario.setPerfil(perfil);
        novoUsuario.setAtivo(true);

        usuarioRepository.save(novoUsuario);
        log.info("Usuário inicial criado com sucesso para o perfil {} com email [{}].", perfil, emailTratado);
    }
}
