package com.invest.controller;

import com.invest.dto.TransacaoRequest;
import com.invest.model.TipoAtivo;
import com.invest.model.TipoTransacao;
import com.invest.model.Transacao;
import com.invest.service.CotacaoStreamingService;
import com.invest.service.TransacaoService;

import io.swagger.v3.oas.annotations.Operation;

import com.invest.dto.CotacaoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller com endpoints simplificados para operações rápidas de compra/venda
 * Usa cotações em tempo real automaticamente
 */
@RestController
@RequestMapping("/api/transacoes/quick")
@CrossOrigin(origins = "*")
public class TransacaoQuickController {

    @Autowired
    private TransacaoService transacaoService;

    @Autowired
    private CotacaoStreamingService cotacaoStreamingService;

    @Operation(summary = "Compra rápida", description = "Realiza uma compra de ativo usando cotação em tempo real")
    @PostMapping("/comprar")
    public ResponseEntity<Map<String, Object>> comprarRapido(@RequestBody Map<String, Object> request) {
        try {
            Long carteiraId = Long.valueOf(request.get("carteiraId").toString());
            String codigoAtivo = request.get("codigoAtivo").toString().toUpperCase();
            BigDecimal quantidade = new BigDecimal(request.get("quantidade").toString());
            BigDecimal taxas = request.containsKey("taxas") ? 
                    new BigDecimal(request.get("taxas").toString()) : BigDecimal.ZERO;

            // Busca cotação em tempo real
            CotacaoDTO cotacao = cotacaoStreamingService.getCotacao(codigoAtivo);
            
            if (cotacao == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cotação não disponível para " + codigoAtivo,
                    "message", "Ativo não encontrado no sistema de cotações"
                ));
            }

            // Cria request de transação
            TransacaoRequest transacaoRequest = new TransacaoRequest();
            transacaoRequest.setTipoTransacao(TipoTransacao.COMPRA);
            transacaoRequest.setCodigoAtivo(codigoAtivo);
            transacaoRequest.setNomeAtivo(codigoAtivo); // Pode ser melhorado
            transacaoRequest.setTipoAtivo(TipoAtivo.ACAO);
            transacaoRequest.setQuantidade(quantidade);
            transacaoRequest.setPrecoUnitario(cotacao.getPrecoAtual());
            transacaoRequest.setTaxasCorretagem(taxas);
            transacaoRequest.setObservacoes("Compra rápida via API - Preço: R$ " + cotacao.getPrecoAtual());

            // Executa transação
            Transacao transacao = transacaoService.createTransacao(carteiraId, transacaoRequest);

            // Resposta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compra realizada com sucesso!");
            response.put("transacaoId", transacao.getId());
            response.put("codigo", codigoAtivo);
            response.put("quantidade", quantidade);
            response.put("precoUnitario", cotacao.getPrecoAtual());
            response.put("valorTotal", transacao.getValorTotal());
            response.put("valorLiquido", transacao.getValorLiquido());
            response.put("dataHora", transacao.getDataTransacao());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Erro ao realizar compra",
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Venda rápida", description = "Realiza uma venda de ativo usando cotação em tempo real")
    @PostMapping("/vender")
    public ResponseEntity<Map<String, Object>> venderRapido(@RequestBody Map<String, Object> request) {
        try {
            Long carteiraId = Long.valueOf(request.get("carteiraId").toString());
            String codigoAtivo = request.get("codigoAtivo").toString().toUpperCase();
            BigDecimal quantidade = new BigDecimal(request.get("quantidade").toString());
            BigDecimal taxas = request.containsKey("taxas") ? 
                    new BigDecimal(request.get("taxas").toString()) : BigDecimal.ZERO;

            // Busca cotação em tempo real
            CotacaoDTO cotacao = cotacaoStreamingService.getCotacao(codigoAtivo);
            
            if (cotacao == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cotação não disponível para " + codigoAtivo,
                    "message", "Ativo não encontrado no sistema de cotações"
                ));
            }

            // Cria request de transação
            TransacaoRequest transacaoRequest = new TransacaoRequest();
            transacaoRequest.setTipoTransacao(TipoTransacao.VENDA);
            transacaoRequest.setCodigoAtivo(codigoAtivo);
            transacaoRequest.setNomeAtivo(codigoAtivo);
            transacaoRequest.setTipoAtivo(TipoAtivo.ACAO);
            transacaoRequest.setQuantidade(quantidade);
            transacaoRequest.setPrecoUnitario(cotacao.getPrecoAtual());
            transacaoRequest.setTaxasCorretagem(taxas);
            transacaoRequest.setObservacoes("Venda rápida via API - Preço: R$ " + cotacao.getPrecoAtual());

            // Executa transação
            Transacao transacao = transacaoService.createTransacao(carteiraId, transacaoRequest);

            // Resposta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venda realizada com sucesso!");
            response.put("transacaoId", transacao.getId());
            response.put("codigo", codigoAtivo);
            response.put("quantidade", quantidade);
            response.put("precoUnitario", cotacao.getPrecoAtual());
            response.put("valorTotal", transacao.getValorTotal());
            response.put("valorLiquido", transacao.getValorLiquido());
            response.put("dataHora", transacao.getDataTransacao());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Erro ao realizar venda",
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Cotação do ativo", description = "Retorna a cotação atual de um ativo específico")
    @GetMapping("/cotacao/{codigo}")
    public ResponseEntity<Map<String, Object>> getCotacaoParaTransacao(@PathVariable String codigo) {
        CotacaoDTO cotacao = cotacaoStreamingService.getCotacao(codigo);
        
        if (cotacao == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("codigo", cotacao.getCodigo());
        response.put("precoAtual", cotacao.getPrecoAtual());
        response.put("variacao", cotacao.getVariacao());
        response.put("dataHora", cotacao.getDataHora());
        response.put("disponivel", true);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Simula compra", description = "Simula uma compra sem executar a transação")
    @PostMapping("/simular/compra")
    public ResponseEntity<Map<String, Object>> simularCompra(@RequestBody Map<String, Object> request) {
        try {
            String codigoAtivo = request.get("codigoAtivo").toString().toUpperCase();
            BigDecimal quantidade = new BigDecimal(request.get("quantidade").toString());
            BigDecimal taxas = request.containsKey("taxas") ? 
                    new BigDecimal(request.get("taxas").toString()) : BigDecimal.ZERO;

            CotacaoDTO cotacao = cotacaoStreamingService.getCotacao(codigoAtivo);
            
            if (cotacao == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cotação não disponível"
                ));
            }

            BigDecimal valorTotal = quantidade.multiply(cotacao.getPrecoAtual());
            BigDecimal valorLiquido = valorTotal.add(taxas);

            Map<String, Object> simulacao = new HashMap<>();
            simulacao.put("codigo", codigoAtivo);
            simulacao.put("quantidade", quantidade);
            simulacao.put("precoUnitario", cotacao.getPrecoAtual());
            simulacao.put("valorTotal", valorTotal);
            simulacao.put("taxas", taxas);
            simulacao.put("valorLiquido", valorLiquido);
            simulacao.put("tipo", "COMPRA");

            return ResponseEntity.ok(simulacao);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Simula venda", description = "Simula uma venda sem executar a transação")
    @PostMapping("/simular/venda")
    public ResponseEntity<Map<String, Object>> simularVenda(@RequestBody Map<String, Object> request) {
        try {
            String codigoAtivo = request.get("codigoAtivo").toString().toUpperCase();
            BigDecimal quantidade = new BigDecimal(request.get("quantidade").toString());
            BigDecimal taxas = request.containsKey("taxas") ? 
                    new BigDecimal(request.get("taxas").toString()) : BigDecimal.ZERO;

            CotacaoDTO cotacao = cotacaoStreamingService.getCotacao(codigoAtivo);
            
            if (cotacao == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cotação não disponível"
                ));
            }

            BigDecimal valorTotal = quantidade.multiply(cotacao.getPrecoAtual());
            BigDecimal valorLiquido = valorTotal.subtract(taxas);

            Map<String, Object> simulacao = new HashMap<>();
            simulacao.put("codigo", codigoAtivo);
            simulacao.put("quantidade", quantidade);
            simulacao.put("precoUnitario", cotacao.getPrecoAtual());
            simulacao.put("valorTotal", valorTotal);
            simulacao.put("taxas", taxas);
            simulacao.put("valorLiquido", valorLiquido);
            simulacao.put("tipo", "VENDA");

            return ResponseEntity.ok(simulacao);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
