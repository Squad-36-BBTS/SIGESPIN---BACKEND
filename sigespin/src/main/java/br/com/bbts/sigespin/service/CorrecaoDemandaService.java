package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.gestao.*;
import br.com.bbts.sigespin.model.entity.CorrecaoDemanda;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.portfolio.CorrecaoDemandaRepository;
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
import java.util.List;

@Service
@Validated
public class CorrecaoDemandaService {
    private final ProjetoRepository projetos;
    private final CorrecaoDemandaRepository correcoes;
    private final UsuarioAtualService usuarioAtual;
    private final EvidenciasDocumentoService documentos;

    public CorrecaoDemandaService(ProjetoRepository projetos, CorrecaoDemandaRepository correcoes,
            UsuarioAtualService usuarioAtual, EvidenciasDocumentoService documentos) {
        this.projetos = projetos;
        this.correcoes = correcoes;
        this.usuarioAtual = usuarioAtual;
        this.documentos = documentos;
    }

    @Transactional
    @PreAuthorize("hasRole('DEMANDANTE')")
    public CorrecaoRegistradaResponse registrar(@NotNull @Positive Long projetoId,
            @NotNull @Valid RegistrarCorrecaoRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.DEMANDANTE);
        var projeto = projetos.findAtivoParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado ou arquivado."));
        if (projeto.getDemandante() == null || !projeto.getDemandante().getId().equals(autor.getId())) {
            throw new AccessDeniedException("Somente o Demandante responsável pode registrar correções.");
        }
        var correcao = new CorrecaoDemanda();
        correcao.setProjeto(projeto);
        correcao.setAutor(autor);
        correcao.setDescricao(entrada.descricao().strip());
        correcao.setReferencia(entrada.referencia() == null ? null : entrada.referencia().strip());
        if (entrada.evidenciaId() != null) {
            correcao.setEvidencia(documentos.carregarDoProjeto(projetoId, List.of(entrada.evidenciaId())).getFirst());
        }
        correcao.setCriadoEm(LocalDateTime.now());
        correcoes.saveAndFlush(correcao);
        return resposta(correcao);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<CorrecaoRegistradaResponse> listar(@NotNull @Positive Long projetoId,
            @Min(0) int pagina, @Min(1) @Max(100) int tamanho) {
        usuarioAtual.obterAtivo();
        if (!projetos.existsById(projetoId)) throw new RegraNegocioException("Projeto não encontrado.");
        return correcoes.findByProjetoId(projetoId, PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Direction.DESC, "criadoEm", "id"))).map(this::resposta);
    }

    private CorrecaoRegistradaResponse resposta(CorrecaoDemanda correcao) {
        return new CorrecaoRegistradaResponse(correcao.getId(), correcao.getProjeto().getId(),
                correcao.getAutor().getId(), correcao.getDescricao(), correcao.getReferencia(),
                correcao.getEvidencia() == null ? null : correcao.getEvidencia().getId(), correcao.getCriadoEm());
    }
}
