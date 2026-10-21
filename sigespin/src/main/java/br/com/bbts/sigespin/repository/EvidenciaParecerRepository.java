package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.EvidenciaParecer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenciaParecerRepository extends JpaRepository<EvidenciaParecer, Long> {
    Page<EvidenciaParecer> findByProjetoId(Long projetoId, Pageable pageable);

    Page<EvidenciaParecer> findByRegistroId(Long parecerId, Pageable pageable);
}
