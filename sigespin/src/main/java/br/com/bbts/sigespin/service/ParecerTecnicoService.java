package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.parecer.ParecerTecnicoResponse;
import br.com.bbts.sigespin.dto.parecer.ParecerTecnicoRequest;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.bbts.sigespin.dto.parecer.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.analise.ConsultaEspecialistaRepository;
import br.com.bbts.sigespin.repository.analise.ParecerTecnicoRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Service
@Validated
public class ParecerTecnicoService {
    private final ProjetoRepository projetos;
    private final ParecerTecnicoRepository pareceres;
    private final ConsultaEspecialistaRepository consultas;
    private final UsuarioAtualService usuarioAtual;
    private final EvidenciasDocumentoService documentos;
    private final EntityManager em;

    public ParecerTecnicoService(ProjetoRepository projetos, ParecerTecnicoRepository pareceres,
            ConsultaEspecialistaRepository consultas, UsuarioAtualService usuarioAtual,
            EvidenciasDocumentoService documentos, EntityManager em) {
        this.projetos = projetos;
        this.pareceres = pareceres;
        this.consultas = consultas;
        this.usuarioAtual = usuarioAtual;
        this.documentos = documentos;
        this.em = em;
    }

    @Transactional
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ParecerRegistradoResponse registrar(@NotNull @Positive Long projetoId,
            @NotNull @Valid RegistrarParecerRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.ESPECIALISTA);
        var projeto = projetos.findAtivoParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado ou arquivado."));
        ConsultaEspecialista consulta = null;
        if (entrada.consultaId() != null) {
            consulta = consultas.findById(entrada.consultaId())
                    .orElseThrow(() -> new RegraNegocioException("Consulta não encontrada."));
            if (!consulta.getProjeto().getId().equals(projetoId)) {
                throw new RegraNegocioException("A consulta precisa pertencer ao mesmo projeto.");
            }
            if (!consulta.getEspecialista().getId().equals(autor.getId())) {
                throw new AccessDeniedException("A consulta deve ser respondida pelo Especialista destinatário.");
            }
        }
        var evidencias = documentos.carregarDoProjeto(projetoId, entrada.evidenciaIds());
        var parecer = new ParecerTecnico();
        parecer.setProjeto(projeto);
        parecer.setAutor(autor);
        parecer.setConsulta(consulta);
        parecer.setFundamentacao(entrada.fundamentacao().strip());
        parecer.setConclusao(entrada.conclusao().strip());
        parecer.setCriadoEm(LocalDateTime.now());
        pareceres.saveAndFlush(parecer);

        for (var evidencia : evidencias) {
            var vinculo = new EvidenciaParecer();
            vinculo.setProjeto(projeto);
            vinculo.setRegistro(parecer);
            vinculo.setEvidencia(evidencia);
            em.persist(vinculo);
        }
        em.flush();
        // Um parecer textual também é fechado: lista vazia não significa conjunto aberto.
        parecer.setEvidenciasFechadas(true);
        em.flush();
        return new ParecerRegistradoResponse(parecer.getId(), projetoId, autor.getId(),
                consulta == null ? null : consulta.getId(), parecer.getFundamentacao(),
                parecer.getConclusao(), evidencias.stream().map(Evidencia::getId).toList(), parecer.getCriadoEm());
    }

    /** Consulta paginada usada pelo contrato HTTP da Fase 4. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<ParecerTecnicoResponse> listarPorProjeto(@NotNull @Positive Long projetoId, Pageable pageable) {
        usuarioAtual.obterAtivo();
        if (!projetos.existsById(projetoId)) {
            throw new RecursoNaoEncontradoException("Projeto não encontrado.");
        }
        return pareceres.findByProjetoId(projetoId, pageable).map(ParecerTecnicoResponse::de);
    }

    /** A autoria vem da sessão. A conversão reutiliza a mesma transação e as regras da Fase 3. */
    @Transactional
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ParecerTecnicoResponse emitirParecer(@NotNull @Positive Long projetoId,
            @NotNull @Valid ParecerTecnicoRequest request, Long autorId) {
        var registro = registrar(projetoId, new RegistrarParecerRequest(request.consultaId(),
                request.fundamentacao(), request.conclusao(), request.evidenciasIds()));
        return ParecerTecnicoResponse.de(pareceres.findById(registro.id()).orElseThrow());
    }
}
