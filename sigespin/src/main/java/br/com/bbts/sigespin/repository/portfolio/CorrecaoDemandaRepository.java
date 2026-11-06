package br.com.bbts.sigespin.repository.portfolio;

import br.com.bbts.sigespin.model.entity.CorrecaoDemanda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

public interface CorrecaoDemandaRepository extends JpaRepository<CorrecaoDemanda, Long> {
    Page<CorrecaoDemanda> findByProjetoId(Long projetoId, Pageable pagina);
}
