package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.usuario.UsuarioResponse;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * BE-07 — Serviço de consulta de usuários com filtros opcionais.
 * Qualquer usuário autenticado pode consultar a lista (necessário para o frontend
 * selecionar destinatários de recomendações, analistas de projetos etc.).
 */
@Service
@Validated
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final UsuarioAtualService usuarioAtual;

    public UsuarioService(UsuarioRepository usuarios, UsuarioAtualService usuarioAtual) {
        this.usuarios = usuarios;
        this.usuarioAtual = usuarioAtual;
    }

    /**
     * Lista usuários cadastrados com filtros opcionais.
     *
     * @param perfil filtra pelo perfil (opcional)
     * @param ativo  filtra pela situação (opcional)
     * @return lista de usuários sem dados sensíveis
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<UsuarioResponse> listar(PerfilUsuario perfil, Boolean ativo) {
        // Reconfirma autenticação via contexto de segurança — nunca confia em parâmetros.
        usuarioAtual.obterAtivo();
        return usuarios.listarComFiltros(perfil, ativo)
                .stream()
                .map(UsuarioResponse::de)
                .toList();
    }
}
