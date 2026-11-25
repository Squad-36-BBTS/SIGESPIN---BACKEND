package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.entrega.EntregaProjetoRequest;
import br.com.bbts.sigespin.dto.entrega.EntregaProjetoResponse;
import br.com.bbts.sigespin.dto.entrega.ValidacaoEntregaRequest;
import br.com.bbts.sigespin.dto.entrega.ValidacaoEntregaResponse;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.model.entity.EntregaProjeto;
import br.com.bbts.sigespin.model.entity.ValidacaoEntrega;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.entrega.EntregaProjetoRepository;
import br.com.bbts.sigespin.repository.entrega.ValidacaoEntregaRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

/**
 * Entregas do projeto (MVP, piloto) e a validação do Demandante. O autor é sempre o usuário da sessão.
 * Unifica o antigo par EntregaService/EntregaServiceImpl com o EntregaProjetoService.
 */
@Service
@Validated
public class EntregaService {
    private static final int TAMANHO_MAXIMO = 100;

    private final ProjetoRepository projetos;
    private final EntregaProjetoRepository entregas;
    private final ValidacaoEntregaRepository validacoes;
    private final UsuarioAtualService usuarioAtual;

    public EntregaService(ProjetoRepository projetos, EntregaProjetoRepository entregas,
            ValidacaoEntregaRepository validacoes, UsuarioAtualService usuarioAtual) {
        this.projetos = projetos;
        this.entregas = entregas;
        this.validacoes = validacoes;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public EntregaProjetoResponse submeterEntrega(@NotNull @Positive Long projetoId,
            @NotNull @Valid EntregaProjetoRequest entrada) {
        var autor = usuarioAtual.exigirUmDosPerfis(PerfilUsuario.ANALISTA, PerfilUsuario.ESPECIALISTA);
        var projeto = projetos.findAtivoParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado ou arquivado."));
        var entrega = new EntregaProjeto();
        entrega.setProjeto(projeto);
        entrega.setAutor(autor);
        entrega.setDescricao(entrada.descricao().strip());
        entrega.setSubmetidoEm(LocalDateTime.now());
        return EntregaProjetoResponse.de(entregas.saveAndFlush(entrega));
    }

    @Transactional
    @PreAuthorize("hasRole('DEMANDANTE')")
    public ValidacaoEntregaResponse registrarValidacao(@NotNull @Positive Long projetoId,
            @NotNull @Positive Long entregaId, @NotNull @Valid ValidacaoEntregaRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.DEMANDANTE);
        var projeto = projetos.findAtivoParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado ou arquivado."));
        if (projeto.getDemandante() == null || !projeto.getDemandante().getId().equals(autor.getId())) {
            throw new AccessDeniedException("Somente o Demandante responsável pode validar a entrega.");
        }
        var entrega = entregas.findById(entregaId)
                .orElseThrow(() -> new RegraNegocioException("Entrega não encontrada."));
        if (!entrega.getProjeto().getId().equals(projetoId)) {
            throw new RegraNegocioException("A entrega precisa pertencer ao projeto informado.");
        }
        // Cada manifestação é nova, inclusive após ressalvas ou uma validação negativa.
        var validacao = new ValidacaoEntrega();
        validacao.setProjeto(projeto);
        validacao.setEntrega(entrega);
        validacao.setDemandante(autor);
        validacao.setResultado(entrada.resultado());
        validacao.setObservacoes(entrada.observacoes().strip());
        validacao.setValidadoEm(LocalDateTime.now());
        return ValidacaoEntregaResponse.de(validacoes.saveAndFlush(validacao));
    }

    /** Mais recentes primeiro quando a página não pede ordenação; no máximo 100 por página. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<EntregaProjetoResponse> listarEntregasPorProjeto(@NotNull @Positive Long projetoId, Pageable pagina) {
        conferirProjeto(projetoId);
        return entregas.findByProjetoId(projetoId, ordenada(pagina, "submetidoEm")).map(EntregaProjetoResponse::de);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<ValidacaoEntregaResponse> listarValidacoesPorProjeto(@NotNull @Positive Long projetoId, Pageable pagina) {
        conferirProjeto(projetoId);
        return validacoes.findByProjetoId(projetoId, ordenada(pagina, "validadoEm")).map(ValidacaoEntregaResponse::de);
    }

    private static Pageable ordenada(Pageable pagina, String data) {
        int tamanho = Math.min(Math.max(pagina.getPageSize(), 1), TAMANHO_MAXIMO);
        Sort ordem = pagina.getSort().isSorted() ? pagina.getSort() : Sort.by(Sort.Direction.DESC, data, "id");
        return PageRequest.of(Math.max(pagina.getPageNumber(), 0), tamanho, ordem);
    }

    private void conferirProjeto(Long projetoId) {
        usuarioAtual.obterAtivo();
        if (!projetos.existsById(projetoId)) throw new RecursoNaoEncontradoException("Projeto não encontrado.");
    }
}
