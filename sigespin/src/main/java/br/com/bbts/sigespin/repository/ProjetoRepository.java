package br.com.bbts.sigespin.repository;

import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.StatusFunil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    @Query("""
            SELECT p FROM Projeto p
            WHERE (:arena IS NULL OR p.arena = :arena)
              AND (:status IS NULL OR p.statusFunil = :status)
              AND (:busca IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(p.descricaoDor) LIKE LOWER(CONCAT('%', :busca, '%')))
            """)
    Page<Projeto> findAllWithFilters(
            @Param("arena") ArenaTipo arena,
            @Param("status") StatusFunil status,
            @Param("busca") String busca,
            Pageable pageable
    );
}
