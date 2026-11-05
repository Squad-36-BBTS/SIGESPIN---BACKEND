package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.qualificacao.QualificacaoDemandaRequest;
import br.com.bbts.sigespin.dto.qualificacao.QualificacaoDemandaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrato de serviço para operações de qualificação de demandas.
 * As regras de negócio transacionais e persistência são de responsabilidade da Fase 3.
 */
public interface QualificacaoDemandaService {

    Page<QualificacaoDemandaResponse> listarPorProjeto(Long projetoId, Pageable pageable);

    QualificacaoDemandaResponse registrarQualificacao(Long projetoId, QualificacaoDemandaRequest request, Long autorId);
}
