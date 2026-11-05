package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.qualificacao.*;
import br.com.bbts.sigespin.model.entity.QualificacaoDemanda;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.ProjetoRepository;
import br.com.bbts.sigespin.repository.QualificacaoDemandaRepository;
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
        var projeto = projetos.findById(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado."));

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
}
