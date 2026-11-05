package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.comum.PaginaResponse;
import br.com.bbts.sigespin.dto.qualificacao.QualificacaoDemandaRequest;
import br.com.bbts.sigespin.dto.qualificacao.QualificacaoDemandaResponse;
import br.com.bbts.sigespin.exception.ServicoIndisponivelException;
import br.com.bbts.sigespin.security.UsuarioPrincipal;
import br.com.bbts.sigespin.service.QualificacaoDemandaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/projetos/{projetoId}/qualificacoes")
public class QualificacaoDemandaController {

    private final QualificacaoDemandaService qualificacaoService;

    public QualificacaoDemandaController(@Autowired(required = false) QualificacaoDemandaService qualificacaoService) {
        this.qualificacaoService = qualificacaoService;
    }

    private QualificacaoDemandaService obterServico() {
        if (this.qualificacaoService == null) {
            throw new ServicoIndisponivelException("O serviço de qualificação de demandas da Fase 3 ainda não foi integrado.");
        }
        return this.qualificacaoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaResponse<QualificacaoDemandaResponse> listarPorProjeto(
            @PathVariable("projetoId") Long projetoId,
            Pageable pageable) {
        Pageable seguro = PaginacaoUtils.ajustarLimite(pageable);
        return PaginaResponse.de(obterServico().listarPorProjeto(projetoId, seguro));
    }

    @PostMapping
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<QualificacaoDemandaResponse> registrarQualificacao(
            @PathVariable("projetoId") Long projetoId,
            @Valid @RequestBody QualificacaoDemandaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long autorId = principal != null && principal.getUsuario() != null ? principal.getUsuario().getId() : null;
        QualificacaoDemandaResponse criada = obterServico().registrarQualificacao(projetoId, request, autorId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();

        return ResponseEntity.created(location).body(criada);
    }
}
