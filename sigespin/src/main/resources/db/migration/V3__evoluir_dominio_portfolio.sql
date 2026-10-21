-- V1/V2 permanecem intactas. Nenhuma avaliacao retroativa e criada.
ALTER TABLE usuarios DROP CONSTRAINT chk_usuarios_perfil;
UPDATE usuarios SET perfil = CASE perfil WHEN 'COMITE' THEN 'ESPECIALISTA' WHEN 'EXECUTIVO' THEN 'ANALISTA' ELSE perfil END;
ALTER TABLE usuarios ADD CONSTRAINT chk_usuarios_perfil CHECK (perfil IN ('DEMANDANTE','ANALISTA','ESPECIALISTA'));
CREATE TABLE acoes_mobilizacao (id BIGSERIAL PRIMARY KEY, nome TEXT NOT NULL CHECK (btrim(nome) <> ''), descricao TEXT);
CREATE TABLE etapas_kanban (
 id BIGSERIAL PRIMARY KEY, codigo VARCHAR(255) NOT NULL UNIQUE CHECK (btrim(codigo) <> ''),
 nome VARCHAR(255) NOT NULL CHECK (btrim(nome) <> ''), ordem INTEGER NOT NULL CHECK (ordem >= 0),
 ativa BOOLEAN NOT NULL DEFAULT TRUE, terminal VARCHAR(255) CHECK (terminal IN ('CONCLUSAO','DESCONTINUACAO')), versao BIGINT NOT NULL DEFAULT 0
);
INSERT INTO etapas_kanban(codigo,nome,ordem,terminal) VALUES
 ('IDEACAO','Ideação',0,NULL),('POC','POC',1,NULL),('MVP','MVP',2,NULL),
 ('CONCLUIDO','Concluído',3,'CONCLUSAO'),('DESCONTINUADO','Descontinuado',4,'DESCONTINUACAO');
ALTER TABLE projetos ADD COLUMN etapa_id BIGINT REFERENCES etapas_kanban(id),
 ADD COLUMN origem VARCHAR(20), ADD COLUMN acao_mobilizacao_id BIGINT REFERENCES acoes_mobilizacao(id);
UPDATE projetos p SET etapa_id = e.id FROM etapas_kanban e WHERE e.codigo = p.status_funil;
ALTER TABLE projetos ALTER COLUMN etapa_id SET NOT NULL;
-- O status antigo do projeto foi convertido; os valores de pedidos/historicos antigos permanecem.
ALTER TABLE projetos DROP COLUMN status_funil;
ALTER TABLE projetos ADD CONSTRAINT chk_projeto_origem CHECK (
 (origem IS NULL AND acao_mobilizacao_id IS NULL) OR
 (origem IS NOT NULL AND ((origem = 'ESPONTANEA' AND acao_mobilizacao_id IS NULL) OR
 (origem = 'INDUZIDA' AND acao_mobilizacao_id IS NOT NULL))));
CREATE INDEX idx_projeto_etapa ON projetos(etapa_id);
CREATE INDEX idx_projeto_acao ON projetos(acao_mobilizacao_id);
CREATE TABLE transicoes_kanban (
 id BIGSERIAL PRIMARY KEY, origem_id BIGINT NOT NULL REFERENCES etapas_kanban(id), destino_id BIGINT NOT NULL REFERENCES etapas_kanban(id),
 exige_governanca BOOLEAN NOT NULL, ativa BOOLEAN NOT NULL DEFAULT TRUE, versao BIGINT NOT NULL DEFAULT 0,
 UNIQUE(origem_id,destino_id), CHECK (origem_id <> destino_id)
);
INSERT INTO transicoes_kanban(origem_id,destino_id,exige_governanca)
 SELECT o.id,d.id,TRUE FROM etapas_kanban o CROSS JOIN etapas_kanban d WHERE
 (o.codigo,d.codigo) IN (('IDEACAO','POC'),('POC','MVP'),('MVP','CONCLUIDO'),
 ('IDEACAO','DESCONTINUADO'),('POC','DESCONTINUADO'),('MVP','DESCONTINUADO')) ORDER BY o.ordem,d.ordem;
CREATE TABLE qualificacoes_demanda (
 id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), autor_id BIGINT NOT NULL REFERENCES usuarios(id),
 aderencia_tese TEXT NOT NULL, relevancia TEXT NOT NULL, grau_inovacao TEXT NOT NULL, viabilidade TEXT NOT NULL, maturidade_tecnologica TEXT NOT NULL,
 fundamentacao TEXT NOT NULL CHECK(btrim(fundamentacao) <> ''), conclusao TEXT NOT NULL CHECK(btrim(conclusao) <> ''), criado_em TIMESTAMP NOT NULL
);
CREATE TABLE consultas_especialista (
 id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), solicitante_id BIGINT NOT NULL REFERENCES usuarios(id),
 especialista_id BIGINT NOT NULL REFERENCES usuarios(id), questao TEXT NOT NULL CHECK(btrim(questao) <> ''), criado_em TIMESTAMP NOT NULL, UNIQUE(id,projeto_id)
);
CREATE TABLE pareceres_tecnicos (
 id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), autor_id BIGINT NOT NULL REFERENCES usuarios(id), consulta_id BIGINT,
 fundamentacao TEXT NOT NULL CHECK(btrim(fundamentacao) <> ''), conclusao TEXT NOT NULL CHECK(btrim(conclusao) <> ''), criado_em TIMESTAMP NOT NULL,
 UNIQUE(id,projeto_id), FOREIGN KEY(consulta_id,projeto_id) REFERENCES consultas_especialista(id,projeto_id)
);
CREATE TABLE avaliacoes_trl (
 id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), autor_id BIGINT NOT NULL REFERENCES usuarios(id),
 nivel_anterior INTEGER NOT NULL CHECK(nivel_anterior BETWEEN 1 AND 9), nivel INTEGER NOT NULL CHECK(nivel BETWEEN 1 AND 9),
 justificativa TEXT NOT NULL CHECK(btrim(justificativa) <> ''), avaliado_em TIMESTAMP NOT NULL, UNIQUE(id,projeto_id)
);
CREATE TABLE solicitacoes_transicao (
 id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), solicitante_id BIGINT NOT NULL REFERENCES usuarios(id),
 transicao_id BIGINT NOT NULL REFERENCES transicoes_kanban(id), origem_id BIGINT NOT NULL REFERENCES etapas_kanban(id), destino_id BIGINT NOT NULL REFERENCES etapas_kanban(id),
 exige_governanca BOOLEAN NOT NULL CHECK(exige_governanca), status VARCHAR(255) NOT NULL CHECK(status IN ('PENDENTE','APROVADA','REJEITADA','CANCELADA')),
 justificativa TEXT NOT NULL CHECK(btrim(justificativa) <> ''), criado_em TIMESTAMP NOT NULL,
 responsavel_encerramento_id BIGINT REFERENCES usuarios(id), justificativa_encerramento TEXT, encerrado_em TIMESTAMP,
 versao BIGINT NOT NULL DEFAULT 0, UNIQUE(id,projeto_id), CHECK(origem_id <> destino_id),
 CHECK ((status = 'PENDENTE' AND responsavel_encerramento_id IS NULL AND justificativa_encerramento IS NULL AND encerrado_em IS NULL) OR
 (status <> 'PENDENTE' AND responsavel_encerramento_id IS NOT NULL AND justificativa_encerramento IS NOT NULL AND btrim(justificativa_encerramento) <> '' AND encerrado_em IS NOT NULL AND encerrado_em >= criado_em))
);
CREATE UNIQUE INDEX uq_transicao_pendente ON solicitacoes_transicao(projeto_id) WHERE status = 'PENDENTE';
CREATE INDEX idx_solicitacao_transicao_fila ON solicitacoes_transicao(status,criado_em);
CREATE TABLE movimentacoes_kanban (
 id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), autor_id BIGINT NOT NULL REFERENCES usuarios(id),
 origem_id BIGINT NOT NULL REFERENCES etapas_kanban(id), destino_id BIGINT NOT NULL REFERENCES etapas_kanban(id), solicitacao_id BIGINT UNIQUE,
 justificativa TEXT NOT NULL CHECK(btrim(justificativa) <> ''), movimentado_em TIMESTAMP NOT NULL,
 CHECK(origem_id <> destino_id), FOREIGN KEY(solicitacao_id,projeto_id) REFERENCES solicitacoes_transicao(id,projeto_id)
);
CREATE TABLE entregas_projeto (
 id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), autor_id BIGINT NOT NULL REFERENCES usuarios(id),
 descricao TEXT NOT NULL CHECK(btrim(descricao) <> ''), submetido_em TIMESTAMP NOT NULL, UNIQUE(id,projeto_id)
);
CREATE TABLE validacoes_entrega (
 id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), entrega_id BIGINT NOT NULL, demandante_id BIGINT NOT NULL REFERENCES usuarios(id),
 resultado VARCHAR(255) NOT NULL CHECK(resultado IN ('VALIDADA','VALIDADA_COM_RESSALVAS','NAO_VALIDADA')), observacoes TEXT NOT NULL, validado_em TIMESTAMP NOT NULL,
 FOREIGN KEY(entrega_id,projeto_id) REFERENCES entregas_projeto(id,projeto_id)
);
ALTER TABLE evidencias ADD CONSTRAINT uq_evidencia_projeto UNIQUE(id,projeto_id);
CREATE TABLE evidencias_trl (id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), avaliacao_id BIGINT NOT NULL, evidencia_id BIGINT NOT NULL,
 UNIQUE(avaliacao_id,evidencia_id), FOREIGN KEY(avaliacao_id,projeto_id) REFERENCES avaliacoes_trl(id,projeto_id), FOREIGN KEY(evidencia_id,projeto_id) REFERENCES evidencias(id,projeto_id));
CREATE TABLE evidencias_parecer (id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), parecer_id BIGINT NOT NULL, evidencia_id BIGINT NOT NULL,
 UNIQUE(parecer_id,evidencia_id), FOREIGN KEY(parecer_id,projeto_id) REFERENCES pareceres_tecnicos(id,projeto_id), FOREIGN KEY(evidencia_id,projeto_id) REFERENCES evidencias(id,projeto_id));
CREATE TABLE evidencias_transicao (id BIGSERIAL PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projetos(id), solicitacao_id BIGINT NOT NULL, evidencia_id BIGINT NOT NULL,
 UNIQUE(solicitacao_id,evidencia_id), FOREIGN KEY(solicitacao_id,projeto_id) REFERENCES solicitacoes_transicao(id,projeto_id), FOREIGN KEY(evidencia_id,projeto_id) REFERENCES evidencias(id,projeto_id));
CREATE INDEX idx_qualificacoes_demanda_projeto ON qualificacoes_demanda(projeto_id);
CREATE INDEX idx_consultas_especialista_projeto ON consultas_especialista(projeto_id);
CREATE INDEX idx_pareceres_tecnicos_projeto ON pareceres_tecnicos(projeto_id);
CREATE INDEX idx_avaliacoes_trl_projeto ON avaliacoes_trl(projeto_id);
CREATE INDEX idx_solicitacoes_transicao_projeto ON solicitacoes_transicao(projeto_id);
CREATE INDEX idx_movimentacoes_kanban_projeto ON movimentacoes_kanban(projeto_id);
CREATE INDEX idx_entregas_projeto_projeto ON entregas_projeto(projeto_id);
CREATE INDEX idx_validacoes_entrega_projeto ON validacoes_entrega(projeto_id);
CREATE INDEX idx_evidencias_trl_projeto ON evidencias_trl(projeto_id);
CREATE INDEX idx_evidencias_parecer_projeto ON evidencias_parecer(projeto_id);
CREATE INDEX idx_evidencias_transicao_projeto ON evidencias_transicao(projeto_id);

-- Registros legados sao congelados, nao cancelados ou aprovados artificialmente.
COMMENT ON TABLE solicitacoes_avanco IS 'LEGADO V2: inclusive pendentes; fora do fluxo ativo, somente consulta';
CREATE FUNCTION impedir_mutacao_historica() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Registro historico imutavel: %', TG_TABLE_NAME USING ERRCODE = '23514'; END $$;
CREATE TRIGGER proteger_solicitacoes_avanco BEFORE INSERT OR UPDATE OR DELETE ON solicitacoes_avanco FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_qualificacoes_demanda BEFORE UPDATE OR DELETE ON qualificacoes_demanda FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_consultas_especialista BEFORE UPDATE OR DELETE ON consultas_especialista FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_pareceres_tecnicos BEFORE UPDATE OR DELETE ON pareceres_tecnicos FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_avaliacoes_trl BEFORE UPDATE OR DELETE ON avaliacoes_trl FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_movimentacoes_kanban BEFORE UPDATE OR DELETE ON movimentacoes_kanban FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_entregas_projeto BEFORE UPDATE OR DELETE ON entregas_projeto FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_validacoes_entrega BEFORE UPDATE OR DELETE ON validacoes_entrega FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_evidencias_trl BEFORE UPDATE OR DELETE ON evidencias_trl FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_evidencias_parecer BEFORE UPDATE OR DELETE ON evidencias_parecer FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_historico_transicoes BEFORE UPDATE OR DELETE ON historico_transicoes FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();
CREATE TRIGGER proteger_deliberacao_historico BEFORE UPDATE OR DELETE ON deliberacao_historico FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historica();

CREATE FUNCTION proteger_solicitacao_transicao() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE t transicoes_kanban;
BEGIN
 IF TG_OP = 'DELETE' THEN RAISE EXCEPTION 'Solicitacao historica' USING ERRCODE='23514'; END IF;
 IF TG_OP = 'INSERT' THEN
  SELECT * INTO STRICT t FROM transicoes_kanban WHERE id = NEW.transicao_id FOR SHARE;
  IF NOT t.ativa OR NOT t.exige_governanca OR (NEW.origem_id,NEW.destino_id,NEW.exige_governanca) IS DISTINCT FROM (t.origem_id,t.destino_id,t.exige_governanca) THEN
   RAISE EXCEPTION 'Snapshot invalido' USING ERRCODE='23514';
  END IF;
 ELSE
  IF OLD.status <> 'PENDENTE' OR (NEW.projeto_id,NEW.solicitante_id,NEW.transicao_id,NEW.origem_id,NEW.destino_id,NEW.exige_governanca,NEW.justificativa,NEW.criado_em)
   IS DISTINCT FROM (OLD.projeto_id,OLD.solicitante_id,OLD.transicao_id,OLD.origem_id,OLD.destino_id,OLD.exige_governanca,OLD.justificativa,OLD.criado_em) THEN
   RAISE EXCEPTION 'Snapshot imutavel' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER proteger_pedido BEFORE INSERT OR UPDATE OR DELETE ON solicitacoes_transicao FOR EACH ROW EXECUTE FUNCTION proteger_solicitacao_transicao();
CREATE FUNCTION proteger_evidencia_transicao() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE situacao TEXT;
BEGIN
 IF TG_OP <> 'INSERT' THEN RAISE EXCEPTION 'Vinculo historico imutavel' USING ERRCODE='23514'; END IF;
 SELECT status INTO situacao FROM solicitacoes_transicao WHERE id=NEW.solicitacao_id FOR UPDATE;
 PERFORM 1 FROM evidencias WHERE id=NEW.evidencia_id FOR SHARE;
 IF situacao <> 'PENDENTE' THEN RAISE EXCEPTION 'Pedido encerrado' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER proteger_anexo_pedido BEFORE INSERT OR UPDATE OR DELETE ON evidencias_transicao FOR EACH ROW EXECUTE FUNCTION proteger_evidencia_transicao();
-- Metadados de arquivos referenciados nao podem mudar sob uma decisao historica.
CREATE FUNCTION proteger_arquivo_referenciado() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM evidencias_trl WHERE evidencia_id=OLD.id) OR EXISTS(SELECT 1 FROM evidencias_parecer WHERE evidencia_id=OLD.id)
 OR EXISTS(SELECT 1 FROM evidencias_transicao WHERE evidencia_id=OLD.id) OR OLD.solicitacao_avanco_id IS NOT NULL THEN
 RAISE EXCEPTION 'Evidencia historica' USING ERRCODE='23514'; END IF;
 RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER proteger_arquivo BEFORE UPDATE OR DELETE ON evidencias FOR EACH ROW EXECUTE FUNCTION proteger_arquivo_referenciado();
CREATE FUNCTION proteger_etapa() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.codigo IS DISTINCT FROM OLD.codigo OR NEW.terminal IS DISTINCT FROM OLD.terminal THEN
 RAISE EXCEPTION 'Identidade e semantica terminal estaveis' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER proteger_identidade_etapa BEFORE UPDATE ON etapas_kanban FOR EACH ROW EXECUTE FUNCTION proteger_etapa();
CREATE FUNCTION validar_origem_transicao() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM etapas_kanban WHERE id=NEW.origem_id AND terminal IS NOT NULL) THEN
 RAISE EXCEPTION 'Etapa terminal sem saida' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER validar_terminal BEFORE INSERT OR UPDATE ON transicoes_kanban FOR EACH ROW EXECUTE FUNCTION validar_origem_transicao();

-- A avaliacao/parecer e seu conjunto documental nascem na mesma transacao.
CREATE FUNCTION validar_anexo_avaliacao() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE criacao TEXT;
BEGIN
 IF TG_TABLE_NAME = 'evidencias_trl' THEN
 SELECT xmin::text INTO criacao FROM avaliacoes_trl WHERE id=NEW.avaliacao_id;
 ELSE
 SELECT xmin::text INTO criacao FROM pareceres_tecnicos WHERE id=NEW.parecer_id;
 END IF;
 IF criacao IS DISTINCT FROM pg_current_xact_id()::text THEN
 RAISE EXCEPTION 'Anexos devem integrar a transacao da avaliacao/parecer' USING ERRCODE='23514'; END IF;
 PERFORM 1 FROM evidencias WHERE id=NEW.evidencia_id FOR SHARE;
 RETURN NEW;
END $$;
CREATE TRIGGER validar_anexo_trl BEFORE INSERT ON evidencias_trl FOR EACH ROW EXECUTE FUNCTION validar_anexo_avaliacao();
CREATE TRIGGER validar_anexo_parecer BEFORE INSERT ON evidencias_parecer FOR EACH ROW EXECUTE FUNCTION validar_anexo_avaliacao();

CREATE FUNCTION impedir_novo_vinculo_legado() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.solicitacao_avanco_id IS NOT NULL THEN RAISE EXCEPTION 'Pedido legado somente leitura' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER impedir_anexo_legado BEFORE INSERT OR UPDATE ON evidencias FOR EACH ROW EXECUTE FUNCTION impedir_novo_vinculo_legado();
CREATE INDEX idx_transicao_destino ON transicoes_kanban(destino_id);
CREATE INDEX idx_parecer_consulta ON pareceres_tecnicos(consulta_id);
CREATE INDEX idx_validacao_entrega ON validacoes_entrega(entrega_id);

CREATE TRIGGER impedir_auditoria_legada BEFORE INSERT ON historico_transicoes FOR EACH ROW EXECUTE FUNCTION impedir_novo_vinculo_legado();
