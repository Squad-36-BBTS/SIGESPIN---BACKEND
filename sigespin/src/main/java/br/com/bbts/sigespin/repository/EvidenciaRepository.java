package br.com.bbts.sigespin.repository;

import br.com.bbts.sigespin.model.entity.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {

    // Mantém metadados estáveis enquanto o conjunto documental é montado.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Evidencia e WHERE e.id IN :ids ORDER BY e.id")
    List<Evidencia> findParaVinculo(@org.springframework.data.repository.query.Param("ids") List<Long> ids);

    List<Evidencia> findByProjetoIdOrderByDataUploadDesc(Long projetoId);

    List<Evidencia> findBySolicitacaoAvancoIdOrderByDataUploadDesc(Long solicitacaoAvancoId);

    boolean existsBySolicitacaoAvancoId(Long solicitacaoAvancoId);
}
