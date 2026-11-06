package br.com.bbts.sigespin.repository.portfolio;

import br.com.bbts.sigespin.model.entity.RecomendacaoDemanda;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface RecomendacaoDemandaRepository extends JpaRepository<RecomendacaoDemanda, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecomendacaoDemanda r WHERE r.id = :id")
    Optional<RecomendacaoDemanda> findParaResposta(@Param("id") Long id);
    @Query("""
        SELECT r FROM RecomendacaoDemanda r
        WHERE (:gestor = true OR r.autor.id = :usuarioId OR r.destinatario.id = :usuarioId)
          AND (:status IS NULL OR r.status = :status)
        """)
    Page<RecomendacaoDemanda> consultar(@Param("usuarioId") Long usuarioId,
            @Param("gestor") boolean gestor,
            @Param("status") br.com.bbts.sigespin.model.enums.StatusRecomendacao status, Pageable pagina);
}
