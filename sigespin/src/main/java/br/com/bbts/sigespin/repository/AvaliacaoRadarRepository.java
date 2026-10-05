package br.com.bbts.sigespin.repository;

import br.com.bbts.sigespin.model.entity.AvaliacaoRadar;
import br.com.bbts.sigespin.model.enums.TipoLeituraRadar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface AvaliacaoRadarRepository extends JpaRepository<AvaliacaoRadar, Long> {

    List<AvaliacaoRadar> findByProjetoIdOrderByDataAvaliacaoDesc(Long projetoId);

    Optional<AvaliacaoRadar> findByProjetoIdAndTipoLeitura(Long projetoId, TipoLeituraRadar tipoLeitura);
}
