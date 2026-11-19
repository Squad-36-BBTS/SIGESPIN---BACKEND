package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.demanda.RegistrarDemandaRequest;
import br.com.bbts.sigespin.dto.gestao.*;
import br.com.bbts.sigespin.model.enums.StatusRecomendacao;
import br.com.bbts.sigespin.service.RecomendacaoDemandaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/recomendacoes")
public class RecomendacaoDemandaController {
    private final RecomendacaoDemandaService recomendacoes;

    public RecomendacaoDemandaController(RecomendacaoDemandaService recomendacoes) {
        this.recomendacoes = recomendacoes;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<RecomendacaoRegistradaResponse> listar(
            @RequestParam(required = false) StatusRecomendacao status, Pageable pageable) {
        var pagina = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(recomendacoes.listar(status, pagina.getPageNumber(), pagina.getPageSize()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public ResponseEntity<RecomendacaoRegistradaResponse> recomendar(@Valid @RequestBody RecomendarDemandaRequest entrada) {
        var criada = recomendacoes.recomendar(entrada);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PostMapping("/{id}/conversao")
    @PreAuthorize("hasRole('DEMANDANTE')")
    public RecomendacaoRegistradaResponse converter(@PathVariable Long id,
            @Valid @RequestBody RegistrarDemandaRequest entrada) {
        return recomendacoes.converter(id, entrada);
    }

    @PostMapping("/{id}/recusa")
    @PreAuthorize("hasRole('DEMANDANTE')")
    public RecomendacaoRegistradaResponse recusar(@PathVariable Long id,
            @Valid @RequestBody RecusarRecomendacaoRequest entrada) {
        return recomendacoes.recusar(id, entrada);
    }
}
