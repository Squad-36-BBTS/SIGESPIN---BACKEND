package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.demanda.RegistrarDemandaRequest;
import br.com.bbts.sigespin.dto.projeto.*;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.enums.EstadoTarefa;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Liga o contrato REST do portfólio ao cadastro transacional da Fase 3. */
@Service
@Validated
public class ProjetoServiceImpl implements ProjetoService {
    private final DemandaService demandas;
    private final ProjetoRepository projetos;
    private final UsuarioAtualService usuarioAtual;
    private final PendenciaProjetoService pendencias;

    public ProjetoServiceImpl(DemandaService demandas, ProjetoRepository projetos, UsuarioAtualService usuarioAtual,
            PendenciaProjetoService pendencias) {
        this.demandas = demandas;
        this.projetos = projetos;
        this.usuarioAtual = usuarioAtual;
        this.pendencias = pendencias;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<ProjetoResponse> listarProjetos(FiltroPortfolio filtro, Pageable pageable) {
        usuarioAtual.obterAtivo();
        Integer periodo = filtro.periodoDias();
        if (periodo != null && (periodo < 1 || periodo > FiltroPortfolio.PERIODO_MAXIMO_DIAS)) {
            throw new IllegalArgumentException("O período deve estar entre 1 e "
                    + FiltroPortfolio.PERIODO_MAXIMO_DIAS + " dias.");
        }
        Pageable ordenado = pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(),
                pageable.getPageSize(), Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id")));
        Page<Projeto> pagina = projetos.findAll(condicoes(filtro), ordenado);
        Map<Long, List<EstadoTarefa>> porProjeto = pendencias.calcular(pagina.getContent());
        return pagina.map(p -> ProjetoResponse.de(p, porProjeto.get(p.getId())));
    }

    private static Specification<Projeto> condicoes(FiltroPortfolio f) {
        List<Specification<Projeto>> partes = new ArrayList<>();
        if (f.arena() != null) partes.add((r, q, cb) -> cb.equal(r.get("arena"), f.arena()));
        if (f.etapaId() != null) partes.add((r, q, cb) -> cb.equal(r.get("etapa").get("id"), f.etapaId()));
        if (f.arquivado() != null) partes.add((r, q, cb) -> cb.equal(r.get("arquivado"), f.arquivado()));
        if (f.origem() != null) partes.add((r, q, cb) -> cb.equal(r.get("origem"), f.origem()));
        if (f.acaoMobilizacaoId() != null) {
            partes.add((r, q, cb) -> cb.equal(r.get("acaoMobilizacao").get("id"), f.acaoMobilizacaoId()));
        }
        if (f.periodoDias() != null) {
            LocalDateTime limite = LocalDateTime.now().minusDays(f.periodoDias());
            partes.add((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("criadoEm"), limite));
        }
        if (f.busca() != null && !f.busca().isBlank()) {
            String termo = "%" + f.busca().strip().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            partes.add((r, q, cb) -> cb.or(cb.like(cb.lower(r.get("titulo")), termo, '\\'),
                    cb.like(cb.lower(r.get("descricaoDor")), termo, '\\')));
        }
        if (f.pendencia() != null) partes.add(PendenciaProjetoService.com(f.pendencia()));
        return Specification.allOf(partes);
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
