package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.interacao.InteracaoProjetoResponse;
import br.com.bbts.sigespin.dto.interacao.RegistrarInteracaoRequest;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.model.entity.InteracaoProjeto;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.portfolio.InteracaoProjetoRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Service
@Validated
public class InteracaoProjetoService {
    private final ProjetoRepository projetos;
    private final InteracaoProjetoRepository interacoes;
    private final UsuarioAtualService usuarioAtual;

    public InteracaoProjetoService(ProjetoRepository projetos, InteracaoProjetoRepository interacoes,
            UsuarioAtualService usuarioAtual) {
        this.projetos = projetos;
        this.interacoes = interacoes;
        this.usuarioAtual = usuarioAtual;
    }

    /**
     * Analista e Especialista comentam em qualquer projeto ativo; o Demandante,
     * só no projeto em que é o demandante. O autor é sempre o usuário da sessão.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public InteracaoProjetoResponse registrar(@NotNull @Positive Long projetoId,
            @NotNull @Valid RegistrarInteracaoRequest entrada) {
        var autor = usuarioAtual.obterAtivo();
        String texto = entrada.texto().strip();
        // @NotBlank não reconhece espaços Unicode (ex.: U+2003); strip() reconhece.
        if (texto.isEmpty()) throw new IllegalArgumentException("O texto do comentário está vazio.");
        if (!projetos.existsById(projetoId)) throw new RecursoNaoEncontradoException("Projeto não encontrado.");
        // Trava a linha do projeto, como nas entregas, para não concorrer com o arquivamento.
        var projeto = projetos.findAtivoParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto arquivado não recebe novos comentários."));
        if (autor.getPerfil() == PerfilUsuario.DEMANDANTE
                && (projeto.getDemandante() == null || !projeto.getDemandante().getId().equals(autor.getId()))) {
            throw new AccessDeniedException("Somente o Demandante responsável pode comentar neste projeto.");
        }
        var interacao = new InteracaoProjeto();
        interacao.setProjeto(projeto);
        interacao.setAutor(autor);
        interacao.setTexto(texto);
        interacao.setCriadoEm(LocalDateTime.now());
        interacoes.saveAndFlush(interacao);
        return InteracaoProjetoResponse.de(interacao);
    }

    /** Qualquer usuário autenticado consulta, inclusive em projeto arquivado. Mais recentes primeiro. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<InteracaoProjetoResponse> listar(@NotNull @Positive Long projetoId,
            @Min(0) int pagina, @Min(1) @Max(100) int tamanho) {
        usuarioAtual.obterAtivo();
        if (!projetos.existsById(projetoId)) throw new RecursoNaoEncontradoException("Projeto não encontrado.");
        return interacoes.findByProjetoId(projetoId, PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Direction.DESC, "criadoEm", "id"))).map(InteracaoProjetoResponse::de);
    }
}
