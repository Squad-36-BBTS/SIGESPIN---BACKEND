package br.com.bbts.sigespin.repository.portfolio;

import br.com.bbts.sigespin.model.entity.AlteracaoProjeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

public interface AlteracaoProjetoRepository extends JpaRepository<AlteracaoProjeto, Long> {
    Page<AlteracaoProjeto> findByProjetoId(Long projetoId, Pageable pagina);
}
