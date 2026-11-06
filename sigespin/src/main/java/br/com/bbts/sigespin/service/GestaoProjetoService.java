package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.gestao.*;
import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.portfolio.AlteracaoProjetoRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.repository.kanban.SolicitacaoTransicaoRepository;
import br.com.bbts.sigespin.repository.seguranca.UsuarioRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Validated
public class GestaoProjetoService {
    private final ProjetoRepository projetos;
    private final UsuarioRepository usuarios;
    private final AlteracaoProjetoRepository alteracoes;
    private final SolicitacaoTransicaoRepository pedidos;
    private final UsuarioAtualService usuarioAtual;
    private final ObjectMapper mapper;

    public GestaoProjetoService(ProjetoRepository projetos, UsuarioRepository usuarios,
            AlteracaoProjetoRepository alteracoes, SolicitacaoTransicaoRepository pedidos,
            UsuarioAtualService usuarioAtual, ObjectMapper mapper) {
        this.projetos = projetos;
        this.usuarios = usuarios;
        this.alteracoes = alteracoes;
        this.pedidos = pedidos;
        this.usuarioAtual = usuarioAtual;
        this.mapper = mapper;
    }

    @Transactional
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ProjetoConsultadoResponse atualizar(@NotNull @Positive Long id,
            @NotNull @Valid AtualizarProjetoRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.ESPECIALISTA);
        var projeto = projetos.findAtivoParaAtualizacao(id)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado ou arquivado."));
        conferirVersao(projeto, entrada.versao());
        var anterior = snapshot(projeto);
        var dados = entrada.dados();
        var demandante = usuarioDoPerfil(dados.demandanteId(), PerfilUsuario.DEMANDANTE);
        var analista = dados.analistaResponsavelId() == null ? null
                : usuarioDoPerfil(dados.analistaResponsavelId(), PerfilUsuario.ANALISTA);
        projeto.setTitulo(dados.titulo().strip());
        projeto.setDescricaoDor(dados.descricaoDor().strip());
        projeto.setAreaDemandanteId(dados.areaDemandanteId());
        projeto.setHipoteseSolucao(dados.hipoteseSolucao() == null ? null : dados.hipoteseSolucao().strip());
        projeto.setArena(dados.arena());
        projeto.setPotencialLeiDoBem(dados.potencialLeiDoBem());
        projeto.setDispendioEstimado(dados.dispendioEstimado());
        projeto.setDemandante(demandante);
        projeto.setResponsavelPo(demandante);
        projeto.setAnalistaResponsavel(analista);
        registrarAlteracao(projeto, autor, TipoAlteracaoProjeto.CADASTRO, entrada.justificativa(), anterior);
        return ProjetoConsultadoResponse.de(projeto);
    }

    @Transactional
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ProjetoConsultadoResponse alterarArquivamento(@NotNull @Positive Long id,
            @NotNull @Valid ArquivarProjetoRequest entrada) {
        var autor = usuarioAtual.exigirPerfil(PerfilUsuario.ESPECIALISTA);
        var projeto = projetos.findByIdParaAtualizacao(id)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado."));
        conferirVersao(projeto, entrada.versao());
        if (entrada.arquivado().equals(projeto.getArquivado())) {
            throw new RegraNegocioException("O projeto já está no estado de arquivamento informado.");
        }
        if (entrada.arquivado() && pedidos.existsByProjetoIdAndStatus(id, StatusSolicitacao.PENDENTE)) {
            throw new RegraNegocioException("Encerre o pedido de governança pendente antes de arquivar.");
        }
        var anterior = snapshot(projeto);
        projeto.setArquivado(entrada.arquivado());
        projeto.setArquivadoEm(entrada.arquivado() ? LocalDateTime.now() : null);
        projeto.setArquivadoPor(entrada.arquivado() ? autor : null);
        projeto.setMotivoArquivamento(entrada.arquivado() ? entrada.justificativa().strip() : null);
        registrarAlteracao(projeto, autor, entrada.arquivado() ? TipoAlteracaoProjeto.ARQUIVAMENTO
                : TipoAlteracaoProjeto.REATIVACAO, entrada.justificativa(), anterior);
        return ProjetoConsultadoResponse.de(projeto);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<AlteracaoProjetoResponse> historico(@NotNull @Positive Long id,
            @Min(0) int pagina, @Min(1) @Max(100) int tamanho) {
        usuarioAtual.obterAtivo();
        if (!projetos.existsById(id)) throw new RegraNegocioException("Projeto não encontrado.");
        return alteracoes.findByProjetoId(id, PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Direction.DESC, "criadoEm", "id"))).map(a -> new AlteracaoProjetoResponse(
                        a.getId(), id, a.getAutor().getId(), a.getTipo(), a.getJustificativa(),
                        a.getAnterior(), a.getAtual(), a.getCriadoEm()));
    }

    private Usuario usuarioDoPerfil(Long id, PerfilUsuario perfil) {
        var usuario = usuarios.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Responsável não encontrado."));
        if (!Boolean.TRUE.equals(usuario.getAtivo()) || usuario.getPerfil() != perfil) {
            throw new RegraNegocioException("Responsável precisa estar ativo e ter o perfil " + perfil + ".");
        }
        return usuario;
    }

    private void conferirVersao(Projeto projeto, Long versao) {
        if (!projeto.getVersao().equals(versao)) {
            throw new RegraNegocioException("O projeto foi alterado. Atualize os dados antes de editar.");
        }
    }

    private Map<String, Object> snapshot(Projeto projeto) {
        return mapper.convertValue(ProjetoConsultadoResponse.de(projeto), new TypeReference<Map<String, Object>>() { });
    }

    private void registrarAlteracao(Projeto projeto, Usuario autor, TipoAlteracaoProjeto tipo,
            String justificativa, Map<String, Object> anterior) {
        projetos.flush(); // A auditoria recebe a versão e a data já atualizadas pelo Hibernate.
        var alteracao = new AlteracaoProjeto();
        alteracao.setProjeto(projeto);
        alteracao.setAutor(autor);
        alteracao.setTipo(tipo);
        alteracao.setJustificativa(justificativa.strip());
        alteracao.setAnterior(anterior);
        alteracao.setAtual(snapshot(projeto));
        alteracao.setCriadoEm(LocalDateTime.now());
        alteracoes.saveAndFlush(alteracao);
    }
}
