package br.com.bbts.sigespin.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tokens_jwt_revogados")
@Getter
@Setter
@NoArgsConstructor
public class TokenJwtRevogado {

    @Id
    @Column(name = "jti", nullable = false, length = 36)
    private String jti;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "revogado_em", nullable = false)
    private Instant revogadoEm;

    public TokenJwtRevogado(String jti, Instant expiraEm, Instant revogadoEm) {
        this.jti = jti;
        this.expiraEm = expiraEm;
        this.revogadoEm = revogadoEm;
    }
}
