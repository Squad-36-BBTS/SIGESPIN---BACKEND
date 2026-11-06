package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.demanda.RegistrarDemandaRequest;
import br.com.bbts.sigespin.dto.gestao.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.portfolio.RecomendacaoDemandaRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.persistence.EntityManager;
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
public class RecomendacaoDemandaService {
    private final RecomendacaoDemandaRepository recomendacoes;
    private final UsuarioRepository usuarios;
    private final UsuarioAtualService usuarioAtual;
    private final DemandaService demandas;
    private final EntityManager em;

    public RecomendacaoDemandaService(RecomendacaoDemandaRepository recomendacoes, UsuarioRepository usuarios,
            UsuarioAtualService usuarioAtual, DemandaService demandas, EntityManager em) {
        this.recomendacoes = recomendacoes;
        this.usuarios = usuarios;
        this.usuarioAtual = usuarioAtual;
        this.demandas = demandas;
        this.em = em;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public RecomendacaoRegistradaResponse recomendar(@NotNull @Valid RecomendarDemandaRequest entrada) {
        var autor = usuarioAtual.exigirUmDosPerfis(PerfilUsuario.ANALISTA, PerfilUsuario.ESPECIALISTA);
        var destinatario = usuarios.findById(entrada.destinatarioId())
                .orElseThrow(() -> new RegraNegocioException("Destinatário não encontrado."));
        if (!Boolean.TRUE.equals(destinatario.getAtivo()) || destinatario.getPerfil() != PerfilUsuario.DEMANDANTE) {
            throw new RegraNegocioException("O destinatário precisa ser um Demandante ativo.");
        }
        var recomendacao = new RecomendacaoDemanda();
        recomendacao.setAutor(autor);
        recomendacao.setDestinatario(destinatario);
        recomendacao.setTitulo(entrada.titulo().strip());
        recomendacao.setDescricao(entrada.descricao().strip());
        recomendacao.setStatus(StatusRecomendacao.PENDENTE);
        recomendacao.setCriadoEm(LocalDateTime.now());
        recomendacoes.saveAndFlush(recomendacao);
        return resposta(recomendacao);
    }

    @Transactional
    @PreAuthorize("hasRole('DEMANDANTE')")
    public RecomendacaoRegistradaResponse converter(@NotNull @Positive Long id,
            @NotNull @Valid RegistrarDemandaRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.DEMANDANTE);
        var recomendacao = paraResponder(id, autor);
        if (entrada.origem() != OrigemDemanda.ESPONTANEA || entrada.acaoMobilizacaoId() != null) {
            throw new RegraNegocioException("Uma recomendação de usuário gera demanda espontânea.");
        }
        // Reutiliza o intake real, com sua validação e autorização, no mesmo commit.
        var criada = demandas.registrar(entrada);
        recomendacao.setProjeto(em.getReference(Projeto.class, criada.id()));
        recomendacao.setStatus(StatusRecomendacao.CONVERTIDA);
        recomendacao.setEncerradoEm(LocalDateTime.now());
        recomendacao.setJustificativaEncerramento("Demanda espontânea registrada pelo destinatário.");
        recomendacoes.saveAndFlush(recomendacao);
        return resposta(recomendacao);
    }

    @Transactional
    @PreAuthorize("hasRole('DEMANDANTE')")
    public RecomendacaoRegistradaResponse recusar(@NotNull @Positive Long id,
            @NotNull @Valid RecusarRecomendacaoRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.DEMANDANTE);
        var recomendacao = paraResponder(id, autor);
        recomendacao.setStatus(StatusRecomendacao.RECUSADA);
        recomendacao.setEncerradoEm(LocalDateTime.now());
        recomendacao.setJustificativaEncerramento(entrada.justificativa().strip());
        recomendacoes.saveAndFlush(recomendacao);
        return resposta(recomendacao);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<RecomendacaoRegistradaResponse> listar(StatusRecomendacao status,
            @Min(0) int pagina, @Min(1) @Max(100) int tamanho) {
        var usuario = usuarioAtual.obterAtivo();
        // Antes de virar projeto, a recomendação fica com os envolvidos e a gestão.
        return recomendacoes.consultar(usuario.getId(), usuario.getPerfil() == PerfilUsuario.ESPECIALISTA,
                status, PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id")))
                .map(this::resposta);
    }

    private RecomendacaoDemanda paraResponder(Long id, Usuario autor) {
        var recomendacao = recomendacoes.findParaResposta(id)
                .orElseThrow(() -> new RegraNegocioException("Recomendação não encontrada."));
        if (!recomendacao.getDestinatario().getId().equals(autor.getId())) {
            throw new AccessDeniedException("Somente o destinatário pode responder à recomendação.");
        }
        if (recomendacao.getStatus() != StatusRecomendacao.PENDENTE) {
            throw new RegraNegocioException("A recomendação já foi encerrada.");
        }
        return recomendacao;
    }

    private RecomendacaoRegistradaResponse resposta(RecomendacaoDemanda recomendacao) {
        return new RecomendacaoRegistradaResponse(recomendacao.getId(), recomendacao.getAutor().getId(),
                recomendacao.getDestinatario().getId(), recomendacao.getTitulo(), recomendacao.getDescricao(),
                recomendacao.getStatus(), recomendacao.getProjeto() == null ? null : recomendacao.getProjeto().getId(),
                recomendacao.getCriadoEm(), recomendacao.getEncerradoEm(), recomendacao.getJustificativaEncerramento());
    }
}
