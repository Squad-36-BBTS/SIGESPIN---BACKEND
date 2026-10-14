\set ON_ERROR_STOP on

BEGIN;

CREATE FUNCTION pg_temp.esperar_erro(comando TEXT, codigo_esperado TEXT)
RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE comando;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = codigo_esperado THEN
            RETURN;
        END IF;
        RAISE;
    END;
    RAISE EXCEPTION 'Era esperado erro % para: %', codigo_esperado, comando;
END;
$$;

INSERT INTO usuarios (id, nome, email, senha_hash, perfil) VALUES
    (-1, 'Demandante teste', 'demandante@teste.local', 'hash-teste', 'DEMANDANTE'),
    (-2, 'Analista teste', 'analista@teste.local', 'hash-teste', 'ANALISTA'),
    (-3, 'Comite teste', 'comite@teste.local', 'hash-teste', 'COMITE'),
    (-4, 'Executivo teste', 'executivo@teste.local', 'hash-teste', 'EXECUTIVO');

INSERT INTO projetos (id, titulo, descricao_dor, area_demandante_id, responsavel_po_id,
                      demandante_id, analista_responsavel_id, arena, trl_atual) VALUES
    (-1, 'Projeto teste', 'Dor teste', 1, -2, -1, -2, 'A_DEFINIR', 2),
    (-2, 'Outro projeto', 'Outra dor', 1, -2, NULL, NULL, 'A_DEFINIR', 1);

INSERT INTO solicitacoes_avanco
    (id, projeto_id, solicitante_id, tipo, justificativa, trl_anterior, trl_pretendido)
VALUES (-1, -1, -2, 'TRL', 'Comprovacao tecnica', 2, 3);

INSERT INTO solicitacoes_avanco
    (id, projeto_id, solicitante_id, tipo, justificativa, status_anterior, status_pretendido)
VALUES (-2, -1, -2, 'STATUS_FUNIL', 'Validacao da ideia', 'IDEACAO', 'POC');

SELECT pg_temp.esperar_erro($sql$
    INSERT INTO solicitacoes_avanco
        (projeto_id, solicitante_id, tipo, justificativa, trl_anterior, trl_pretendido)
    VALUES (-1, -2, 'TRL', 'Pedido duplicado', 2, 4)
$sql$, '23505');

SELECT pg_temp.esperar_erro($sql$
    INSERT INTO solicitacoes_avanco
        (projeto_id, solicitante_id, tipo, justificativa, trl_anterior, trl_pretendido)
    VALUES (-2, -2, 'TRL', 'Regressao invalida', 3, 2)
$sql$, '23514');

SELECT pg_temp.esperar_erro($sql$
    INSERT INTO solicitacoes_avanco
        (projeto_id, solicitante_id, tipo, justificativa, trl_anterior, trl_pretendido)
    VALUES (-2, -2, 'TRL', 'Fora da escala', 1, 10)
$sql$, '23514');

SELECT pg_temp.esperar_erro($sql$
    INSERT INTO solicitacoes_avanco
        (projeto_id, solicitante_id, tipo, justificativa, status_anterior, status_pretendido)
    VALUES (-2, -2, 'STATUS_FUNIL', 'Salto invalido', 'IDEACAO', 'MVP')
$sql$, '23514');

SELECT pg_temp.esperar_erro($sql$
    INSERT INTO solicitacoes_avanco
        (projeto_id, solicitante_id, tipo, justificativa, trl_anterior, trl_pretendido)
    VALUES (-2, -2, 'TRL', 'Sem valores', NULL, NULL)
$sql$, '23514');

SELECT pg_temp.esperar_erro($sql$
    UPDATE solicitacoes_avanco SET status = 'APROVADA' WHERE id = -1
$sql$, '23514');

INSERT INTO evidencias
    (id, projeto_id, solicitacao_avanco_id, nome_arquivo, caminho_storage, tipo_arquivo)
VALUES (-1, -1, -1, 'teste.pdf', 's3://teste/teste.pdf', 'application/pdf');

SELECT pg_temp.esperar_erro($sql$
    INSERT INTO evidencias
        (projeto_id, solicitacao_avanco_id, nome_arquivo, caminho_storage, tipo_arquivo)
    VALUES (-2, -1, 'teste.pdf', 's3://teste/teste.pdf', 'application/pdf')
$sql$, '23503');

UPDATE solicitacoes_avanco
SET status = 'APROVADA', responsavel_encerramento_id = -3,
    justificativa_encerramento = 'Evidencias suficientes', encerrado_em = NOW()
WHERE id = -1;

INSERT INTO historico_transicoes
    (projeto_id, usuario_id, solicitacao_avanco_id, tipo_mudanca, valor_anterior, valor_novo)
VALUES (-1, -3, -1, 'TRL', '2', '3');

SELECT pg_temp.esperar_erro($sql$
    INSERT INTO historico_transicoes
        (projeto_id, usuario_id, solicitacao_avanco_id, tipo_mudanca, valor_anterior, valor_novo)
    VALUES (-1, -3, -1, 'TRL', '2', '3')
$sql$, '23505');

SELECT pg_temp.esperar_erro($sql$
    INSERT INTO historico_transicoes
        (projeto_id, usuario_id, solicitacao_avanco_id, tipo_mudanca, valor_anterior, valor_novo)
    VALUES (-2, -3, -2, 'STATUS_FUNIL', 'IDEACAO', 'POC')
$sql$, '23503');

INSERT INTO solicitacoes_avanco
    (projeto_id, solicitante_id, tipo, justificativa, trl_anterior, trl_pretendido)
VALUES (-1, -2, 'TRL', 'Nova tentativa apos encerramento', 3, 4);

UPDATE solicitacoes_avanco
SET status = 'REJEITADA', responsavel_encerramento_id = -3,
    justificativa_encerramento = 'Complementar documentos', encerrado_em = NOW()
WHERE id = -2;

DO $$
BEGIN
    IF (SELECT status_funil FROM projetos WHERE id = -1) <> 'IDEACAO' THEN
        RAISE EXCEPTION 'Rejeitar pedido nao deve alterar etapa do projeto';
    END IF;
END;
$$;

ROLLBACK;

\echo 'Validacao das constraints V2 concluida com sucesso.'
