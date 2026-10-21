ALTER TABLE avaliacoes_trl
    ADD COLUMN evidencias_fechadas BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE avaliacoes_trl
    ALTER COLUMN evidencias_fechadas SET DEFAULT FALSE;

ALTER TABLE pareceres_tecnicos
    ADD COLUMN evidencias_fechadas BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE pareceres_tecnicos
    ALTER COLUMN evidencias_fechadas SET DEFAULT FALSE;

DROP TRIGGER validar_anexo_trl ON evidencias_trl;
DROP TRIGGER validar_anexo_parecer ON evidencias_parecer;
DROP FUNCTION validar_anexo_avaliacao();
DROP TRIGGER proteger_avaliacoes_trl ON avaliacoes_trl;
DROP TRIGGER proteger_pareceres_tecnicos ON pareceres_tecnicos;

CREATE FUNCTION proteger_registro_documental() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Registro documental historico imutavel: %', TG_TABLE_NAME USING ERRCODE = '23514';
    END IF;

    IF TG_OP = 'INSERT' THEN
        IF NEW.evidencias_fechadas IS DISTINCT FROM FALSE THEN
            RAISE EXCEPTION 'Registro documental deve iniciar aberto: %', TG_TABLE_NAME USING ERRCODE = '23514';
        END IF;
        RETURN NEW;
    END IF;

    IF OLD.evidencias_fechadas IS DISTINCT FROM FALSE
        OR NEW.evidencias_fechadas IS DISTINCT FROM TRUE
        OR (to_jsonb(NEW) - 'evidencias_fechadas') IS DISTINCT FROM (to_jsonb(OLD) - 'evidencias_fechadas') THEN
        RAISE EXCEPTION 'Somente o fechamento do conjunto documental e permitido: %', TG_TABLE_NAME USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER proteger_avaliacao_trl_documental
    BEFORE INSERT OR UPDATE OR DELETE ON avaliacoes_trl
    FOR EACH ROW EXECUTE FUNCTION proteger_registro_documental();

CREATE TRIGGER proteger_parecer_documental
    BEFORE INSERT OR UPDATE OR DELETE ON pareceres_tecnicos
    FOR EACH ROW EXECUTE FUNCTION proteger_registro_documental();

CREATE FUNCTION exigir_documento_fechado() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE documento_fechado BOOLEAN;
BEGIN
    IF TG_TABLE_NAME = 'avaliacoes_trl' THEN
        SELECT evidencias_fechadas INTO documento_fechado
        FROM avaliacoes_trl WHERE id = NEW.id;
    ELSE
        SELECT evidencias_fechadas INTO documento_fechado
        FROM pareceres_tecnicos WHERE id = NEW.id;
    END IF;

    IF documento_fechado IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'O conjunto de evidencias deve ser fechado antes do commit: %', TG_TABLE_NAME
            USING ERRCODE = '23514';
    END IF;

    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER fechar_conjunto_evidencias_trl
    AFTER INSERT OR UPDATE ON avaliacoes_trl
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION exigir_documento_fechado();

CREATE CONSTRAINT TRIGGER fechar_conjunto_evidencias_parecer
    AFTER INSERT OR UPDATE ON pareceres_tecnicos
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION exigir_documento_fechado();

CREATE FUNCTION inserir_evidencia_em_documento_aberto() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE documento_fechado BOOLEAN;
BEGIN
    IF TG_TABLE_NAME = 'evidencias_trl' THEN
        SELECT evidencias_fechadas INTO documento_fechado
        FROM avaliacoes_trl WHERE id = NEW.avaliacao_id FOR UPDATE;
    ELSE
        SELECT evidencias_fechadas INTO documento_fechado
        FROM pareceres_tecnicos WHERE id = NEW.parecer_id FOR UPDATE;
    END IF;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Registro documental nao encontrado' USING ERRCODE = '23503';
    END IF;

    IF documento_fechado IS DISTINCT FROM FALSE THEN
        RAISE EXCEPTION 'O conjunto de evidencias ja foi fechado: %', TG_TABLE_NAME USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER anexar_evidencia_avaliacao_aberta
    BEFORE INSERT ON evidencias_trl
    FOR EACH ROW EXECUTE FUNCTION inserir_evidencia_em_documento_aberto();

CREATE TRIGGER anexar_evidencia_parecer_aberto
    BEFORE INSERT ON evidencias_parecer
    FOR EACH ROW EXECUTE FUNCTION inserir_evidencia_em_documento_aberto();

CREATE TRIGGER proteger_vinculo_evidencia_trl
    BEFORE UPDATE OR DELETE ON evidencias_trl
    FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();

CREATE TRIGGER proteger_vinculo_evidencia_parecer
    BEFORE UPDATE OR DELETE ON evidencias_parecer
    FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
