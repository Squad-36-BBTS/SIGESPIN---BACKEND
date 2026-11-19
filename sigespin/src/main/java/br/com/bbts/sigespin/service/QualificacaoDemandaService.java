package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.qualificacao.QualificacaoDemandaResponse;
import br.com.bbts.sigespin.dto.qualificacao.QualificacaoDemandaRequest;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.bbts.sigespin.dto.qualificacao.*;
import br.com.bbts.sigespin.model.entity.QualificacaoDemanda;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.analise.QualificacaoDemandaRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Service
@Validated
public class QualificacaoDemandaService {
    private final ProjetoRepository projetos;
    private final QualificacaoDemandaRepository qualificacoes;
    private final UsuarioAtualService usuarioAtual;

    public QualificacaoDemandaService(ProjetoRepository projetos,
            QualificacaoDemandaRepository qualificacoes, UsuarioAtualService usuarioAtual) {
        this.projetos = projetos;
        this.qualificacoes = qualificacoes;
        this.usuarioAtual = usuarioAtual;
    }

    /** Cada chamada acrescenta uma avaliação histórica, sem editar as anteriores. */
    @Transactional
    @PreAuthorize("hasRole('ANALISTA')")
    public QualificacaoRegistradaResponse registrar(@NotNull @Positive Long projetoId,
            @NotNull @Valid RegistrarQualificacaoRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.ANALISTA);
        var projeto = projetos.findAtivoParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado ou arquivado."));

        var qualificacao = new QualificacaoDemanda();
        qualificacao.setProjeto(projeto);
        qualificacao.setAutor(autor);
        qualificacao.setAderenciaTese(entrada.aderenciaTese().strip());
        qualificacao.setRelevancia(entrada.relevancia().strip());
        qualificacao.setGrauInovacao(entrada.grauInovacao().strip());
        qualificacao.setViabilidade(entrada.viabilidade().strip());
        qualificacao.setMaturidadeTecnologica(entrada.maturidadeTecnologica().strip());
        qualificacao.setFundamentacao(entrada.fundamentacao().strip());
        qualificacao.setConclusao(entrada.conclusao().strip());
        qualificacao.setCriadoEm(LocalDateTime.now());
        qualificacoes.saveAndFlush(qualificacao);

        // A análise textual não constitui decisão de governança nem avaliação formal de TRL.
        return new QualificacaoRegistradaResponse(qualificacao.getId(), projeto.getId(), autor.getId(),
                qualificacao.getAderenciaTese(), qualificacao.getRelevancia(),
                qualificacao.getGrauInovacao(), qualificacao.getViabilidade(),
                qualificacao.getMaturidadeTecnologica(), qualificacao.getFundamentacao(),
                qualificacao.getConclusao(), qualificacao.getCriadoEm());
    }

    /** Consulta paginada usada pelo contrato HTTP da Fase 4. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<QualificacaoDemandaResponse> listarPorProjeto(@NotNull @Positive Long projetoId, Pageable pageable) {
        usuarioAtual.obterAtivo();
        if (!projetos.existsById(projetoId)) {
            throw new RecursoNaoEncontradoException("Projeto não encontrado.");
        }
        return qualificacoes.findByProjetoId(projetoId, pageable).map(QualificacaoDemandaResponse::de);
    }

    /** A autoria vem da sessão. A conversão reutiliza a mesma transação e as regras da Fase 3. */
    @Transactional
    @PreAuthorize("hasRole('ANALISTA')")
    public QualificacaoDemandaResponse registrarQualificacao(@NotNull @Positive Long projetoId,
            @NotNull @Valid QualificacaoDemandaRequest request, Long autorId) {
        var registro = registrar(projetoId, new RegistrarQualificacaoRequest(
                request.aderenciaTese(), request.relevancia(), request.grauInovacao(), request.viabilidade(),
                request.maturidadeTecnologica(), request.fundamentacao(), request.conclusao()));
        return QualificacaoDemandaResponse.de(qualificacoes.findById(registro.id()).orElseThrow());
    }
}
