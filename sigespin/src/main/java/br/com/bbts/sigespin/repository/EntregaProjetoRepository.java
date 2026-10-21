package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.EntregaProjeto;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EntregaProjetoRepository extends JpaRepository<EntregaProjeto, Long> {
    org.springframework.data.domain.Page<EntregaProjeto> findByProjetoId(Long projetoId, org.springframework.data.domain.Pageable pageable);
}
