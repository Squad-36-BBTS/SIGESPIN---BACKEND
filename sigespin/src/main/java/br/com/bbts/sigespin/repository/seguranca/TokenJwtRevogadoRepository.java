package br.com.bbts.sigespin.repository.seguranca;

import br.com.bbts.sigespin.model.entity.TokenJwtRevogado;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface TokenJwtRevogadoRepository extends JpaRepository<TokenJwtRevogado, String> {
    long deleteByExpiraEmBefore(Instant instante);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO tokens_jwt_revogados(jti, expira_em, revogado_em) VALUES (:jti, :expiraEm, :revogadoEm) ON CONFLICT (jti) DO NOTHING", nativeQuery = true)
    int inserirSeAusente(@Param("jti") String jti, @Param("expiraEm") Instant expiraEm,
                         @Param("revogadoEm") Instant revogadoEm);
}
