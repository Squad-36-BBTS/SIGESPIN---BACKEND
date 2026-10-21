package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.TransicaoKanban;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TransicaoKanbanRepository extends JpaRepository<TransicaoKanban, Long> {
    java.util.List<TransicaoKanban> findByOrigemIdAndAtivaTrue(Long origemId);
}
