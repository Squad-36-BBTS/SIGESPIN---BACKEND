-- Pedidos de acesso enviados pela rota pública. Sem senha: o usuário é criado à parte.
CREATE TABLE solicitacoes_acesso (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL CHECK (btrim(nome) <> ''),
    email VARCHAR(200) NOT NULL CHECK (btrim(email) <> ''),
    justificativa TEXT NOT NULL CHECK (btrim(justificativa) <> '' AND char_length(justificativa) <= 1000),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE')),
    criado_em TIMESTAMP NOT NULL
);

-- No máximo um pedido pendente por e-mail, inclusive sob requisições simultâneas.
CREATE UNIQUE INDEX uk_solicitacoes_acesso_email_pendente
    ON solicitacoes_acesso(email) WHERE status = 'PENDENTE';
