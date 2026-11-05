package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.parecer.ParecerTecnicoRequest;
import br.com.bbts.sigespin.dto.parecer.ParecerTecnicoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrato de serviço para emissão e consulta de pareceres técnicos.
 * As regras de negócio transacionais e persistência são de responsabilidade da Fase 3.
 */
public interface ParecerTecnicoService {

    Page<ParecerTecnicoResponse> listarPorProjeto(Long projetoId, Pageable pageable);

    ParecerTecnicoResponse emitirParecer(Long projetoId, ParecerTecnicoRequest request, Long autorId);
}
