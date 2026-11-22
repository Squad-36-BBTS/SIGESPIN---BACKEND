package br.com.bbts.sigespin.repository.portfolio;

import br.com.bbts.sigespin.model.entity.InteracaoProjeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteracaoProjetoRepository extends JpaRepository<InteracaoProjeto, Long> {
    /** Carrega o autor junto para montar a resposta sem uma consulta por comentário. */
    @EntityGraph(attributePaths = "autor")
    Page<InteracaoProjeto> findByProjetoId(Long projetoId, Pageable pageable);
}
