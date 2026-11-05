package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.entrega.EntregaProjetoRequest;
import br.com.bbts.sigespin.dto.entrega.EntregaProjetoResponse;
import br.com.bbts.sigespin.dto.entrega.ValidacaoEntregaRequest;
import br.com.bbts.sigespin.dto.entrega.ValidacaoEntregaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrato de serviço para submissão de entregas/MVPs e validações pelo demandante.
 * As regras de negócio transacionais e persistência são de responsabilidade da Fase 3.
 */
public interface EntregaService {

    Page<EntregaProjetoResponse> listarEntregasPorProjeto(Long projetoId, Pageable pageable);

    EntregaProjetoResponse submeterEntrega(Long projetoId, EntregaProjetoRequest request, Long autorId);

    Page<ValidacaoEntregaResponse> listarValidacoesPorProjeto(Long projetoId, Pageable pageable);

    ValidacaoEntregaResponse registrarValidacao(Long projetoId, Long entregaId, ValidacaoEntregaRequest request, Long demandanteId);
}
