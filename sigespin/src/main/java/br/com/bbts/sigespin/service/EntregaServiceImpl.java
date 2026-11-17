package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.entrega.*;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.repository.entrega.EntregaProjetoRepository;
import br.com.bbts.sigespin.repository.entrega.ValidacaoEntregaRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Preserva as respostas HTTP da Fase 4 usando as regras de entrega e validação existentes. */
@Service
public class EntregaServiceImpl implements EntregaService {
    private final EntregaProjetoService servico;
    private final EntregaProjetoRepository entregas;
    private final ValidacaoEntregaRepository validacoes;
    private final ProjetoRepository projetos;
    private final UsuarioAtualService usuarioAtual;

    public EntregaServiceImpl(EntregaProjetoService servico, EntregaProjetoRepository entregas,
            ValidacaoEntregaRepository validacoes, ProjetoRepository projetos, UsuarioAtualService usuarioAtual) {
        this.servico = servico;
        this.entregas = entregas;
        this.validacoes = validacoes;
        this.projetos = projetos;
        this.usuarioAtual = usuarioAtual;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<EntregaProjetoResponse> listarEntregasPorProjeto(Long projetoId, Pageable pageable) {
        conferirProjeto(projetoId);
        return entregas.findByProjetoId(projetoId, pageable).map(EntregaProjetoResponse::de);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public EntregaProjetoResponse submeterEntrega(Long projetoId, EntregaProjetoRequest request, Long autorId) {
        var criada = servico.submeter(projetoId, new SubmeterEntregaRequest(request.descricao()));
        return EntregaProjetoResponse.de(entregas.findById(criada.id()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<ValidacaoEntregaResponse> listarValidacoesPorProjeto(Long projetoId, Pageable pageable) {
        conferirProjeto(projetoId);
        return validacoes.findByProjetoId(projetoId, pageable).map(ValidacaoEntregaResponse::de);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('DEMANDANTE')")
    public ValidacaoEntregaResponse registrarValidacao(Long projetoId, Long entregaId,
            ValidacaoEntregaRequest request, Long demandanteId) {
        var criada = servico.validar(projetoId, entregaId,
                new RegistrarValidacaoRequest(request.resultado(), request.observacoes()));
        return ValidacaoEntregaResponse.de(validacoes.findById(criada.id()).orElseThrow());
    }

    private void conferirProjeto(Long projetoId) {
        usuarioAtual.obterAtivo();
        if (!projetos.existsById(projetoId)) throw new RecursoNaoEncontradoException("Projeto não encontrado.");
    }
}
