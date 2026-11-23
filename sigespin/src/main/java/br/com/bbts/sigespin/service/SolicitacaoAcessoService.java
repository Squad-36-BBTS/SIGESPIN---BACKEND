package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.auth.SolicitarAcessoRequest;
import br.com.bbts.sigespin.repository.seguranca.SolicitacaoAcessoRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Registra pedidos de acesso vindos da rota pública. Não cria usuário e não aprova nada:
 * a equipe analisa os pedidos pendentes fora da API.
 */
@Service
@Validated
public class SolicitacaoAcessoService {
    private final SolicitacaoAcessoRepository solicitacoes;
    private final UsuarioRepository usuarios;
    private final LimiteSolicitacoesAcesso limite;

    public SolicitacaoAcessoService(SolicitacaoAcessoRepository solicitacoes, UsuarioRepository usuarios,
            LimiteSolicitacoesAcesso limite) {
        this.solicitacoes = solicitacoes;
        this.usuarios = usuarios;
        this.limite = limite;
    }

    /**
     * Não informa o resultado a quem chama: e-mail novo, e-mail de usuário existente e
     * pedido pendente repetido terminam do mesmo jeito, para não revelar quem está cadastrado.
     */
    @Transactional
    public void solicitar(@NotNull @Valid SolicitarAcessoRequest entrada) {
        // Conta toda tentativa válida, inclusive as ignoradas, para que repetir não fure o limite.
        limite.consumir();
        String nome = entrada.nome().strip();
        String email = entrada.email().strip().toLowerCase(Locale.ROOT);
        String justificativa = entrada.justificativa().strip();
        // @NotBlank não reconhece espaços Unicode (ex.: U+2003); strip() reconhece.
        if (nome.isEmpty() || justificativa.isEmpty()) {
            throw new IllegalArgumentException("Nome e justificativa não podem ficar vazios.");
        }
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) return;
        solicitacoes.registrarSeNaoHouverPendente(nome, email, justificativa, LocalDateTime.now());
    }
}
