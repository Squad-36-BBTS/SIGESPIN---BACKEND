package br.com.bbts.sigespin;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SIGESPIN_TEST_DB_URL", matches=".+")
class EvolucaoPortfolioMigrationTests {
    String url = System.getenv("SIGESPIN_TEST_DB_URL");
    String user = System.getenv().getOrDefault("SIGESPIN_TEST_DB_USER", "postgres");
    String password = System.getenv().getOrDefault("SIGESPIN_TEST_DB_PASSWORD", "");
    Flyway flyway(String schema, String target) {
        return Flyway.configure().dataSource(url,user,password).schemas(schema).target(target).load();
    }
    long number(Connection c, String sql) throws SQLException {
        try(var s=c.createStatement(); var r=s.executeQuery(sql)) { r.next(); return r.getLong(1); }
    }
    void sql(Connection c, String sql) throws SQLException { try(var s=c.createStatement()) { s.execute(sql); } }
    void rejects(Connection c, String code, String sql) throws SQLException {
        Savepoint savepoint=c.getAutoCommit() ? null : c.setSavepoint();
        SQLException ex=assertThrows(SQLException.class, ()->sql(c,sql));
        if(savepoint!=null) { c.rollback(savepoint); c.releaseSavepoint(savepoint); }
        assertEquals(code,ex.getSQLState());
    }
    @Test void instalaBancoVazio() throws Exception {
        String schema="novo_"+UUID.randomUUID().toString().replace("-","");
        try(var c=DriverManager.getConnection(url,user,password)) {
            try {
                flyway(schema,"latest").migrate(); sql(c,"SET search_path TO "+schema);
                assertEquals(5,number(c,"SELECT count(*) FROM etapas_kanban"));
                assertEquals(2,number(c,"SELECT count(*) FROM etapas_kanban WHERE (codigo='IDEACAO' AND nome='Ideação') OR (codigo='CONCLUIDO' AND nome='Concluído')"));
                assertEquals(6,number(c,"SELECT count(*) FROM transicoes_kanban WHERE exige_governanca"));
                assertEquals(0,number(c,"SELECT count(*) FROM avaliacoes_trl"));
            } finally { sql(c,"DROP SCHEMA IF EXISTS "+schema+" CASCADE"); }
        }
    }
    @Test void preservaLegadoEValidaNovasRelacoes() throws Exception {
        String schema="evolucao_"+UUID.randomUUID().toString().replace("-","");
        try(var c=DriverManager.getConnection(url,user,password)) {
            try {
                flyway(schema,"2").migrate(); sql(c,"SET search_path TO "+schema);
                sql(c,"""
                    INSERT INTO usuarios(id,nome,email,senha_hash,perfil) VALUES
                    (1,'D','d@teste','hash','DEMANDANTE'),(2,'A','a@teste','hash','ANALISTA'),
                    (3,'C','c@teste','hash','COMITE'),(4,'E','e@teste','hash','EXECUTIVO');
                    INSERT INTO projetos(id,titulo,descricao_dor,area_demandante_id,responsavel_po_id,arena,status_funil,trl_atual,dispendio_estimado)
                    SELECT row_number() over(),'Projeto','Dor',1,2,'A_DEFINIR',s,6,123.45
                    FROM unnest(ARRAY['IDEACAO','POC','MVP','CONCLUIDO','DESCONTINUADO']) s;
                    INSERT INTO solicitacoes_avanco(id,projeto_id,solicitante_id,tipo,justificativa,trl_anterior,trl_pretendido)
                    VALUES(1,1,2,'TRL','Pedido legado',6,7);
                    INSERT INTO solicitacoes_avanco(id,projeto_id,solicitante_id,tipo,justificativa,status_anterior,status_pretendido,status,responsavel_encerramento_id,justificativa_encerramento,encerrado_em)
                    VALUES(2,1,2,'STATUS_FUNIL','Antigo','IDEACAO','POC','APROVADA',3,'Decisao antiga',now());
                    INSERT INTO evidencias(id,projeto_id,solicitacao_avanco_id,nome_arquivo,caminho_storage,tipo_arquivo)
                    VALUES(1,1,1,'antigo.pdf','arquivo-antigo','pdf'),(2,1,NULL,'novo.pdf','arquivo-novo','pdf'),(3,2,NULL,'outro.pdf','outro','pdf');
                    INSERT INTO historico_transicoes(projeto_id,usuario_id,tipo_mudanca,valor_anterior,valor_novo,solicitacao_avanco_id)
                    VALUES(1,3,'STATUS_FUNIL','IDEACAO','POC',2);
                    """);
                flyway(schema,"latest").migrate();
                assertEquals(2,number(c,"SELECT count(*) FROM usuarios WHERE perfil='ANALISTA'"));
                assertEquals(1,number(c,"SELECT count(*) FROM usuarios WHERE perfil='ESPECIALISTA'"));
                assertEquals(5,number(c,"SELECT count(*) FROM projetos p JOIN etapas_kanban e ON p.etapa_id=e.id WHERE p.id=e.ordem+1 AND p.origem IS NULL AND dispendio_estimado=123.45"));
                assertEquals(2,number(c,"SELECT count(*) FROM solicitacoes_avanco"));
                assertEquals(1,number(c,"SELECT count(*) FROM solicitacoes_avanco WHERE status='PENDENTE'"));
                assertEquals(1,number(c,"SELECT count(*) FROM historico_transicoes"));
                assertEquals(3,number(c,"SELECT count(*) FROM evidencias"));
                assertEquals(0,number(c,"SELECT count(*) FROM avaliacoes_trl"));
                rejects(c,"23514","UPDATE evidencias SET solicitacao_avanco_id=1 WHERE id=2");
                rejects(c,"23514","INSERT INTO historico_transicoes(projeto_id,usuario_id,tipo_mudanca,valor_anterior,valor_novo,solicitacao_avanco_id) VALUES(1,3,'TRL','6','7',1)");
                rejects(c,"23514","UPDATE solicitacoes_avanco SET status='CANCELADA' WHERE id=1");
                rejects(c,"23514","UPDATE projetos SET origem='INDUZIDA' WHERE id=1");
                rejects(c,"23514","UPDATE projetos SET acao_mobilizacao_id=1 WHERE id=1");
                sql(c,"INSERT INTO acoes_mobilizacao(id,nome) VALUES(1,'Hackathon'); UPDATE projetos SET origem='INDUZIDA',acao_mobilizacao_id=1 WHERE id=2");
                // A downward assessment and current value are written atomically by this test, not by a service.
                c.setAutoCommit(false);
                sql(c,"INSERT INTO avaliacoes_trl(id,projeto_id,autor_id,nivel_anterior,nivel,justificativa,avaliado_em,evidencias_fechadas) VALUES(1,1,2,6,3,'Resultado revisto',now(),false)");
                rejects(c,"23503","INSERT INTO evidencias_trl(projeto_id,avaliacao_id,evidencia_id) VALUES(1,1,3)");
                sql(c,"INSERT INTO evidencias_trl(projeto_id,avaliacao_id,evidencia_id) VALUES(1,1,2)");
                sql(c,"UPDATE avaliacoes_trl SET evidencias_fechadas=true WHERE id=1");
                sql(c,"UPDATE projetos SET trl_atual=3,versao=versao+1 WHERE id=1"); c.commit(); c.setAutoCommit(true);
                assertEquals(3,number(c,"SELECT trl_atual FROM projetos WHERE id=1"));
                rejects(c,"23514","INSERT INTO evidencias_trl(projeto_id,avaliacao_id,evidencia_id) VALUES(1,1,1)");
                rejects(c,"23514","UPDATE avaliacoes_trl SET nivel=4 WHERE id=1");
                rejects(c,"23514","UPDATE evidencias_trl SET evidencia_id=1 WHERE avaliacao_id=1");
                rejects(c,"23514","DELETE FROM evidencias_trl WHERE avaliacao_id=1");
                rejects(c,"23514","UPDATE evidencias SET caminho_storage='mudou' WHERE id=2");
                sql(c,"INSERT INTO consultas_especialista(id,projeto_id,solicitante_id,especialista_id,questao,criado_em) VALUES(1,1,2,3,'Viavel?',now())");
                rejects(c,"23503","INSERT INTO pareceres_tecnicos(projeto_id,autor_id,consulta_id,fundamentacao,conclusao,criado_em) VALUES(2,3,1,'Teste','Teste',now())");
                c.setAutoCommit(false);
                sql(c,"INSERT INTO pareceres_tecnicos(id,projeto_id,autor_id,consulta_id,fundamentacao,conclusao,criado_em,evidencias_fechadas) VALUES(10,1,3,1,'Experimento','Viavel',now(),false)");
                rejects(c,"23503","INSERT INTO evidencias_parecer(projeto_id,parecer_id,evidencia_id) VALUES(1,10,3)");
                sql(c,"INSERT INTO evidencias_parecer(projeto_id,parecer_id,evidencia_id) VALUES(1,10,2)");
                sql(c,"UPDATE pareceres_tecnicos SET evidencias_fechadas=true WHERE id=10");
                c.commit(); c.setAutoCommit(true);
                rejects(c,"23514","INSERT INTO evidencias_parecer(projeto_id,parecer_id,evidencia_id) VALUES(1,10,1)");
                rejects(c,"23514","DELETE FROM evidencias_parecer WHERE parecer_id=10");
                sql(c,"INSERT INTO entregas_projeto(id,projeto_id,autor_id,descricao,submetido_em) VALUES(1,1,2,'MVP',now())");
                rejects(c,"23503","INSERT INTO validacoes_entrega(projeto_id,entrega_id,demandante_id,resultado,observacoes,validado_em) VALUES(2,1,1,'VALIDADA','Ok',now())");
                sql(c,"INSERT INTO validacoes_entrega(projeto_id,entrega_id,demandante_id,resultado,observacoes,validado_em) VALUES(1,1,1,'NAO_VALIDADA','Ajustar MVP',now())");
                assertEquals(1,number(c,"SELECT etapa_id FROM projetos WHERE id=1"));
                sql(c,"INSERT INTO solicitacoes_transicao(id,projeto_id,solicitante_id,transicao_id,origem_id,destino_id,exige_governanca,status,justificativa,criado_em) VALUES(1,1,2,(SELECT id FROM transicoes_kanban WHERE origem_id=1 AND destino_id=2),1,2,true,'PENDENTE','Experimento',now())");
                rejects(c,"23505","INSERT INTO solicitacoes_transicao(id,projeto_id,solicitante_id,transicao_id,origem_id,destino_id,exige_governanca,status,justificativa,criado_em) VALUES(2,1,2,(SELECT id FROM transicoes_kanban WHERE origem_id=1 AND destino_id=2),1,2,true,'PENDENTE','Duplicado',now())");
                rejects(c,"23503","INSERT INTO evidencias_transicao(projeto_id,solicitacao_id,evidencia_id) VALUES(1,1,3)");
                sql(c,"INSERT INTO evidencias_transicao(projeto_id,solicitacao_id,evidencia_id) VALUES(1,1,2)");
                rejects(c,"23503","INSERT INTO movimentacoes_kanban(projeto_id,autor_id,origem_id,destino_id,solicitacao_id,justificativa,movimentado_em) VALUES(2,3,1,2,1,'Outro projeto',now())");
                assertEquals(1,number(c,"SELECT etapa_id FROM projetos WHERE id=1"));
                sql(c,"UPDATE transicoes_kanban SET exige_governanca=false WHERE origem_id=1 AND destino_id=2");
                assertEquals(1,number(c,"SELECT count(*) FROM solicitacoes_transicao WHERE exige_governanca"));
                rejects(c,"23514","UPDATE solicitacoes_transicao SET status='APROVADA' WHERE id=1");
                sql(c,"UPDATE solicitacoes_transicao SET status='REJEITADA',responsavel_encerramento_id=3,justificativa_encerramento='Reavaliar',encerrado_em=now() WHERE id=1");
                assertEquals(1,number(c,"SELECT etapa_id FROM projetos WHERE id=1"));
                rejects(c,"23514","INSERT INTO evidencias_transicao(projeto_id,solicitacao_id,evidencia_id) VALUES(1,1,1)");
                rejects(c,"23514","UPDATE solicitacoes_transicao SET status='PENDENTE' WHERE id=1");
                rejects(c,"23503","DELETE FROM etapas_kanban WHERE id=1");
                rejects(c,"23514","INSERT INTO transicoes_kanban(origem_id,destino_id,exige_governanca) VALUES(4,1,false)");
                // Two independent transactions race for the same pending-project slot.
                sql(c,"UPDATE transicoes_kanban SET exige_governanca=true WHERE origem_id=1 AND destino_id=2");
                var start=new java.util.concurrent.CountDownLatch(1);
                try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
                    var futures=new java.util.ArrayList<java.util.concurrent.Future<String>>();
                    for(int id : new int[]{20,21}) futures.add(executor.submit(()->{
                        start.await();
                        try(var other=DriverManager.getConnection(url,user,password)) {
                            sql(other,"SET search_path TO "+schema);
                            sql(other,"SET statement_timeout='10s'");
                            try {
                                sql(other,"INSERT INTO solicitacoes_transicao(id,projeto_id,solicitante_id,transicao_id,origem_id,destino_id,exige_governanca,status,justificativa,criado_em) VALUES("+id+",1,2,(SELECT id FROM transicoes_kanban WHERE origem_id=1 AND destino_id=2),1,2,true,'PENDENTE','Concorrente',now())");
                                return "OK";
                            } catch(SQLException ex) { return ex.getSQLState(); }
                        }
                    }));
                    start.countDown();
                    var results=new java.util.HashSet<String>();
                    for(var future:futures) results.add(future.get(15,java.util.concurrent.TimeUnit.SECONDS));
                    assertEquals(java.util.Set.of("OK","23505"),results);
                }
                sql(c,"UPDATE etapas_kanban SET nome='Descoberta',ativa=false WHERE id=1");
                assertEquals(1,number(c,"SELECT etapa_id FROM projetos WHERE id=1"));
            } finally { if(!c.getAutoCommit()) { c.rollback(); c.setAutoCommit(true); } sql(c,"DROP SCHEMA IF EXISTS "+schema+" CASCADE"); }
        }
    }
}
