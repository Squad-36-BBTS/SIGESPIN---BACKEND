CREATE TABLE solicitacoes_configuracao_kanban (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(255) NOT NULL CHECK (tipo IN ('COLUNA','TRANSICAO')),
    solicitante_id BIGINT NOT NULL REFERENCES usuarios(id),
    codigo VARCHAR(255), nome VARCHAR(255), ordem INTEGER,
    terminal VARCHAR(255) CHECK (terminal IN ('CONCLUSAO','DESCONTINUACAO')),
    origem_id BIGINT REFERENCES etapas_kanban(id),
    destino_id BIGINT REFERENCES etapas_kanban(id),
    exige_governanca BOOLEAN,
    justificativa TEXT NOT NULL CHECK (btrim(justificativa) <> ''),
    status VARCHAR(255) NOT NULL CHECK (status IN ('PENDENTE','APROVADA','REJEITADA','CANCELADA')),
    criado_em TIMESTAMP NOT NULL,
    responsavel_encerramento_id BIGINT REFERENCES usuarios(id),
    justificativa_encerramento TEXT,
    encerrado_em TIMESTAMP,
    coluna_criada_id BIGINT UNIQUE REFERENCES etapas_kanban(id),
    transicao_criada_id BIGINT UNIQUE REFERENCES transicoes_kanban(id),
    versao BIGINT NOT NULL DEFAULT 0,
    CHECK (
        (tipo='COLUNA' AND codigo IS NOT NULL AND codigo ~ '^[A-Z][A-Z0-9_]{0,254}$'
            AND nome IS NOT NULL AND btrim(nome) <> '' AND ordem IS NOT NULL AND ordem >= 0
            AND origem_id IS NULL AND destino_id IS NULL AND exige_governanca IS NULL)
        OR
        (tipo='TRANSICAO' AND codigo IS NULL AND nome IS NULL AND ordem IS NULL AND terminal IS NULL
            AND origem_id IS NOT NULL AND destino_id IS NOT NULL AND origem_id <> destino_id
            AND exige_governanca IS NOT NULL)
    ),
    CHECK (
        (status='PENDENTE' AND responsavel_encerramento_id IS NULL
            AND justificativa_encerramento IS NULL AND encerrado_em IS NULL)
        OR
        (status<>'PENDENTE' AND responsavel_encerramento_id IS NOT NULL
            AND justificativa_encerramento IS NOT NULL AND btrim(justificativa_encerramento) <> ''
            AND encerrado_em IS NOT NULL AND encerrado_em >= criado_em)
    ),
    CHECK (
        (status<>'APROVADA' AND coluna_criada_id IS NULL AND transicao_criada_id IS NULL)
        OR
        (status='APROVADA' AND (
            (tipo='COLUNA' AND coluna_criada_id IS NOT NULL AND transicao_criada_id IS NULL)
            OR (tipo='TRANSICAO' AND transicao_criada_id IS NOT NULL AND coluna_criada_id IS NULL)))
    )
);

CREATE INDEX idx_pedidos_configuracao_status ON solicitacoes_configuracao_kanban(status, criado_em, id);

CREATE FUNCTION proteger_pedido_configuracao_kanban() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        RAISE EXCEPTION 'Pedido de configuracao historico' USING ERRCODE='23514';
    ELSIF TG_OP='INSERT' THEN
        IF NEW.status <> 'PENDENTE' THEN
            RAISE EXCEPTION 'Pedido deve iniciar pendente' USING ERRCODE='23514';
        END IF;
    ELSE
        IF OLD.status <> 'PENDENTE' OR NEW.status = 'PENDENTE'
            OR (to_jsonb(NEW) - ARRAY['status','responsavel_encerramento_id',
                'justificativa_encerramento','encerrado_em','coluna_criada_id','transicao_criada_id','versao'])
            IS DISTINCT FROM
               (to_jsonb(OLD) - ARRAY['status','responsavel_encerramento_id',
                'justificativa_encerramento','encerrado_em','coluna_criada_id','transicao_criada_id','versao']) THEN
            RAISE EXCEPTION 'Somente encerramento do pedido pendente e permitido' USING ERRCODE='23514';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER proteger_pedidos_configuracao_kanban
    BEFORE INSERT OR UPDATE OR DELETE ON solicitacoes_configuracao_kanban
    FOR EACH ROW EXECUTE FUNCTION proteger_pedido_configuracao_kanban();
