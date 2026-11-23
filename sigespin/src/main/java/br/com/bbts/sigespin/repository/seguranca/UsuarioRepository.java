package br.com.bbts.sigespin.repository.seguranca;

import br.com.bbts.sigespin.model.entity.Usuario;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Lista usuários com filtros opcionais de perfil e situação, ordenados por nome. */
    @Query("SELECT u FROM Usuario u " +
           "WHERE (:perfil IS NULL OR u.perfil = :perfil) " +
           "AND (:ativo IS NULL OR u.ativo = :ativo) " +
           "ORDER BY u.nome")
    List<Usuario> listarComFiltros(@Param("perfil") PerfilUsuario perfil,
                                   @Param("ativo") Boolean ativo);
}
