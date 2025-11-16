package com.invest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Componente que verifica e cria o banco de dados automaticamente
 * se ele não existir ao iniciar a aplicação.
 * 
 * Funciona apenas para MariaDB/MySQL (não para H2).
 * 
 * Este componente executa ANTES do DataSource ser configurado,
 * usando ApplicationEnvironmentPreparedEvent.
 */
@Component
@ConditionalOnProperty(
    name = "spring.datasource.driver-class-name",
    havingValue = "org.mariadb.jdbc.Driver",
    matchIfMissing = false
)
public class DatabaseInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        
        String datasourceUrl = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");
        
        if (datasourceUrl == null || username == null) {
            logger.warn("Configurações do banco de dados não encontradas. Pulando verificação.");
            return;
        }
        
        // Verifica e cria o banco de dados se necessário
        verificarECriarBanco(datasourceUrl, username, password);
    }

    /**
     * Verifica se o banco de dados existe e cria se não existir
     */
    private void verificarECriarBanco(String datasourceUrl, String username, String password) {
        try {
            // Extrai informações da URL do banco
            String[] urlParts = extrairInformacoesUrl(datasourceUrl);
            if (urlParts == null) {
                logger.warn("Não foi possível extrair informações da URL do banco de dados. Pulando verificação.");
                return;
            }

            String host = urlParts[0];
            String port = urlParts[1];
            String databaseName = urlParts[2];

            logger.info("Verificando se o banco de dados '{}' existe...", databaseName);

            // Conecta ao servidor MySQL/MariaDB sem especificar o banco
            String serverUrl = String.format("jdbc:mariadb://%s:%s/", host, port);
            
            try (Connection connection = DriverManager.getConnection(serverUrl, username, password)) {
                // Verifica se o banco existe
                if (!bancoExiste(connection, databaseName)) {
                    logger.info("Banco de dados '{}' não encontrado. Criando...", databaseName);
                    criarBanco(connection, databaseName);
                    logger.info("✅ Banco de dados '{}' criado com sucesso!", databaseName);
                } else {
                    logger.info("✅ Banco de dados '{}' já existe. Continuando...", databaseName);
                }
            }

        } catch (SQLException e) {
            logger.error("❌ Erro ao verificar/criar banco de dados: {}", e.getMessage());
            // Não lança exceção para não impedir a inicialização da aplicação
            // Se o banco não puder ser criado, o Spring tentará conectar e mostrará o erro apropriado
        } catch (Exception e) {
            logger.error("❌ Erro inesperado ao verificar banco de dados: {}", e.getMessage());
        }
    }

    /**
     * Extrai host, porta e nome do banco da URL do datasource
     * Formato esperado: jdbc:mariadb://host:port/database
     */
    private String[] extrairInformacoesUrl(String url) {
        try {
            // Remove jdbc:mariadb://
            String urlSemPrefixo = url.replace("jdbc:mariadb://", "");
            
            // Separa host:port/database
            String[] partes = urlSemPrefixo.split("/");
            if (partes.length < 2) {
                return null;
            }

            String hostPort = partes[0];
            String database = partes[1].split("\\?")[0]; // Remove query parameters se houver

            // Separa host e porta
            String[] hostPortParts = hostPort.split(":");
            String host = hostPortParts[0];
            String port = hostPortParts.length > 1 ? hostPortParts[1] : "3306";

            return new String[]{host, port, database};
        } catch (Exception e) {
            logger.error("Erro ao extrair informações da URL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifica se o banco de dados existe
     */
    private boolean bancoExiste(Connection connection, String databaseName) throws SQLException {
        String sql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
        
        try (java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, databaseName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Cria o banco de dados
     */
    private void criarBanco(Connection connection, String databaseName) throws SQLException {
        // Escapa o nome do banco para evitar SQL injection
        String escapedName = "`" + databaseName.replace("`", "``") + "`";
        String sql = "CREATE DATABASE IF NOT EXISTS " + escapedName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}
