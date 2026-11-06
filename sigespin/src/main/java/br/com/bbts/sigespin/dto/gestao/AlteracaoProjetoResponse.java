package br.com.bbts.sigespin.dto.gestao;

import br.com.bbts.sigespin.model.enums.TipoAlteracaoProjeto;
import java.time.LocalDateTime;
import java.util.Map;

public record AlteracaoProjetoResponse(Long id, Long projetoId, Long autorId, TipoAlteracaoProjeto tipo,
        String justificativa, Map<String, Object> anterior, Map<String, Object> atual, LocalDateTime criadoEm) {
}
