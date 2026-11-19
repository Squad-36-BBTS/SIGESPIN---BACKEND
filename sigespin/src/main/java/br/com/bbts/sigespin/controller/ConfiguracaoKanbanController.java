package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.kanban.*;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import br.com.bbts.sigespin.service.ConfiguracaoKanbanService;
import br.com.bbts.sigespin.service.PedidoConfiguracaoKanbanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;

@RestController
@RequestMapping("/api/kanban")
public class ConfiguracaoKanbanController {
    private final ConfiguracaoKanbanService configuracao;
    private final PedidoConfiguracaoKanbanService propostas;

    public ConfiguracaoKanbanController(ConfiguracaoKanbanService configuracao, PedidoConfiguracaoKanbanService propostas) {
        this.configuracao = configuracao;
        this.propostas = propostas;
    }

    public record ConfiguracaoResponse(List<ColunaConfiguradaResponse> etapas, List<PercursoConfiguradoResponse> transicoes) { }

    @GetMapping("/configuracao")
    @PreAuthorize("isAuthenticated()")
    public ConfiguracaoResponse consultar() {
        return new ConfiguracaoResponse(configuracao.listarColunas(), configuracao.listarPercursos());
    }

    @PostMapping("/etapas")
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ResponseEntity<ColunaConfiguradaResponse> criarColuna(@Valid @RequestBody CriarColunaRequest entrada) {
        var criada = configuracao.criarColuna(entrada);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/etapas/{id}")
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ColunaConfiguradaResponse editarColuna(@PathVariable Long id, @Valid @RequestBody EditarColunaRequest entrada) {
        return configuracao.editarColuna(id, entrada);
    }

    @PostMapping("/transicoes")
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ResponseEntity<PercursoConfiguradoResponse> criarPercurso(@Valid @RequestBody CriarPercursoRequest entrada) {
        var criado = configuracao.criarPercurso(entrada);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/transicoes/{id}")
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public PercursoConfiguradoResponse editarPercurso(@PathVariable Long id, @Valid @RequestBody EditarPercursoRequest entrada) {
        return configuracao.editarPercurso(id, entrada);
    }

    @GetMapping("/propostas")
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<PedidoConfiguracaoResponse> listarPropostas(
            @RequestParam(defaultValue = "PENDENTE") StatusSolicitacao status, Pageable pageable) {
        var pagina = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(propostas.listar(status, pagina.getPageNumber(), pagina.getPageSize()));
    }

    @PostMapping("/propostas/colunas")
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<PedidoConfiguracaoResponse> proporColuna(@Valid @RequestBody SolicitarColunaRequest entrada) {
        var criada = propostas.solicitarColuna(entrada);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/kanban/propostas/{id}")
                .buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PostMapping("/propostas/percursos")
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<PedidoConfiguracaoResponse> proporPercurso(@Valid @RequestBody SolicitarPercursoRequest entrada) {
        var criada = propostas.solicitarPercurso(entrada);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/kanban/propostas/{id}")
                .buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PostMapping("/propostas/{id}/decisao")
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public PedidoConfiguracaoResponse decidirProposta(@PathVariable Long id, @Valid @RequestBody DecidirGovernancaRequest entrada) {
        return propostas.decidir(id, entrada);
    }

    @PostMapping("/propostas/{id}/cancelamento")
    @PreAuthorize("hasAnyRole('ANALISTA', 'ESPECIALISTA')")
    public PedidoConfiguracaoResponse cancelarProposta(@PathVariable Long id, @Valid @RequestBody CancelarGovernancaRequest entrada) {
        return propostas.cancelar(id, entrada);
    }
}
