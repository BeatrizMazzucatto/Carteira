package com.invest.controller;

import com.invest.service.CotacaoStreamingService;
import com.invest.service.CotacaoUpdateService;
import com.invest.service.PythonScriptExecutor;
import com.invest.service.external.GoogleSheetsService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller REST para consulta de cotações em tempo real
 * Permite acesso via HTTP às cotações atualizadas periodicamente
 */
@RestController
@RequestMapping("/api/cotacoes")
@CrossOrigin(origins = "*")
public class CotacaoRestController {

    @Autowired
    private CotacaoStreamingService cotacaoStreamingService;

    @Autowired
    private GoogleSheetsService googleSheetsService;

    @Autowired
    private CotacaoUpdateService cotacaoUpdateService;

    @Autowired
    private PythonScriptExecutor pythonScriptExecutor;

    @Operation(summary = "Listar todas as cotações",
               description = "Retorna todas as cotações disponíveis no JSON com timestamp e total de ativos")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCotacoes() {
        // Atualiza o JSON antes de retornar
        pythonScriptExecutor.executarAtualizacaoCotacoes();
        
        Map<String, BigDecimal> cotacoesMap = googleSheetsService.getAllCotacoes();
        
        Map<String, Object> response = new HashMap<>();
        response.put("cotacoes", cotacoesMap);
        response.put("total", cotacoesMap.size());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Buscar cotação de um ativo",
               description = "Retorna a cotação de um ativo específico do JSON pelo código do ativo")
    @GetMapping("/{codigo}")
    public ResponseEntity<Map<String, Object>> getCotacao(@PathVariable String codigo) {
        // Atualiza o JSON antes de buscar
        pythonScriptExecutor.executarAtualizacaoCotacoes();
        
        BigDecimal preco = googleSheetsService.buscarPrecoAtivo(codigo);
        
        if (preco == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("codigo", codigo.toUpperCase());
        response.put("preco", preco);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Forçar atualização de cotações",
               description = "Força recarregamento do cache de cotações do JSON")
    @PostMapping("/atualizar")
    public ResponseEntity<Map<String, String>> forcarAtualizacao() {
        googleSheetsService.forcarRecarregamento();
        cotacaoStreamingService.forcarAtualizacao();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache de cotações recarregado com sucesso");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Recarregar cache de cotações",
               description = "Força recarregamento do cache de cotações do arquivo JSON")
    @PostMapping("/recarregar")
    public ResponseEntity<Map<String, String>> recarregarCache() {
        googleSheetsService.forcarRecarregamento();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache recarregado com sucesso");
        response.put("totalCotacoes", String.valueOf(googleSheetsService.getAllCotacoes().size()));
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Atualizar JSON de cotações do Google Sheets",
               description = "Busca dados atualizados do Google Sheets e atualiza o arquivo JSON")
    @PostMapping("/atualizar-json")
    public ResponseEntity<Map<String, Object>> atualizarJson() {
        try {
            cotacaoUpdateService.atualizarCotacoes();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "JSON de cotações atualizado com sucesso");
            response.put("totalCotacoes", googleSheetsService.getAllCotacoes().size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Erro ao atualizar JSON de cotações");
            response.put("message", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(summary = "Verificar status do serviço de cotações",
               description = "Retorna status do serviço, quantidade de cotações disponíveis e fonte de dados")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "online");
        status.put("cotacoesDisponiveis", googleSheetsService.getAllCotacoes().size());
        status.put("fonte", "cotacoes.json");
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }
}
