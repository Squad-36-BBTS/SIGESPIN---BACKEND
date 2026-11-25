package br.com.bbts.sigespin.repository.portfolio;

import br.com.bbts.sigespin.model.entity.Projeto;
import br.com.bbts.sigespin.model.enums.ArenaTipo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, Long>, JpaSpecificationExecutor<Projeto> {

    // Portfólio filtrado: já traz etapa, pessoas e ação de mobilização, evitando uma consulta por projeto.
    @Override
    @EntityGraph(attributePaths = {"etapa", "demandante", "analistaResponsavel", "responsavelPo", "acaoMobilizacao"})
    Page<Projeto> findAll(Specification<Projeto> filtro, Pageable pagina);
    boolean existsByEtapaId(Long etapaId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Projeto p WHERE p.id = :id AND p.arquivado = false")
    java.util.Optional<Projeto> findAtivoParaAtualizacao(@Param("id") Long id);

    @Query("""
        SELECT p FROM Projeto p WHERE (:arena IS NULL OR p.arena = :arena)
        AND (:etapaId IS NULL OR p.etapa.id = :etapaId)
        AND (:arquivado IS NULL OR p.arquivado = :arquivado)
        AND (:busca IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
             OR LOWER(p.descricaoDor) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
        """)
    Page<Projeto> consultarPortfolio(@Param("arena") ArenaTipo arena, @Param("etapaId") Long etapaId,
            @Param("busca") String busca, @Param("arquivado") Boolean arquivado, Pageable pagina);

    // Serializa avaliações do mesmo projeto para registrar corretamente o nível anterior.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Projeto p WHERE p.id = :id")
    java.util.Optional<Projeto> findByIdParaAtualizacao(@Param("id") Long id);

    // O contrato anterior compartilha a mesma consulta e mantém a leitura de todo o portfólio.
    default Page<Projeto> findAllWithFilters(ArenaTipo arena, Long etapaId, String busca, Pageable pageable) {
        return consultarPortfolio(arena, etapaId, busca, null, pageable);
    }
}
