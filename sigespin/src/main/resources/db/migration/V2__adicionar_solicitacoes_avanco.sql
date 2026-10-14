ALTER TABLE projetos
    ADD COLUMN demandante_id BIGINT REFERENCES usuarios(id),
    ADD COLUMN analista_responsavel_id BIGINT REFERENCES usuarios(id),
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_projetos_demandante ON projetos(demandante_id);
CREATE INDEX idx_projetos_analista ON projetos(analista_responsavel_id);

CREATE TABLE solicitacoes_avanco (
    id BIGSERIAL PRIMARY KEY,
    projeto_id BIGINT NOT NULL REFERENCES projetos(id),
    solicitante_id BIGINT NOT NULL REFERENCES usuarios(id),
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    justificativa TEXT NOT NULL,
    trl_anterior INTEGER,
    trl_pretendido INTEGER,
    status_anterior VARCHAR(20),
    status_pretendido VARCHAR(20),
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    responsavel_encerramento_id BIGINT REFERENCES usuarios(id),
    justificativa_encerramento TEXT,
    encerrado_em TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_solicitacao_projeto UNIQUE (id, projeto_id),
    CONSTRAINT chk_solicitacao_tipo CHECK (tipo IN ('TRL', 'STATUS_FUNIL')),
    CONSTRAINT chk_solicitacao_status CHECK (status IN ('PENDENTE', 'APROVADA', 'REJEITADA', 'CANCELADA')),
    CONSTRAINT chk_solicitacao_justificativa CHECK (LENGTH(TRIM(justificativa)) > 0),
    CONSTRAINT chk_solicitacao_alvo CHECK (
        (tipo = 'TRL'
            AND trl_anterior IS NOT NULL AND trl_pretendido IS NOT NULL
            AND trl_anterior BETWEEN 1 AND 9 AND trl_pretendido BETWEEN 1 AND 9
            AND trl_pretendido > trl_anterior
            AND status_anterior IS NULL AND status_pretendido IS NULL)
        OR
        (tipo = 'STATUS_FUNIL'
            AND trl_anterior IS NULL AND trl_pretendido IS NULL
            AND status_anterior IS NOT NULL AND status_pretendido IS NOT NULL
            AND ((status_anterior = 'IDEACAO' AND status_pretendido = 'POC')
                OR (status_anterior = 'POC' AND status_pretendido = 'MVP')
                OR (status_anterior = 'MVP' AND status_pretendido = 'CONCLUIDO')))
    ),
    CONSTRAINT chk_solicitacao_encerramento CHECK (
        (status = 'PENDENTE'
            AND responsavel_encerramento_id IS NULL
            AND justificativa_encerramento IS NULL AND encerrado_em IS NULL)
        OR
        (status IN ('APROVADA', 'REJEITADA', 'CANCELADA')
            AND responsavel_encerramento_id IS NOT NULL
            AND justificativa_encerramento IS NOT NULL
            AND LENGTH(TRIM(justificativa_encerramento)) > 0
            AND encerrado_em IS NOT NULL AND encerrado_em >= criado_em)
    )
);

CREATE INDEX idx_solicitacao_projeto_data ON solicitacoes_avanco(projeto_id, criado_em DESC);
CREATE INDEX idx_solicitacao_status_data ON solicitacoes_avanco(status, criado_em);
CREATE UNIQUE INDEX uq_solicitacao_pendente_tipo ON solicitacoes_avanco(projeto_id, tipo)
    WHERE status = 'PENDENTE';

ALTER TABLE evidencias
    ADD COLUMN solicitacao_avanco_id BIGINT,
    ADD CONSTRAINT fk_evidencia_solicitacao_projeto
        FOREIGN KEY (solicitacao_avanco_id, projeto_id)
        REFERENCES solicitacoes_avanco(id, projeto_id);

CREATE INDEX idx_evidencia_solicitacao ON evidencias(solicitacao_avanco_id);

ALTER TABLE historico_transicoes
    ADD COLUMN solicitacao_avanco_id BIGINT,
    ADD CONSTRAINT fk_transicao_solicitacao_projeto
        FOREIGN KEY (solicitacao_avanco_id, projeto_id)
        REFERENCES solicitacoes_avanco(id, projeto_id);

CREATE UNIQUE INDEX uq_transicao_solicitacao ON historico_transicoes(solicitacao_avanco_id)
    WHERE solicitacao_avanco_id IS NOT NULL;
