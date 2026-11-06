package br.com.bbts.sigespin.repository.kanban;
import br.com.bbts.sigespin.model.entity.EtapaKanban;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EtapaKanbanRepository extends JpaRepository<EtapaKanban, Long> {
    boolean existsByCodigo(String codigo);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM EtapaKanban e WHERE e.id = :id")
    java.util.Optional<EtapaKanban> findParaConfiguracao(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM EtapaKanban e WHERE e.id IN :ids ORDER BY e.id")
    java.util.List<EtapaKanban> findParaMovimentacao(@org.springframework.data.repository.query.Param("ids") java.util.List<Long> ids);
    java.util.Optional<EtapaKanban> findByCodigoAndAtivaTrue(String codigo);
    java.util.List<EtapaKanban> findByAtivaTrueOrderByOrdemAscIdAsc();
}
