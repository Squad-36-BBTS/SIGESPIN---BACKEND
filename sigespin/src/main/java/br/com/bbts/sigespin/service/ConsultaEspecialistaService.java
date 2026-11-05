package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.parecer.ConsultaEspecialistaRequest;
import br.com.bbts.sigespin.dto.parecer.ConsultaEspecialistaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrato de serviço para consultas encaminhadas a especialistas.
 * As regras de negócio transacionais e persistência são de responsabilidade da Fase 3.
 */
public interface ConsultaEspecialistaService {

    Page<ConsultaEspecialistaResponse> listarPorProjeto(Long projetoId, Pageable pageable);

    ConsultaEspecialistaResponse encaminharConsulta(Long projetoId, ConsultaEspecialistaRequest request, Long solicitanteId);
}
