package br.com.bbts.sigespin.repository.kanban;
import br.com.bbts.sigespin.model.entity.SolicitacaoTransicao;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SolicitacaoTransicaoRepository extends JpaRepository<SolicitacaoTransicao, Long> {
    boolean existsByTransicaoIdAndStatus(Long transicaoId, StatusSolicitacao status);
    boolean existsByProjetoIdAndStatus(Long projetoId, StatusSolicitacao status);

    @org.springframework.data.jpa.repository.Query("SELECT s.projeto.id FROM SolicitacaoTransicao s WHERE s.id = :id")
    java.util.Optional<Long> findProjetoId(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM SolicitacaoTransicao s WHERE s.id = :id")
    java.util.Optional<SolicitacaoTransicao> findParaDecisao(@org.springframework.data.repository.query.Param("id") Long id);
    org.springframework.data.domain.Page<SolicitacaoTransicao> findByProjetoId(Long projetoId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<SolicitacaoTransicao> findByStatus(StatusSolicitacao status, org.springframework.data.domain.Pageable pageable);
}
