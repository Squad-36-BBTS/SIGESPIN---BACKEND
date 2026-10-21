package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.EvidenciaTrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenciaTrlRepository extends JpaRepository<EvidenciaTrl, Long> {
    Page<EvidenciaTrl> findByProjetoId(Long projetoId, Pageable pageable);

    Page<EvidenciaTrl> findByRegistroId(Long avaliacaoTrlId, Pageable pageable);
}
