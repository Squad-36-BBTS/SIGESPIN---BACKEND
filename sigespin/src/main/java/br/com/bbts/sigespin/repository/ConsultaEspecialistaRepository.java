package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.ConsultaEspecialista;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConsultaEspecialistaRepository extends JpaRepository<ConsultaEspecialista, Long> {
    org.springframework.data.domain.Page<ConsultaEspecialista> findByProjetoId(Long projetoId, org.springframework.data.domain.Pageable pageable);
}
