package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.ValidacaoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ValidacaoEntregaRepository extends JpaRepository<ValidacaoEntrega, Long> {
    org.springframework.data.domain.Page<ValidacaoEntrega> findByProjetoId(Long projetoId, org.springframework.data.domain.Pageable pageable);
}
