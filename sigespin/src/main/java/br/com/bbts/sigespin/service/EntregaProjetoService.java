package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.entrega.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.entrega.EntregaProjetoRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.entrega.ValidacaoEntregaRepository;
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
public class EntregaProjetoService {
    private final ProjetoRepository projetos;
    private final EntregaProjetoRepository entregas;
    private final ValidacaoEntregaRepository validacoes;
    private final UsuarioAtualService usuarioAtual;

    public EntregaProjetoService(ProjetoRepository projetos, EntregaProjetoRepository entregas,
            ValidacaoEntregaRepository validacoes, UsuarioAtualService usuarioAtual) {
        this.projetos = projetos;
        this.entregas = entregas;
        this.validacoes = validacoes;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public EntregaRegistradaResponse submeter(@NotNull @Positive Long projetoId,
            @NotNull @Valid SubmeterEntregaRequest entrada) {
        var autor = usuarioAtual.exigirUmDosPerfis(PerfilUsuario.ANALISTA, PerfilUsuario.ESPECIALISTA);
        var projeto = projetos.findAtivoParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado ou arquivado."));
        var entrega = new EntregaProjeto();
        entrega.setProjeto(projeto);
        entrega.setAutor(autor);
        entrega.setDescricao(entrada.descricao().strip());
        entrega.setSubmetidoEm(LocalDateTime.now());
        entregas.saveAndFlush(entrega);
        return resposta(entrega);
    }

    @Transactional
    @PreAuthorize("hasRole('DEMANDANTE')")
    public ValidacaoRegistradaResponse validar(@NotNull @Positive Long projetoId,
            @NotNull @Positive Long entregaId, @NotNull @Valid RegistrarValidacaoRequest entrada) {
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
        validacoes.saveAndFlush(validacao);
        return resposta(validacao);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<EntregaRegistradaResponse> listarEntregas(@NotNull @Positive Long projetoId,
            @Min(0) int pagina, @Min(1) @Max(100) int tamanho) {
        conferirConsulta(projetoId);
        return entregas.findByProjetoId(projetoId, PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Direction.DESC, "submetidoEm", "id"))).map(this::resposta);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<ValidacaoRegistradaResponse> listarValidacoes(@NotNull @Positive Long projetoId,
            @Min(0) int pagina, @Min(1) @Max(100) int tamanho) {
        conferirConsulta(projetoId);
        return validacoes.findByProjetoId(projetoId, PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Direction.DESC, "validadoEm", "id"))).map(this::resposta);
    }

    private void conferirConsulta(Long projetoId) {
        usuarioAtual.obterAtivo();
        if (!projetos.existsById(projetoId)) throw new RegraNegocioException("Projeto não encontrado.");
    }

    private EntregaRegistradaResponse resposta(EntregaProjeto entrega) {
        return new EntregaRegistradaResponse(entrega.getId(), entrega.getProjeto().getId(),
                entrega.getAutor().getId(), entrega.getDescricao(), entrega.getSubmetidoEm());
    }

    private ValidacaoRegistradaResponse resposta(ValidacaoEntrega validacao) {
        return new ValidacaoRegistradaResponse(validacao.getId(), validacao.getProjeto().getId(),
                validacao.getEntrega().getId(), validacao.getDemandante().getId(), validacao.getResultado(),
                validacao.getObservacoes(), validacao.getValidadoEm());
    }
}
