package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.MovimentacaoKanban;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MovimentacaoKanbanRepository extends JpaRepository<MovimentacaoKanban, Long> {
    org.springframework.data.domain.Page<MovimentacaoKanban> findByProjetoId(Long projetoId, org.springframework.data.domain.Pageable pageable);
}
