package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.demanda.RegistrarDemandaRequest;
import br.com.bbts.sigespin.dto.projeto.*;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Liga o contrato REST do portfólio ao cadastro transacional da Fase 3. */
@Service
@Validated
public class ProjetoServiceImpl implements ProjetoService {
    private final DemandaService demandas;
    private final ProjetoRepository projetos;
    private final UsuarioAtualService usuarioAtual;

    public ProjetoServiceImpl(DemandaService demandas, ProjetoRepository projetos, UsuarioAtualService usuarioAtual) {
        this.demandas = demandas;
        this.projetos = projetos;
        this.usuarioAtual = usuarioAtual;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<ProjetoResponse> listarProjetos(ArenaTipo arena, Long etapaId, String busca, Boolean arquivado, Pageable pageable) {
        usuarioAtual.obterAtivo();
        String termo = busca == null || busca.isBlank() ? null : busca.strip();
        return projetos.consultarPortfolio(arena, etapaId, termo, arquivado, pageable).map(ProjetoResponse::de);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ProjetoDetalheResponse buscarPorId(Long id) {
        usuarioAtual.obterAtivo();
        return ProjetoDetalheResponse.de(projetos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado.")));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('DEMANDANTE')")
    public ProjetoDetalheResponse criarDemanda(DemandaCreateRequest request, Long usuarioAutenticadoId) {
        // A etapa, arena e autoria iniciais são atribuídas pelo caso de uso de cadastro.
        var criada = demandas.registrar(new RegistrarDemandaRequest(request.titulo(), request.descricaoDor(),
                request.areaDemandanteId(), request.origem(), request.acaoMobilizacaoId(),
                request.trlInicialDeclarado(), request.hipoteseSolucao()));
        return ProjetoDetalheResponse.de(projetos.findById(criada.id()).orElseThrow());
    }
}
