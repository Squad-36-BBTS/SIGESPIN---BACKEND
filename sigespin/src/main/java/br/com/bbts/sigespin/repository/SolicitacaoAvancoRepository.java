package br.com.bbts.sigespin.repository;

import br.com.bbts.sigespin.model.entity.SolicitacaoAvanco;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import br.com.bbts.sigespin.model.enums.TipoMudanca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Consulta do legado V2. Escritas sao bloqueadas pelo banco; nao usar como fila ativa. */
@Deprecated
@Repository
public interface SolicitacaoAvancoRepository extends JpaRepository<SolicitacaoAvanco, Long> {

    List<SolicitacaoAvanco> findByProjetoIdOrderByCriadoEmDesc(Long projetoId);

    Page<SolicitacaoAvanco> findByStatusOrderByCriadoEmAsc(StatusSolicitacao status, Pageable pageable);

    boolean existsByProjetoIdAndTipoAndStatus(Long projetoId, TipoMudanca tipo, StatusSolicitacao status);
}
