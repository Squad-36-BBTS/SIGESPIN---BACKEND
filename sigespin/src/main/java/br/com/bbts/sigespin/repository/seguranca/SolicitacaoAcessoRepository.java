package br.com.bbts.sigespin.repository.seguranca;

import br.com.bbts.sigespin.model.entity.SolicitacaoAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SolicitacaoAcessoRepository extends JpaRepository<SolicitacaoAcesso, Long> {

    /**
     * Grava o pedido, a menos que já exista um pendente para o e-mail. O banco decide
     * de forma atômica, então requisições simultâneas não geram erro. Retorna 1 se gravou.
     */
    @Modifying
    @Query(value = """
            INSERT INTO solicitacoes_acesso (nome, email, justificativa, status, criado_em)
            VALUES (:nome, :email, :justificativa, 'PENDENTE', :criadoEm)
            ON CONFLICT (email) WHERE status = 'PENDENTE' DO NOTHING
            """, nativeQuery = true)
    int registrarSeNaoHouverPendente(@Param("nome") String nome, @Param("email") String email,
            @Param("justificativa") String justificativa, @Param("criadoEm") LocalDateTime criadoEm);
}
