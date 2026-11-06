package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.model.entity.Evidencia;
import br.com.bbts.sigespin.repository.evidencia.EvidenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/** Compartilha apenas a validação documental; a autorização pertence a cada caso de uso. */
@Service
public class EvidenciasDocumentoService {
    private final EvidenciaRepository evidencias;

    public EvidenciasDocumentoService(EvidenciaRepository evidencias) {
        this.evidencias = evidencias;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<Evidencia> carregarDoProjeto(Long projetoId, List<Long> ids) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new RegraNegocioException("Uma evidência não pode ser repetida no mesmo registro.");
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        var encontrados = evidencias.findParaVinculo(ids);
        if (encontrados.size() != ids.size()) {
            throw new RegraNegocioException("Uma ou mais evidências não foram encontradas.");
        }
        if (encontrados.stream().anyMatch(e -> !e.getProjeto().getId().equals(projetoId))) {
            throw new RegraNegocioException("Todas as evidências precisam pertencer ao mesmo projeto.");
        }
        return encontrados;
    }
}
