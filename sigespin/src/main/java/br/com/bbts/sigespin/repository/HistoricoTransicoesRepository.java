package br.com.bbts.sigespin.repository;

import br.com.bbts.sigespin.model.entity.HistoricoTransicoes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoTransicoesRepository extends JpaRepository<HistoricoTransicoes, Long> {

    List<HistoricoTransicoes> findByProjetoIdOrderByDataMudancaDesc(Long projetoId);
}
