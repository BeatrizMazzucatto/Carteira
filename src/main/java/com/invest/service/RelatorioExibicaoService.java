package com.invest.service;

import com.invest.dto.RelatorioExibicaoResponse;
import com.invest.model.*;
import com.invest.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service para gerar relatório agregado de exibição
 */
@Service
@Transactional
public class RelatorioExibicaoService {

    @Autowired
    private InvestidorRepository investidorRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private AtivoRepository ativoRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    /**
     * Gera relatório completo de exibição para um investidor
     */
    public RelatorioExibicaoResponse gerarRelatorioExibicao(Long investidorId) {
        Investidor investidor = investidorRepository.findById(investidorId)
                .orElseThrow(() -> new RuntimeException("Investidor não encontrado: " + investidorId));

        RelatorioExibicaoResponse relatorio = new RelatorioExibicaoResponse();
        
        // Dados do Investidor
        relatorio.setInvestidorId(investidor.getId());
        relatorio.setInvestidorNome(investidor.getNome());
        relatorio.setInvestidorEmail(investidor.getEmail());
        relatorio.setInvestidorDataCriacao(investidor.getDataCriacao());
        relatorio.setInvestidorDataAtualizacao(investidor.getDataAtualizacao());

        // Busca todas as carteiras do investidor
        List<Carteira> carteiras = carteiraRepository.findByInvestidor(investidor);
        
        // Processa cada carteira
        List<RelatorioExibicaoResponse.CarteiraResumo> carteirasResumo = new ArrayList<>();
        BigDecimal valorTotalInvestido = BigDecimal.ZERO;
        BigDecimal valorTotalAtual = BigDecimal.ZERO;
        int totalAtivos = 0;
        int totalTransacoes = 0;
        
        Map<TipoAtivo, RelatorioExibicaoResponse.EstatisticaTipoAtivo> estatisticasPorTipo = new HashMap<>();
        
        for (Carteira carteira : carteiras) {
            RelatorioExibicaoResponse.CarteiraResumo carteiraResumo = processarCarteira(carteira);
            carteirasResumo.add(carteiraResumo);
            
            // Acumula valores totais
            if (carteiraResumo.getValorInicial() != null) {
                valorTotalInvestido = valorTotalInvestido.add(carteiraResumo.getValorInicial());
            }
            if (carteiraResumo.getValorAtual() != null) {
                valorTotalAtual = valorTotalAtual.add(carteiraResumo.getValorAtual());
            }
            totalAtivos += carteiraResumo.getTotalAtivos() != null ? carteiraResumo.getTotalAtivos() : 0;
            totalTransacoes += carteiraResumo.getTotalTransacoes() != null ? carteiraResumo.getTotalTransacoes() : 0;
            
            // Processa estatísticas por tipo de ativo
            if (carteiraResumo.getAtivos() != null) {
                for (RelatorioExibicaoResponse.AtivoResumo ativo : carteiraResumo.getAtivos()) {
                    TipoAtivo tipo = ativo.getTipo();
                    if (tipo != null) {
                        RelatorioExibicaoResponse.EstatisticaTipoAtivo estat = estatisticasPorTipo.getOrDefault(
                            tipo, new RelatorioExibicaoResponse.EstatisticaTipoAtivo());
                        estat.setTipo(tipo);
                        estat.setQuantidade((estat.getQuantidade() != null ? estat.getQuantidade() : 0) + 1);
                        
                        if (ativo.getValorTotalCompra() != null) {
                            BigDecimal valorInvestido = estat.getValorTotalInvestido() != null ? 
                                estat.getValorTotalInvestido() : BigDecimal.ZERO;
                            estat.setValorTotalInvestido(valorInvestido.add(ativo.getValorTotalCompra()));
                        }
                        
                        if (ativo.getValorTotalAtual() != null) {
                            BigDecimal valorAtual = estat.getValorTotalAtual() != null ? 
                                estat.getValorTotalAtual() : BigDecimal.ZERO;
                            estat.setValorTotalAtual(valorAtual.add(ativo.getValorTotalAtual()));
                        }
                        
                        estatisticasPorTipo.put(tipo, estat);
                    }
                }
            }
        }
        
        // Calcula rentabilidade das estatísticas por tipo
        for (RelatorioExibicaoResponse.EstatisticaTipoAtivo estat : estatisticasPorTipo.values()) {
            if (estat.getValorTotalInvestido() != null && estat.getValorTotalAtual() != null) {
                BigDecimal rentabilidade = estat.getValorTotalAtual().subtract(estat.getValorTotalInvestido());
                estat.setRentabilidade(rentabilidade);
                
                if (estat.getValorTotalInvestido().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percentual = rentabilidade
                        .divide(estat.getValorTotalInvestido(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    estat.setRentabilidadePercentual(percentual);
                }
            }
        }
        
        // Resumo Geral
        relatorio.setTotalCarteiras(carteiras.size());
        relatorio.setTotalAtivos(totalAtivos);
        relatorio.setTotalTransacoes(totalTransacoes);
        relatorio.setValorTotalInvestido(valorTotalInvestido);
        relatorio.setValorTotalAtual(valorTotalAtual);
        
        BigDecimal rentabilidadeTotal = valorTotalAtual.subtract(valorTotalInvestido);
        relatorio.setRentabilidadeTotal(rentabilidadeTotal);
        
        if (valorTotalInvestido.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentual = rentabilidadeTotal
                .divide(valorTotalInvestido, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            relatorio.setRentabilidadePercentual(percentual);
        } else {
            relatorio.setRentabilidadePercentual(BigDecimal.ZERO);
        }
        
        relatorio.setCarteiras(carteirasResumo);
        relatorio.setEstatisticasPorTipo(estatisticasPorTipo);
        
        // Transações Recentes (últimas 10)
        List<Transacao> todasTransacoes = new ArrayList<>();
        for (Carteira carteira : carteiras) {
            todasTransacoes.addAll(transacaoRepository.findByCarteira(carteira));
        }
        todasTransacoes.sort((t1, t2) -> {
            if (t1.getDataTransacao() == null && t2.getDataTransacao() == null) return 0;
            if (t1.getDataTransacao() == null) return 1;
            if (t2.getDataTransacao() == null) return -1;
            return t2.getDataTransacao().compareTo(t1.getDataTransacao()); // Mais recente primeiro
        });
        
        List<RelatorioExibicaoResponse.TransacaoResumo> transacoesRecentes = todasTransacoes.stream()
            .limit(10)
            .map(this::convertToTransacaoResumo)
            .collect(Collectors.toList());
        
        relatorio.setTransacoesRecentes(transacoesRecentes);
        relatorio.setDataGeracao(LocalDateTime.now());
        
        return relatorio;
    }
    
    private RelatorioExibicaoResponse.CarteiraResumo processarCarteira(Carteira carteira) {
        RelatorioExibicaoResponse.CarteiraResumo resumo = new RelatorioExibicaoResponse.CarteiraResumo();
        
        resumo.setId(carteira.getId());
        resumo.setNome(carteira.getNome());
        resumo.setDescricao(carteira.getDescricao());
        resumo.setObjetivo(carteira.getObjetivo());
        resumo.setPrazo(carteira.getPrazo());
        resumo.setPerfilRisco(carteira.getPerfilRisco());
        resumo.setValorInicial(carteira.getValorInicial());
        resumo.setValorAtual(carteira.getValorAtual());
        resumo.setDataCriacao(carteira.getDataCriacao());
        resumo.setDataAtualizacao(carteira.getDataAtualizacao());
        
        // Calcula rentabilidade da carteira
        if (carteira.getValorInicial() != null && carteira.getValorAtual() != null) {
            BigDecimal rentabilidade = carteira.getValorAtual().subtract(carteira.getValorInicial());
            resumo.setRentabilidade(rentabilidade);
            
            if (carteira.getValorInicial().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentual = rentabilidade
                    .divide(carteira.getValorInicial(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                resumo.setRentabilidadePercentual(percentual);
            }
        }
        
        // Busca ativos da carteira
        List<Ativo> ativos = ativoRepository.findByCarteira(carteira);
        resumo.setTotalAtivos(ativos.size());
        
        List<RelatorioExibicaoResponse.AtivoResumo> ativosResumo = ativos.stream()
            .map(this::convertToAtivoResumo)
            .collect(Collectors.toList());
        resumo.setAtivos(ativosResumo);
        
        // Conta transações
        long countTransacoes = transacaoRepository.countByCarteira(carteira);
        resumo.setTotalTransacoes((int) countTransacoes);
        
        return resumo;
    }
    
    private RelatorioExibicaoResponse.AtivoResumo convertToAtivoResumo(Ativo ativo) {
        RelatorioExibicaoResponse.AtivoResumo resumo = new RelatorioExibicaoResponse.AtivoResumo();
        
        resumo.setId(ativo.getId());
        resumo.setCodigo(ativo.getCodigo());
        resumo.setNome(ativo.getNome());
        resumo.setTipo(ativo.getTipo());
        resumo.setQuantidade(ativo.getQuantidade());
        resumo.setPrecoCompra(ativo.getPrecoCompra());
        resumo.setPrecoAtual(ativo.getPrecoAtual());
        resumo.setDataCompra(ativo.getDataCompra());
        
        // Calcula valores totais
        if (ativo.getQuantidade() != null && ativo.getPrecoCompra() != null) {
            resumo.setValorTotalCompra(ativo.getQuantidade().multiply(ativo.getPrecoCompra()));
        }
        
        if (ativo.getQuantidade() != null && ativo.getPrecoAtual() != null) {
            resumo.setValorTotalAtual(ativo.getQuantidade().multiply(ativo.getPrecoAtual()));
        }
        
        // Calcula variação percentual
        if (ativo.getPrecoCompra() != null && ativo.getPrecoAtual() != null && 
            ativo.getPrecoCompra().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal variacao = ativo.getPrecoAtual().subtract(ativo.getPrecoCompra());
            BigDecimal percentual = variacao
                .divide(ativo.getPrecoCompra(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            resumo.setVariacaoPercentual(percentual);
        }
        
        return resumo;
    }
    
    private RelatorioExibicaoResponse.TransacaoResumo convertToTransacaoResumo(Transacao transacao) {
        RelatorioExibicaoResponse.TransacaoResumo resumo = new RelatorioExibicaoResponse.TransacaoResumo();
        
        resumo.setId(transacao.getId());
        resumo.setTipoTransacao(transacao.getTipoTransacao());
        resumo.setCodigoAtivo(transacao.getCodigoAtivo());
        resumo.setNomeAtivo(transacao.getNomeAtivo());
        resumo.setTipoAtivo(transacao.getTipoAtivo());
        resumo.setQuantidade(transacao.getQuantidade());
        resumo.setPrecoUnitario(transacao.getPrecoUnitario());
        resumo.setValorTotal(transacao.getValorTotal());
        resumo.setDataTransacao(transacao.getDataTransacao());
        
        if (transacao.getCarteira() != null) {
            resumo.setCarteiraId(transacao.getCarteira().getId());
            resumo.setCarteiraNome(transacao.getCarteira().getNome());
        }
        
        return resumo;
    }
}

