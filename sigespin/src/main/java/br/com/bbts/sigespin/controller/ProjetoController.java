package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.projeto.DemandaCreateRequest;
import br.com.bbts.sigespin.dto.projeto.FiltroPortfolio;
import br.com.bbts.sigespin.dto.projeto.ProjetoDetalheResponse;
import br.com.bbts.sigespin.dto.projeto.ProjetoResponse;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.EstadoTarefa;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import br.com.bbts.sigespin.service.ProjetoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    /**
     * Portfólio paginado. Aceita os nomes de parâmetro do Spring ({@code page}, {@code size}, {@code arquivado},
     * {@code acaoMobilizacaoId}) e os do frontend ({@code pagina}, {@code tamanho}, {@code arquivamento},
     * {@code campanhaId}). Sem ordenação explícita, os mais recentes vêm primeiro.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<ProjetoResponse> listarProjetos(
            @RequestParam(name = "arena", required = false) ArenaTipo arena,
            @RequestParam(name = "etapaId", required = false) Long etapaId,
            @RequestParam(name = "busca", required = false) String busca,
            @RequestParam(name = "arquivado", required = false) Boolean arquivado,
            @RequestParam(name = "arquivamento", required = false) FiltroPortfolio.Arquivamento arquivamento,
            @RequestParam(name = "origem", required = false) OrigemDemanda origem,
            @RequestParam(name = "acaoMobilizacaoId", required = false) Long acaoMobilizacaoId,
            @RequestParam(name = "campanhaId", required = false) Long campanhaId,
            @RequestParam(name = "pendencia", required = false) EstadoTarefa pendencia,
            @RequestParam(name = "periodoDias", required = false) Integer periodoDias,
            @RequestParam(name = "pagina", required = false) Integer pagina,
            @RequestParam(name = "tamanho", required = false) Integer tamanho,
            Pageable pageable) {
        Pageable base = pagina == null && tamanho == null ? pageable : PageRequest.of(
                pagina != null ? pagina : pageable.getPageNumber(),
                tamanho != null ? tamanho : pageable.getPageSize(), pageable.getSort());
        Pageable seguro = PaginacaoUtils.ajustarLimite(base);
        var filtro = new FiltroPortfolio(arena, etapaId, busca,
                arquivamento != null ? arquivamento.comoArquivado() : arquivado,
                origem, acaoMobilizacaoId != null ? acaoMobilizacaoId : campanhaId, pendencia, periodoDias);
        return PaginaResponse.de(projetoService.listarProjetos(filtro, seguro));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProjetoDetalheResponse buscarPorId(@PathVariable("id") Long id) {
        return projetoService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('DEMANDANTE')")
    public ResponseEntity<ProjetoDetalheResponse> criarDemanda(
            @Valid @RequestBody DemandaCreateRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long usuarioId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        ProjetoDetalheResponse criado = projetoService.criarDemanda(request, usuarioId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(location).body(criado);
    }
}
