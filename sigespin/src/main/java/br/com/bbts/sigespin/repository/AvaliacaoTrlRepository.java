package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.AvaliacaoTrl;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AvaliacaoTrlRepository extends JpaRepository<AvaliacaoTrl, Long> {
    org.springframework.data.domain.Page<AvaliacaoTrl> findByProjetoId(Long projetoId, org.springframework.data.domain.Pageable pageable);
}
