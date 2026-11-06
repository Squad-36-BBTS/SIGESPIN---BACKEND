package br.com.bbts.sigespin.repository.kanban;

import br.com.bbts.sigespin.model.entity.SolicitacaoConfiguracaoKanban;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SolicitacaoConfiguracaoKanbanRepository extends JpaRepository<SolicitacaoConfiguracaoKanban, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SolicitacaoConfiguracaoKanban s WHERE s.id = :id")
    Optional<SolicitacaoConfiguracaoKanban> findParaDecisao(@Param("id") Long id);
    Page<SolicitacaoConfiguracaoKanban> findByStatus(StatusSolicitacao status, Pageable pagina);
}
