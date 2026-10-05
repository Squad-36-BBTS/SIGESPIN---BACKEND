package br.com.bbts.sigespin.repository;

import br.com.bbts.sigespin.model.entity.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {

    List<Evidencia> findByProjetoIdOrderByDataUploadDesc(Long projetoId);
}
