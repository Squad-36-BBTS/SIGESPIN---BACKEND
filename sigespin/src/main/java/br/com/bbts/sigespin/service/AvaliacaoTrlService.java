package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.trl.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.*;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.persistence.EntityManager;
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
public class AvaliacaoTrlService {
    private final ProjetoRepository projetos;
    private final AvaliacaoTrlRepository avaliacoes;
    private final UsuarioAtualService usuarioAtual;
    private final EvidenciasDocumentoService documentos;
    private final EntityManager em;

    public AvaliacaoTrlService(ProjetoRepository projetos, AvaliacaoTrlRepository avaliacoes,
            UsuarioAtualService usuarioAtual, EvidenciasDocumentoService documentos, EntityManager em) {
        this.projetos = projetos;
        this.avaliacoes = avaliacoes;
        this.usuarioAtual = usuarioAtual;
        this.documentos = documentos;
        this.em = em;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public AvaliacaoTrlRegistradaResponse registrar(@NotNull @Positive Long projetoId,
            @NotNull @Valid RegistrarAvaliacaoTrlRequest entrada) {
        var autor = usuarioAtual.exigirUmDosPerfis(PerfilUsuario.ANALISTA, PerfilUsuario.ESPECIALISTA);
        // O bloqueio dura até o commit: outra avaliação aguarda e lê o nível já atualizado.
        var projeto = projetos.findByIdParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado."));
        var evidencias = documentos.carregarDoProjeto(projetoId, entrada.evidenciaIds());

        var avaliacao = new AvaliacaoTrl();
        avaliacao.setProjeto(projeto);
        avaliacao.setAutor(autor);
        avaliacao.setNivelAnterior(projeto.getTrlAtual());
        avaliacao.setNivel(entrada.nivel());
        avaliacao.setJustificativa(entrada.justificativa().strip());
        avaliacao.setAvaliadoEm(LocalDateTime.now());
        avaliacoes.saveAndFlush(avaliacao); // V4 exige INSERT com o conjunto aberto.

        for (var evidencia : evidencias) {
            var vinculo = new EvidenciaTrl();
            vinculo.setProjeto(projeto);
            vinculo.setRegistro(avaliacao);
            vinculo.setEvidencia(evidencia);
            em.persist(vinculo);
        }
        em.flush(); // Vínculos devem chegar ao banco antes do fechamento.
        avaliacao.setEvidenciasFechadas(true);
        projeto.setTrlAtual(entrada.nivel()); // Pode subir, cair ou permanecer igual.
        em.flush();

        return new AvaliacaoTrlRegistradaResponse(avaliacao.getId(), projetoId, autor.getId(),
                avaliacao.getNivelAnterior(), avaliacao.getNivel(), avaliacao.getJustificativa(),
                evidencias.stream().map(Evidencia::getId).toList(), avaliacao.getAvaliadoEm());
    }
}
