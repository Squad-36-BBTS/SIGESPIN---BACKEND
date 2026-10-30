CREATE TABLE tokens_jwt_revogados (
    jti VARCHAR(36) PRIMARY KEY,
    expira_em TIMESTAMPTZ NOT NULL,
    revogado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tokens_jwt_revogados_expira_em
    ON tokens_jwt_revogados(expira_em);
