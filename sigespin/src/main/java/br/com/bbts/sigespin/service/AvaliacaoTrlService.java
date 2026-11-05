package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.trl.AvaliacaoTrlRequest;
import br.com.bbts.sigespin.dto.trl.AvaliacaoTrlResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrato de serviço para avaliações de maturidade tecnológica (TRL).
 * As regras de negócio transacionais e persistência são de responsabilidade da Fase 3.
 */
public interface AvaliacaoTrlService {

    Page<AvaliacaoTrlResponse> listarPorProjeto(Long projetoId, Pageable pageable);

    AvaliacaoTrlResponse registrarAvaliacao(Long projetoId, AvaliacaoTrlRequest request, Long autorId);
}
