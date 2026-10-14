package br.com.bbts.sigespin;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;

class SolicitacoesAvancoMigrationTests {

    @Test
    @EnabledIfEnvironmentVariable(named = "SIGESPIN_TEST_DB_URL", matches = ".+")
    void deveValidarConstraintsEmPostgresDescartavel() throws Exception {
        String url = System.getenv("SIGESPIN_TEST_DB_URL");
        String usuario = System.getenv().getOrDefault("SIGESPIN_TEST_DB_USER", "postgres");
        String senha = System.getenv().getOrDefault("SIGESPIN_TEST_DB_PASSWORD", "");
        Flyway.configure().dataSource(url, usuario, senha).load().migrate();

        try (var recurso = getClass().getResourceAsStream("/db/validar_solicitacoes_avanco.sql")) {
            if (recurso == null) {
                throw new IllegalStateException("Script de validacao nao encontrado");
            }
            String script = new String(recurso.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^\\\\.*$", "");
            try (var conexao = DriverManager.getConnection(url, usuario, senha);
                 var comando = conexao.createStatement()) {
                comando.execute(script);
            }
        }
    }
}
