package com.invest.controller;

import com.invest.service.PythonScriptExecutor;
import com.invest.service.external.GoogleSheetsService;

import io.swagger.v3.oas.annotations.Operation;

import com.invest.service.CarteiraService;
import com.invest.model.Carteira;
import com.invest.repository.CarteiraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller para integração com Google Sheets via JSON local
 * 
 * Busca cotações de ativos a partir do arquivo cotacoes.json
 * gerado por script Python a partir da planilha do Google Sheets.
 */
@RestController
@RequestMapping("/api/google-sheets")
@CrossOrigin(origins = "*")
public class GoogleSheetsController {
    
    // Substitua ou remova esta linha se não usar mais o serviço antigo
    // @Autowired
    // private GoogleSheetsService googleSheetsService;
    
    @Autowired
    private GoogleSheetsService googleSheetsJsonService; // Correto: injetado
    
    @Autowired
    private PythonScriptExecutor pythonScriptExecutor;
    
    @Autowired
    private CarteiraService carteiraService;
    
    @Autowired
    private CarteiraRepository carteiraRepository;
    
    @Operation(summary = "Buscar preço de um ativo", description = "Retorna o preço de um ativo específico a partir do JSON de cotações")
    @GetMapping("/preco/{codigoAtivo}")
    public ResponseEntity<String> buscarPrecoAtivo(@PathVariable String codigoAtivo) {
        try {
            // Atualiza o JSON antes de buscar
            pythonScriptExecutor.executarAtualizacaoCotacoes();
            
            System.out.println("Buscando preço para: " + codigoAtivo);
            
            //  Chame na instância injetada, não na classe
            BigDecimal preco = googleSheetsJsonService.buscarPrecoAtivo(codigoAtivo);
            
            if (preco != null) {
                return ResponseEntity.ok("Preço do " + codigoAtivo + ": R$ " + preco);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao buscar preço: " + e.getMessage());
            e.printStackTrace(); // opcional, para depuração
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }
    
    // ⚠️ Este endpoint não faz sentido se você não atualiza o JSON via Java
    // Sugestão: desative ou redefina
    @Operation(summary = "Preparar leitura do JSON de cotações", description = "Confirma que o backend está pronto para ler o cotacoes.json atualizado externamente")
    @PostMapping("/atualizar")
    public ResponseEntity<String> atualizarCotacoes() {
        // O arquivo cotacoes.json deve ser atualizado EXTERNAMENTE (ex: script Python)
        // Este endpoint só confirma que o backend está pronto para ler a nova versão
        try {
            System.out.println("✅ Leitura do cotacoes.json será realizada na próxima requisição.");
            return ResponseEntity.ok("Dados prontos para leitura (arquivo JSON deve ser atualizado externamente).");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Sincronizar carteira com Google Sheets", description = "Sincroniza uma carteira específica com os dados do Google Sheets")
    @PostMapping("/carteira/{carteiraId}/sincronizar")
    public ResponseEntity<String> sincronizarCarteira(@PathVariable Long carteiraId) {
        try {
            carteiraService.sincronizarComGoogleSheets(carteiraId);
            return ResponseEntity.ok("Carteira sincronizada com Google Sheets!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro na sincronização: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Atualizar preços de uma carteira", description = "Atualiza os preços de uma carteira específica a partir do JSON")
    @PostMapping("/carteira/{carteiraId}/atualizar-precos")
    public ResponseEntity<String> atualizarPrecosCarteira(@PathVariable Long carteiraId) {
        try {
            carteiraService.atualizarPrecosCarteira(carteiraId);
            return ResponseEntity.ok("Preços da carteira atualizados!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao atualizar preços: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Listar todas as carteiras", description = "Retorna uma lista de todas as carteiras que podem ser sincronizadas")
    @GetMapping("/carteiras")
    public ResponseEntity<List<Carteira>> listarCarteiras() {
        try {
            List<Carteira> carteiras = carteiraRepository.findAll();
            return ResponseEntity.ok(carteiras);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(summary = "Sincronizar todas as carteiras", description = "Sincroniza todas as carteiras com os dados do Google Sheets")
    @PostMapping("/sincronizar-todas")
    public ResponseEntity<String> sincronizarTodasCarteiras() {
        try {
            List<Carteira> carteiras = carteiraRepository.findAll();
            int sucessos = 0;
            int erros = 0;
            
            for (Carteira carteira : carteiras) {
                try {
                    carteiraService.sincronizarComGoogleSheets(carteira.getId());
                    sucessos++;
                } catch (Exception e) {
                    erros++;
                    System.err.println("Erro ao sincronizar carteira " + carteira.getId() + ": " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok("Sincronização concluída! Sucessos: " + sucessos + ", Erros: " + erros);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro na sincronização: " + e.getMessage());
        }
    }
}