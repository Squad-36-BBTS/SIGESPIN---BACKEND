package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.QualificacaoDemanda;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QualificacaoDemandaRepository extends JpaRepository<QualificacaoDemanda, Long> {
    org.springframework.data.domain.Page<QualificacaoDemanda> findByProjetoId(Long projetoId, org.springframework.data.domain.Pageable pageable);
}
