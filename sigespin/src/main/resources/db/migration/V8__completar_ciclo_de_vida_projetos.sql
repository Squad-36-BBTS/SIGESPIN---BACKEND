ALTER TABLE projetos
    ADD COLUMN arquivado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN arquivado_em TIMESTAMP,
    ADD COLUMN arquivado_por_id BIGINT REFERENCES usuarios(id),
    ADD COLUMN motivo_arquivamento TEXT,
    ADD CONSTRAINT chk_projeto_arquivamento CHECK (
        (NOT arquivado AND arquivado_em IS NULL AND arquivado_por_id IS NULL AND motivo_arquivamento IS NULL)
        OR (arquivado AND arquivado_em IS NOT NULL AND arquivado_por_id IS NOT NULL
            AND motivo_arquivamento IS NOT NULL AND btrim(motivo_arquivamento) <> '')
    );

CREATE TABLE correcoes_demanda (
    id BIGSERIAL PRIMARY KEY,
    projeto_id BIGINT NOT NULL REFERENCES projetos(id),
    autor_id BIGINT NOT NULL REFERENCES usuarios(id),
    descricao TEXT NOT NULL CHECK (btrim(descricao) <> ''),
    referencia VARCHAR(500),
    evidencia_id BIGINT,
    criado_em TIMESTAMP NOT NULL,
    FOREIGN KEY(evidencia_id, projeto_id) REFERENCES evidencias(id, projeto_id)
);
CREATE INDEX idx_correcoes_projeto ON correcoes_demanda(projeto_id, criado_em, id);
CREATE TRIGGER proteger_correcoes_demanda BEFORE UPDATE OR DELETE ON correcoes_demanda
    FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();

CREATE TABLE alteracoes_projeto (
    id BIGSERIAL PRIMARY KEY,
    projeto_id BIGINT NOT NULL REFERENCES projetos(id),
    autor_id BIGINT NOT NULL REFERENCES usuarios(id),
    tipo VARCHAR(255) NOT NULL CHECK (tipo IN ('CADASTRO','ARQUIVAMENTO','REATIVACAO')),
    justificativa TEXT NOT NULL CHECK (btrim(justificativa) <> ''),
    anterior JSONB NOT NULL CHECK (jsonb_typeof(anterior)='object'),
    atual JSONB NOT NULL CHECK (jsonb_typeof(atual)='object'),
    criado_em TIMESTAMP NOT NULL
);
CREATE INDEX idx_alteracoes_projeto ON alteracoes_projeto(projeto_id, criado_em, id);
CREATE TRIGGER proteger_alteracoes_projeto BEFORE UPDATE OR DELETE ON alteracoes_projeto
    FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();

CREATE TABLE recomendacoes_demanda (
    id BIGSERIAL PRIMARY KEY,
    autor_id BIGINT NOT NULL REFERENCES usuarios(id),
    destinatario_id BIGINT NOT NULL REFERENCES usuarios(id),
    titulo VARCHAR(255) NOT NULL CHECK (btrim(titulo) <> ''),
    descricao TEXT NOT NULL CHECK (btrim(descricao) <> ''),
    status VARCHAR(255) NOT NULL CHECK (status IN ('PENDENTE','CONVERTIDA','RECUSADA')),
    criado_em TIMESTAMP NOT NULL,
    projeto_id BIGINT UNIQUE REFERENCES projetos(id),
    encerrado_em TIMESTAMP,
    justificativa_encerramento TEXT,
    versao BIGINT NOT NULL DEFAULT 0,
    CHECK ((status='PENDENTE' AND projeto_id IS NULL AND encerrado_em IS NULL AND justificativa_encerramento IS NULL)
        OR (status IN ('CONVERTIDA','RECUSADA') AND encerrado_em IS NOT NULL AND encerrado_em >= criado_em
            AND justificativa_encerramento IS NOT NULL AND btrim(justificativa_encerramento) <> ''
            AND ((status='CONVERTIDA' AND projeto_id IS NOT NULL) OR (status='RECUSADA' AND projeto_id IS NULL))))
);
CREATE INDEX idx_recomendacoes_autor ON recomendacoes_demanda(autor_id, criado_em, id);
CREATE INDEX idx_recomendacoes_destinatario ON recomendacoes_demanda(destinatario_id, criado_em, id);

CREATE FUNCTION proteger_recomendacao_demanda() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        RAISE EXCEPTION 'Recomendacao historica' USING ERRCODE='23514';
    ELSIF TG_OP='INSERT' THEN
        IF NEW.status <> 'PENDENTE' THEN
            RAISE EXCEPTION 'Recomendacao deve iniciar pendente' USING ERRCODE='23514';
        END IF;
    ELSIF OLD.status <> 'PENDENTE' OR NEW.status='PENDENTE'
        OR (to_jsonb(NEW) - ARRAY['status','projeto_id','encerrado_em','justificativa_encerramento','versao'])
            IS DISTINCT FROM
           (to_jsonb(OLD) - ARRAY['status','projeto_id','encerrado_em','justificativa_encerramento','versao']) THEN
        RAISE EXCEPTION 'Somente encerramento de recomendacao pendente e permitido' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER proteger_recomendacoes_demanda BEFORE INSERT OR UPDATE OR DELETE ON recomendacoes_demanda
    FOR EACH ROW EXECUTE FUNCTION proteger_recomendacao_demanda();

CREATE OR REPLACE FUNCTION proteger_arquivo_referenciado() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS(SELECT 1 FROM evidencias_trl WHERE evidencia_id=OLD.id)
        OR EXISTS(SELECT 1 FROM evidencias_parecer WHERE evidencia_id=OLD.id)
        OR EXISTS(SELECT 1 FROM evidencias_transicao WHERE evidencia_id=OLD.id)
        OR EXISTS(SELECT 1 FROM correcoes_demanda WHERE evidencia_id=OLD.id)
        OR OLD.solicitacao_avanco_id IS NOT NULL THEN
        RAISE EXCEPTION 'Evidencia historica' USING ERRCODE='23514';
    END IF;
    RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END;
$$;
