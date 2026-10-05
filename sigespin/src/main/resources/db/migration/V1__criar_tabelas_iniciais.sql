-- =============================================================================
-- V1__criar_tabelas_iniciais.sql
-- Migration Flyway: Cria toda a estrutura de tabelas do SIGES-PIN
--
-- IMPORTANTE: Este script é executado UMA VEZ pelo Flyway.
-- Após executado, NUNCA deve ser alterado. Se precisar mudar algo,
-- crie uma nova migration (V2__...).
-- =============================================================================

-- 1. Tabela de Usuários
-- Armazena os utilizadores do sistema com seus perfis de acesso.
CREATE TABLE usuarios (
    id              BIGSERIAL       PRIMARY KEY,
    nome            VARCHAR(150)    NOT NULL,
    email           VARCHAR(200)    NOT NULL UNIQUE,
    senha_hash      VARCHAR(255)    NOT NULL,
    perfil          VARCHAR(30)     NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,

    -- CONSTRAINT de validação: garante que o perfil seja um dos valores válidos.
    -- Mesmo que o Java valide no Enum, é boa prática ter a regra no banco também
    -- (defesa em profundidade).
    CONSTRAINT chk_usuarios_perfil CHECK (perfil IN ('DEMANDANTE', 'ANALISTA', 'COMITE', 'EXECUTIVO'))
);

-- 2. Tabela de Projetos
-- Entidade central do sistema. Todos os demais registros se relacionam com ela.
CREATE TABLE projetos (
    id                      BIGSERIAL       PRIMARY KEY,
    titulo                  VARCHAR(255)    NOT NULL,
    descricao_dor           TEXT            NOT NULL,
    area_demandante_id      BIGINT          NOT NULL,
    responsavel_po_id       BIGINT          NOT NULL,
    arena                   VARCHAR(30)     NOT NULL,
    status_funil            VARCHAR(20)     NOT NULL DEFAULT 'IDEACAO',
    trl_atual               INTEGER         NOT NULL,
    hipotese_solucao        TEXT,
    potencial_lei_do_bem    BOOLEAN,
    dispendio_estimado      NUMERIC(12,2),
    criado_em               TIMESTAMP       NOT NULL DEFAULT NOW(),
    atualizado_em           TIMESTAMP,

    -- FK: Cada projeto tem um responsável (PO) que é um usuário do sistema.
    CONSTRAINT fk_projetos_responsavel FOREIGN KEY (responsavel_po_id) REFERENCES usuarios(id),

    -- Validações no nível do banco:
    CONSTRAINT chk_projetos_arena CHECK (arena IN ('EFICIENCIA_OPERACIONAL', 'NEGOCIOS_DIGITAIS', 'SEGURANCA_INTEGRADA', 'A_DEFINIR')),
    CONSTRAINT chk_projetos_status CHECK (status_funil IN ('IDEACAO', 'POC', 'MVP', 'CONCLUIDO', 'DESCONTINUADO')),
    CONSTRAINT chk_projetos_trl CHECK (trl_atual BETWEEN 1 AND 9)
);

-- 3. Tabela de Deliberações (Histórico de Decisões do Comitê)
-- Registra cada decisão formal: APROVADO, PIVOTADO ou REPROVADO.
CREATE TABLE deliberacao_historico (
    id              BIGSERIAL       PRIMARY KEY,
    projeto_id      BIGINT          NOT NULL,
    responsavel_id  BIGINT          NOT NULL,
    decisao         VARCHAR(20)     NOT NULL,
    justificativa   TEXT            NOT NULL,
    data_decisao    TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_deliberacao_projeto FOREIGN KEY (projeto_id) REFERENCES projetos(id),
    CONSTRAINT fk_deliberacao_responsavel FOREIGN KEY (responsavel_id) REFERENCES usuarios(id),
    CONSTRAINT chk_deliberacao_decisao CHECK (decisao IN ('APROVADO', 'PIVOTADO', 'REPROVADO'))
);

-- 4. Tabela de Avaliações do Radar de Maturidade
-- Cada avaliação tem 4 scores (0.00 a 4.00) representando as dimensões do radar.
CREATE TABLE avaliacao_radar (
    id              BIGSERIAL       PRIMARY KEY,
    projeto_id      BIGINT          NOT NULL,
    tipo_leitura    VARCHAR(10)     NOT NULL,
    score_negocio   NUMERIC(3,2)    NOT NULL,
    score_produto   NUMERIC(3,2)    NOT NULL,
    score_time      NUMERIC(3,2)    NOT NULL,
    score_receita   NUMERIC(3,2)    NOT NULL,
    data_avaliacao  TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_radar_projeto FOREIGN KEY (projeto_id) REFERENCES projetos(id),
    CONSTRAINT chk_radar_tipo CHECK (tipo_leitura IN ('INICIAL', 'ATUAL')),
    CONSTRAINT chk_radar_negocio CHECK (score_negocio BETWEEN 0.00 AND 4.00),
    CONSTRAINT chk_radar_produto CHECK (score_produto BETWEEN 0.00 AND 4.00),
    CONSTRAINT chk_radar_time CHECK (score_time BETWEEN 0.00 AND 4.00),
    CONSTRAINT chk_radar_receita CHECK (score_receita BETWEEN 0.00 AND 4.00)
);

-- 5. Tabela de Histórico de Transições (Auditoria)
-- Log imutável de todas as mudanças de status e TRL.
CREATE TABLE historico_transicoes (
    id              BIGSERIAL       PRIMARY KEY,
    projeto_id      BIGINT          NOT NULL,
    usuario_id      BIGINT          NOT NULL,
    tipo_mudanca    VARCHAR(20)     NOT NULL,
    valor_anterior  VARCHAR(30)     NOT NULL,
    valor_novo      VARCHAR(30)     NOT NULL,
    data_mudanca    TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transicao_projeto FOREIGN KEY (projeto_id) REFERENCES projetos(id),
    CONSTRAINT fk_transicao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_transicao_tipo CHECK (tipo_mudanca IN ('STATUS_FUNIL', 'TRL'))
);

-- 6. Tabela de Evidências
-- Referências a arquivos armazenados no AWS S3.
CREATE TABLE evidencias (
    id                  BIGSERIAL       PRIMARY KEY,
    projeto_id          BIGINT          NOT NULL,
    nome_arquivo        VARCHAR(255)    NOT NULL,
    caminho_storage     VARCHAR(500)    NOT NULL,
    tipo_arquivo        VARCHAR(100)    NOT NULL,
    data_upload         TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_evidencia_projeto FOREIGN KEY (projeto_id) REFERENCES projetos(id)
);

-- =============================================================================
-- Índices para Performance
-- Índices aceleram consultas frequentes. Sem eles, o banco faria "full table scan"
-- (ler todas as linhas) a cada busca por email ou projeto_id.
-- =============================================================================
CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_projetos_arena ON projetos(arena);
CREATE INDEX idx_projetos_status ON projetos(status_funil);
CREATE INDEX idx_projetos_responsavel ON projetos(responsavel_po_id);
CREATE INDEX idx_deliberacao_projeto ON deliberacao_historico(projeto_id);
CREATE INDEX idx_radar_projeto ON avaliacao_radar(projeto_id);
CREATE INDEX idx_transicao_projeto ON historico_transicoes(projeto_id);
CREATE INDEX idx_evidencia_projeto ON evidencias(projeto_id);
