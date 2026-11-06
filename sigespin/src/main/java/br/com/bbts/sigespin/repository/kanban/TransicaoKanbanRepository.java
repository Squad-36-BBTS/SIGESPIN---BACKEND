package br.com.bbts.sigespin.repository.kanban;
import br.com.bbts.sigespin.model.entity.TransicaoKanban;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TransicaoKanbanRepository extends JpaRepository<TransicaoKanban, Long> {
    boolean existsByOrigemIdAndDestinoId(Long origemId, Long destinoId);
    boolean existsByAtivaTrueAndOrigemId(Long origemId);
    boolean existsByAtivaTrueAndDestinoId(Long destinoId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TransicaoKanban t WHERE t.id = :id")
    java.util.Optional<TransicaoKanban> findParaConfiguracao(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TransicaoKanban t WHERE t.id = :id")
    java.util.Optional<TransicaoKanban> findParaExecucao(@org.springframework.data.repository.query.Param("id") Long id);
    java.util.List<TransicaoKanban> findByOrigemIdAndAtivaTrue(Long origemId);
}
