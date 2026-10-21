package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.ParecerTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ParecerTecnicoRepository extends JpaRepository<ParecerTecnico, Long> {
    org.springframework.data.domain.Page<ParecerTecnico> findByProjetoId(Long projetoId, org.springframework.data.domain.Pageable pageable);
}
