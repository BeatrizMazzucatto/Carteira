package com.invest.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.invest.dto.CotacaoDTO;
import com.invest.service.external.GoogleSheetsService;

/**
 * Service responsável por streaming de cotações em tempo real
 * Atualiza cotações periodicamente e envia para clientes conectados via WebSocket
 */
@Service
public class CotacaoStreamingService {

    @Autowired
    private GoogleSheetsService googleSheetsService;

    private Map<String, CotacaoDTO> cotacoesCache = new HashMap<>();

    /**
     * Atualiza cotações a cada 10 segundos e envia para clientes conectados
     * DESATIVADO - Sistema agora usa JSON local
     */
    // @Scheduled(fixedRate = 10000) // 10 segundos - DESATIVADO
    public void atualizarEEnviarCotacoes() {
        // Desativado - sistema agora usa GoogleSheetsService com JSON local
    }

    /**
     * Obtém cotação atual de um ativo específico
     * Se não estiver em cache, busca do GoogleSheetsService
     */
    public CotacaoDTO getCotacao(String codigo) {
        String codigoUpper = codigo.toUpperCase();
        
        // Tenta buscar do cache primeiro
        CotacaoDTO cotacao = cotacoesCache.get(codigoUpper);
        
        if (cotacao != null) {
            return cotacao;
        }
        
        // Se não estiver em cache, busca do GoogleSheetsService
        Map<String, Object> cotacaoMap = googleSheetsService.buscarCotacaoCompleta(codigo);
        
        if (cotacaoMap == null) {
            return null;
        }
        
        // Cria CotacaoDTO a partir dos dados do JSON
        cotacao = new CotacaoDTO();
        cotacao.setCodigo(codigoUpper);
        cotacao.setNome(cotacaoMap.get("nome") != null ? cotacaoMap.get("nome").toString() : "");
        
        if (cotacaoMap.get("precoAtual") != null) {
            cotacao.setPrecoAtual((BigDecimal) cotacaoMap.get("precoAtual"));
        }
        
        if (cotacaoMap.get("variacao") != null) {
            cotacao.setVariacao((BigDecimal) cotacaoMap.get("variacao"));
        }
        
        if (cotacaoMap.get("precoMaximo") != null) {
            cotacao.setPrecoMaximo((BigDecimal) cotacaoMap.get("precoMaximo"));
        }
        
        if (cotacaoMap.get("precoMinimo") != null) {
            cotacao.setPrecoMinimo((BigDecimal) cotacaoMap.get("precoMinimo"));
        }
        
        cotacao.setDataHora(LocalDateTime.now());
        
        // Armazena no cache para próximas consultas
        cotacoesCache.put(codigoUpper, cotacao);
        
        return cotacao;
    }

    /**
     * Obtém todas as cotações em cache
     */
    public Map<String, CotacaoDTO> getAllCotacoes() {
        return new HashMap<>(cotacoesCache);
    }

    /**
     * Força atualização imediata das cotações
     */
    public void forcarAtualizacao() {
        // Força recarregamento do cache do GoogleSheetsService
        googleSheetsService.forcarRecarregamento();
        atualizarEEnviarCotacoes();
    }
}