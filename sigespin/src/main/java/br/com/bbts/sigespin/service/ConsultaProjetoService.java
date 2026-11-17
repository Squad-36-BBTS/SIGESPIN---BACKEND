package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.gestao.ProjetoConsultadoResponse;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ConsultaProjetoService {
    private final ProjetoRepository projetos;
    private final UsuarioAtualService usuarioAtual;

    public ConsultaProjetoService(ProjetoRepository projetos, UsuarioAtualService usuarioAtual) {
        this.projetos = projetos;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ProjetoConsultadoResponse detalhar(@NotNull @Positive Long id) {
        usuarioAtual.obterAtivo();
        return ProjetoConsultadoResponse.de(projetos.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado.")));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<ProjetoConsultadoResponse> listar(ArenaTipo arena, @Positive Long etapaId,
            @Size(max = 255) String busca, Boolean arquivado,
            @Min(0) int pagina, @Min(1) @Max(100) int tamanho) {
        usuarioAtual.obterAtivo();
        String termo = busca == null || busca.isBlank() ? null : busca.strip();
        return projetos.consultarPortfolio(arena, etapaId, termo, arquivado,
                PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id")))
                .map(ProjetoConsultadoResponse::de);
    }
}
