package br.com.bbts.sigespin.repository;

import br.com.bbts.sigespin.model.entity.DeliberacaoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliberacaoHistoricoRepository extends JpaRepository<DeliberacaoHistorico, Long> {

    List<DeliberacaoHistorico> findByProjetoIdOrderByDataDecisaoDesc(Long projetoId);
}
