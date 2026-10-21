package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.SolicitacaoTransicao;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SolicitacaoTransicaoRepository extends JpaRepository<SolicitacaoTransicao, Long> {
    org.springframework.data.domain.Page<SolicitacaoTransicao> findByProjetoId(Long projetoId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<SolicitacaoTransicao> findByStatus(StatusSolicitacao status, org.springframework.data.domain.Pageable pageable);
}
