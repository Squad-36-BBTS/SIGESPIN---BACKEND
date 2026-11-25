package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.projeto.DemandaCreateRequest;
import br.com.bbts.sigespin.dto.projeto.ProjetoDetalheResponse;
import br.com.bbts.sigespin.dto.projeto.ProjetoResponse;
import br.com.bbts.sigespin.dto.projeto.FiltroPortfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrato de serviço para operações do portfólio de projetos e demandas.
 * As regras de negócio transacionais e persistência são de responsabilidade da Fase 3.
 */
public interface ProjetoService {

    Page<ProjetoResponse> listarProjetos(FiltroPortfolio filtro, Pageable pageable);

    ProjetoDetalheResponse buscarPorId(Long id);

    ProjetoDetalheResponse criarDemanda(DemandaCreateRequest request, Long usuarioAutenticadoId);
}
