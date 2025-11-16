package com.invest.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Serviço para executar scripts Python, especialmente o atualiza_cotacoes.py
 */
@Service
public class PythonScriptExecutor {

    private static final String SCRIPT_NAME = "atualiza_cotacoes.py";
    private static final long CACHE_DURATION_MS = 60000; // 1 minuto - evita executar muito frequentemente
    
    private long lastExecutionTime = 0;
    private boolean lastExecutionSuccess = false;

    /**
     * Executa o script atualiza_cotacoes.py para atualizar o JSON de cotações
     * @return true se executado com sucesso, false caso contrário
     */
    public boolean executarAtualizacaoCotacoes() {
        return executarAtualizacaoCotacoes(false);
    }

    /**
     * Executa o script atualiza_cotacoes.py para atualizar o JSON de cotações
     * @param silencioso Se true, não mostra mensagens de progresso
     * @return true se executado com sucesso, false caso contrário
     */
    public boolean executarAtualizacaoCotacoes(boolean silencioso) {
        // Evita executar muito frequentemente (cache de 1 minuto)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastExecutionTime < CACHE_DURATION_MS && lastExecutionSuccess) {
            return true; // Retorna sucesso se executou recentemente com sucesso
        }

        try {
            // Tenta encontrar o script na raiz do projeto
            File scriptFile = encontrarScript();
            
            if (scriptFile == null || !scriptFile.exists()) {
                if (!silencioso) {
                    System.err.println("⚠️ Script " + SCRIPT_NAME + " não encontrado. Pulando atualização automática.");
                }
                return false;
            }

            if (!silencioso) {
                System.out.println("🔄 Executando script Python para atualizar cotações...");
            }
            
            // Detecta o comando Python (python3 ou python)
            String pythonCommand = detectarComandoPython();
            
            if (pythonCommand == null) {
                if (!silencioso) {
                    System.err.println("⚠️ Python não encontrado no sistema. Pulando atualização automática.");
                }
                return false;
            }

            // Executa o script
            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonCommand,
                scriptFile.getAbsolutePath()
            );
            
            processBuilder.directory(scriptFile.getParentFile());
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Lê a saída do script
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Mostra apenas mensagens importantes se não estiver em modo silencioso
                    if (!silencioso && (line.contains("✅") || line.contains("🔄") || line.contains("❌"))) {
                        System.out.println(line);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            
            lastExecutionTime = currentTime;
            
            if (exitCode == 0) {
                lastExecutionSuccess = true;
                if (!silencioso) {
                    System.out.println("✅ Cotações atualizadas com sucesso!");
                }
                return true;
            } else {
                lastExecutionSuccess = false;
                if (!silencioso) {
                    System.err.println("❌ Erro ao executar script Python (código: " + exitCode + ")");
                    if (output.length() > 0) {
                        System.err.println("Saída do script:\n" + output.toString());
                    }
                }
                return false;
            }
            
        } catch (Exception e) {
            lastExecutionSuccess = false;
            if (!silencioso) {
                System.err.println("❌ Erro ao executar script Python: " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Encontra o arquivo do script Python
     */
    private File encontrarScript() {
        // Tenta vários locais possíveis
        String[] possiveisLocais = {
            SCRIPT_NAME, // Raiz do projeto
            "carteira/" + SCRIPT_NAME,
            "../" + SCRIPT_NAME,
            System.getProperty("user.dir") + File.separator + SCRIPT_NAME
        };
        
        for (String local : possiveisLocais) {
            File file = new File(local);
            if (file.exists() && file.isFile()) {
                return file;
            }
        }
        
        // Tenta encontrar usando o diretório de trabalho atual
        try {
            String currentDir = System.getProperty("user.dir");
            File currentDirFile = new File(currentDir);
            
            // Procura recursivamente até 3 níveis acima
            File searchDir = currentDirFile;
            for (int i = 0; i < 3; i++) {
                File script = new File(searchDir, SCRIPT_NAME);
                if (script.exists()) {
                    return script;
                }
                searchDir = searchDir.getParentFile();
                if (searchDir == null) break;
            }
        } catch (Exception e) {
            // Ignora erros na busca
        }
        
        return null;
    }

    /**
     * Detecta qual comando Python está disponível (python3 ou python)
     */
    private String detectarComandoPython() {
        String[] comandos = {"python3", "python"};
        
        for (String comando : comandos) {
            try {
                ProcessBuilder pb = new ProcessBuilder(comando, "--version");
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    return comando;
                }
            } catch (Exception e) {
                // Tenta próximo comando
            }
        }
        
        return null;
    }

    /**
     * Força nova execução ignorando o cache
     */
    public boolean executarAtualizacaoCotacoesForcado() {
        lastExecutionTime = 0; // Reseta cache
        return executarAtualizacaoCotacoes();
    }
}

