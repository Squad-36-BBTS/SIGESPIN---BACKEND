CREATE TABLE notificacoes_kanban (
    id BIGSERIAL PRIMARY KEY,
    movimentacao_id BIGINT NOT NULL UNIQUE REFERENCES movimentacoes_kanban(id),
    mensagem TEXT NOT NULL CHECK (btrim(mensagem) <> ''),
    criada_em TIMESTAMP NOT NULL
);

CREATE INDEX idx_notificacoes_kanban_criacao ON notificacoes_kanban(criada_em DESC, id DESC);

CREATE TRIGGER proteger_notificacoes_kanban
    BEFORE UPDATE OR DELETE ON notificacoes_kanban
    FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
