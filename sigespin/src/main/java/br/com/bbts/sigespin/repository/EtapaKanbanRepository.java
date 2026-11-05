package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.EtapaKanban;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EtapaKanbanRepository extends JpaRepository<EtapaKanban, Long> {
    java.util.Optional<EtapaKanban> findByCodigoAndAtivaTrue(String codigo);
    java.util.List<EtapaKanban> findByAtivaTrueOrderByOrdemAscIdAsc();
}
