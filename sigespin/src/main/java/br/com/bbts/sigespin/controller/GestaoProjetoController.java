package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.gestao.*;
import br.com.bbts.sigespin.service.CorrecaoDemandaService;
import br.com.bbts.sigespin.service.GestaoProjetoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/projetos/{projetoId}")
public class GestaoProjetoController {
    private final GestaoProjetoService gestao;
    private final CorrecaoDemandaService correcoes;

    public GestaoProjetoController(GestaoProjetoService gestao, CorrecaoDemandaService correcoes) {
        this.gestao = gestao;
        this.correcoes = correcoes;
    }

    @PutMapping
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ProjetoConsultadoResponse editar(@PathVariable Long projetoId,
            @Valid @RequestBody AtualizarProjetoRequest entrada) {
        return gestao.atualizar(projetoId, entrada);
    }

    @PutMapping("/arquivamento")
    @PreAuthorize("hasRole('ESPECIALISTA')")
    public ProjetoConsultadoResponse arquivar(@PathVariable Long projetoId,
            @Valid @RequestBody ArquivarProjetoRequest entrada) {
        return gestao.alterarArquivamento(projetoId, entrada);
    }

    @GetMapping("/historico-gestao")
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<AlteracaoProjetoResponse> historico(@PathVariable Long projetoId, Pageable pageable) {
        var pagina = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(gestao.historico(projetoId, pagina.getPageNumber(), pagina.getPageSize()));
    }

    @PostMapping("/correcoes")
    @PreAuthorize("hasRole('DEMANDANTE')")
    public ResponseEntity<CorrecaoRegistradaResponse> corrigir(@PathVariable Long projetoId,
            @Valid @RequestBody RegistrarCorrecaoRequest entrada) {
        var criada = correcoes.registrar(projetoId, entrada);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping("/correcoes")
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<CorrecaoRegistradaResponse> listarCorrecoes(@PathVariable Long projetoId, Pageable pageable) {
        var pagina = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(correcoes.listar(projetoId, pagina.getPageNumber(), pagina.getPageSize()));
    }
}
