package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.mobilizacao.AcaoMobilizacaoResponse;
import br.com.bbts.sigespin.repository.portfolio.AcaoMobilizacaoRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * BE-09 — Serviço para consulta de ações de mobilização.
 */
@Service
@Validated
public class AcaoMobilizacaoService {

    private final AcaoMobilizacaoRepository acaoMobilizacaoRepository;
    private final UsuarioAtualService usuarioAtual;

    public AcaoMobilizacaoService(AcaoMobilizacaoRepository acaoMobilizacaoRepository,
                                  UsuarioAtualService usuarioAtual) {
        this.acaoMobilizacaoRepository = acaoMobilizacaoRepository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<AcaoMobilizacaoResponse> listar() {
        usuarioAtual.obterAtivo();
        return acaoMobilizacaoRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"))
                .stream()
                .map(AcaoMobilizacaoResponse::de)
                .toList();
    }
}
