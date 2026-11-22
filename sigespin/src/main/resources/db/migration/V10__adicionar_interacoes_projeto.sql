-- Comentários de um projeto. O autor vem sempre do usuário autenticado.
CREATE TABLE interacoes_projeto (
    id BIGSERIAL PRIMARY KEY,
    projeto_id BIGINT NOT NULL REFERENCES projetos(id),
    autor_id BIGINT NOT NULL REFERENCES usuarios(id),
    texto TEXT NOT NULL CHECK (btrim(texto) <> '' AND char_length(texto) <= 2000),
    criado_em TIMESTAMP NOT NULL
);

-- Atende a listagem paginada por projeto ordenada por data.
CREATE INDEX idx_interacoes_projeto ON interacoes_projeto(projeto_id, criado_em, id);

-- Comentários são histórico: não podem ser editados nem apagados.
CREATE TRIGGER proteger_interacoes_projeto BEFORE UPDATE OR DELETE ON interacoes_projeto
    FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
