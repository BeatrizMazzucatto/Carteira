package com.invest.controller;

import com.invest.dto.RelatorioExibicaoResponse;
import com.invest.service.RelatorioExibicaoService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para relatório de exibição agregado
 * Retorna JSON completo para uso no front-end
 */
@RestController
@RequestMapping("/api/relatorio")
@CrossOrigin(origins = "*")
public class RelatorioExibicaoController {

    @Autowired
    private RelatorioExibicaoService relatorioExibicaoService;

    @Operation(summary = "Gera relatório completo de exibição",
               description = "Retorna um JSON agregado com todos os dados do investidor para exibição no front-end")
    @GetMapping("/investidor/{investidorId}")
    public ResponseEntity<RelatorioExibicaoResponse> getRelatorioExibicao(
            @PathVariable Long investidorId) {
        try {
            RelatorioExibicaoResponse relatorio = relatorioExibicaoService.gerarRelatorioExibicao(investidorId);
            return ResponseEntity.ok(relatorio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

