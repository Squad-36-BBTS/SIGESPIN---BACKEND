package br.com.bbts.sigespin.repository;
import br.com.bbts.sigespin.model.entity.EvidenciaTransicao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenciaTransicaoRepository extends JpaRepository<EvidenciaTransicao, Long> {
    Page<EvidenciaTransicao> findByProjetoId(Long projetoId, Pageable pageable);

    Page<EvidenciaTransicao> findByRegistroId(Long solicitacaoTransicaoId, Pageable pageable);
}
