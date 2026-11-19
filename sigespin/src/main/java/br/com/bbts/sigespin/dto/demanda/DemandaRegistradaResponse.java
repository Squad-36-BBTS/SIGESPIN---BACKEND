package br.com.bbts.sigespin.dto.demanda;

import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import java.time.LocalDateTime;

/** Snapshot da gravação: evita devolver a entidade JPA e seus relacionamentos. */
public record DemandaRegistradaResponse(
        Long id, String titulo, Long demandanteId, OrigemDemanda origem,
        Long acaoMobilizacaoId, Long etapaId, Integer trlInicial, LocalDateTime criadoEm) {
}
