package com.invest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Configuração do DataSource que verifica e cria o banco de dados
 * automaticamente antes de inicializar a conexão
 */
@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    /**
     * Cria o DataSource e verifica/cria o banco antes de retornar
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource(DataSourceProperties properties) {
        // Verifica e cria o banco antes de criar o DataSource
        verificarECriarBanco();
        
        // Retorna o DataSource padrão do Spring Boot
        return properties.initializeDataSourceBuilder().build();
    }

    /**
     * Verifica e cria o banco de dados se necessário
     */
    private void verificarECriarBanco() {
        // Só executa se estiver usando MariaDB/MySQL (não H2)
        if (!isMariaDbOrMySql()) {
            logger.debug("Banco de dados não é MariaDB/MySQL. Pulando criação automática do banco.");
            return;
        }

        try {
            String databaseName = extrairNomeBanco(datasourceUrl);
            
            if (databaseName == null || databaseName.isEmpty()) {
                logger.debug("Não foi possível extrair o nome do banco da URL.");
                return;
            }

            String serverUrl = criarUrlServidor(datasourceUrl);
            
            logger.info("🔍 Verificando se o banco de dados '{}' existe...", databaseName);

            try (Connection connection = DriverManager.getConnection(serverUrl, datasourceUsername, datasourcePassword)) {
                if (bancoExiste(connection, databaseName)) {
                    logger.info("✅ Banco de dados '{}' já existe.", databaseName);
                    return;
                }

                logger.info("📦 Criando banco de dados '{}'...", databaseName);
                criarBanco(connection, databaseName);
                logger.info("✅ Banco de dados '{}' criado com sucesso!", databaseName);
            }
        } catch (Exception e) {
            logger.warn("⚠️ Não foi possível verificar/criar o banco de dados: " + e.getMessage());
            logger.debug("Detalhes:", e);
            // Não lança exceção para não impedir a inicialização
        }
    }

    private boolean isMariaDbOrMySql() {
        if (datasourceUrl == null || datasourceUrl.isEmpty()) {
            return false;
        }
        String urlLower = datasourceUrl.toLowerCase();
        return urlLower.contains("mariadb") || urlLower.contains("mysql");
    }

    private String extrairNomeBanco(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            String urlSemQuery = url.split("\\?")[0];
            int lastSlash = urlSemQuery.lastIndexOf('/');
            if (lastSlash == -1) return null;
            String databaseName = urlSemQuery.substring(lastSlash + 1).trim();
            return databaseName.isEmpty() ? null : databaseName;
        } catch (Exception e) {
            return null;
        }
    }

    private String criarUrlServidor(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        try {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash == -1) return url;
            String baseUrl = url.substring(0, lastSlash + 1);
            if (url.contains("mariadb") || url.contains("mysql")) {
                return baseUrl + "mysql";
            }
            return baseUrl;
        } catch (Exception e) {
            return url;
        }
    }

    private boolean bancoExiste(Connection connection, String databaseName) throws Exception {
        String sql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
        try (java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, databaseName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void criarBanco(Connection connection, String databaseName) throws Exception {
        String sql = "CREATE DATABASE IF NOT EXISTS `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}

