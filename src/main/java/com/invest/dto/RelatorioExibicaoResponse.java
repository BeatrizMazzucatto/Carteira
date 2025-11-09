package com.invest.dto;

import com.invest.model.ObjetivoCarteira;
import com.invest.model.PerfilRisco;
import com.invest.model.PrazoCarteira;
import com.invest.model.TipoAtivo;
import com.invest.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO para relatório agregado de exibição no front-end
 * Contém todos os dados necessários para visualização completa do investidor
 */
public class RelatorioExibicaoResponse {
    
    // Dados do Investidor
    private Long investidorId;
    private String investidorNome;
    private String investidorEmail;
    private LocalDateTime investidorDataCriacao;
    private LocalDateTime investidorDataAtualizacao;
    
    // Resumo Geral
    private Integer totalCarteiras;
    private Integer totalAtivos;
    private Integer totalTransacoes;
    private BigDecimal valorTotalInvestido;
    private BigDecimal valorTotalAtual;
    private BigDecimal rentabilidadeTotal;
    private BigDecimal rentabilidadePercentual;
    
    // Lista de Carteiras
    private List<CarteiraResumo> carteiras;
    
    // Transações Recentes
    private List<TransacaoResumo> transacoesRecentes;
    
    // Estatísticas por Tipo de Ativo
    private Map<TipoAtivo, EstatisticaTipoAtivo> estatisticasPorTipo;
    
    // Timestamp
    private LocalDateTime dataGeracao;
    
    // Classe interna para resumo de carteira
    public static class CarteiraResumo {
        private Long id;
        private String nome;
        private String descricao;
        private ObjetivoCarteira objetivo;
        private PrazoCarteira prazo;
        private PerfilRisco perfilRisco;
        private BigDecimal valorInicial;
        private BigDecimal valorAtual;
        private BigDecimal rentabilidade;
        private BigDecimal rentabilidadePercentual;
        private Integer totalAtivos;
        private Integer totalTransacoes;
        private LocalDateTime dataCriacao;
        private LocalDateTime dataAtualizacao;
        private List<AtivoResumo> ativos;
        
        // Getters e Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        
        public ObjetivoCarteira getObjetivo() { return objetivo; }
        public void setObjetivo(ObjetivoCarteira objetivo) { this.objetivo = objetivo; }
        
        public PrazoCarteira getPrazo() { return prazo; }
        public void setPrazo(PrazoCarteira prazo) { this.prazo = prazo; }
        
        public PerfilRisco getPerfilRisco() { return perfilRisco; }
        public void setPerfilRisco(PerfilRisco perfilRisco) { this.perfilRisco = perfilRisco; }
        
        public BigDecimal getValorInicial() { return valorInicial; }
        public void setValorInicial(BigDecimal valorInicial) { this.valorInicial = valorInicial; }
        
        public BigDecimal getValorAtual() { return valorAtual; }
        public void setValorAtual(BigDecimal valorAtual) { this.valorAtual = valorAtual; }
        
        public BigDecimal getRentabilidade() { return rentabilidade; }
        public void setRentabilidade(BigDecimal rentabilidade) { this.rentabilidade = rentabilidade; }
        
        public BigDecimal getRentabilidadePercentual() { return rentabilidadePercentual; }
        public void setRentabilidadePercentual(BigDecimal rentabilidadePercentual) { this.rentabilidadePercentual = rentabilidadePercentual; }
        
        public Integer getTotalAtivos() { return totalAtivos; }
        public void setTotalAtivos(Integer totalAtivos) { this.totalAtivos = totalAtivos; }
        
        public Integer getTotalTransacoes() { return totalTransacoes; }
        public void setTotalTransacoes(Integer totalTransacoes) { this.totalTransacoes = totalTransacoes; }
        
        public LocalDateTime getDataCriacao() { return dataCriacao; }
        public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
        
        public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
        public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
        
        public List<AtivoResumo> getAtivos() { return ativos; }
        public void setAtivos(List<AtivoResumo> ativos) { this.ativos = ativos; }
    }
    
    // Classe interna para resumo de ativo
    public static class AtivoResumo {
        private Long id;
        private String codigo;
        private String nome;
        private TipoAtivo tipo;
        private BigDecimal quantidade;
        private BigDecimal precoCompra;
        private BigDecimal precoAtual;
        private BigDecimal valorTotalCompra;
        private BigDecimal valorTotalAtual;
        private BigDecimal variacaoPercentual;
        private LocalDateTime dataCompra;
        
        // Getters e Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        
        public TipoAtivo getTipo() { return tipo; }
        public void setTipo(TipoAtivo tipo) { this.tipo = tipo; }
        
        public BigDecimal getQuantidade() { return quantidade; }
        public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
        
        public BigDecimal getPrecoCompra() { return precoCompra; }
        public void setPrecoCompra(BigDecimal precoCompra) { this.precoCompra = precoCompra; }
        
        public BigDecimal getPrecoAtual() { return precoAtual; }
        public void setPrecoAtual(BigDecimal precoAtual) { this.precoAtual = precoAtual; }
        
        public BigDecimal getValorTotalCompra() { return valorTotalCompra; }
        public void setValorTotalCompra(BigDecimal valorTotalCompra) { this.valorTotalCompra = valorTotalCompra; }
        
        public BigDecimal getValorTotalAtual() { return valorTotalAtual; }
        public void setValorTotalAtual(BigDecimal valorTotalAtual) { this.valorTotalAtual = valorTotalAtual; }
        
        public BigDecimal getVariacaoPercentual() { return variacaoPercentual; }
        public void setVariacaoPercentual(BigDecimal variacaoPercentual) { this.variacaoPercentual = variacaoPercentual; }
        
        public LocalDateTime getDataCompra() { return dataCompra; }
        public void setDataCompra(LocalDateTime dataCompra) { this.dataCompra = dataCompra; }
    }
    
    // Classe interna para resumo de transação
    public static class TransacaoResumo {
        private Long id;
        private TipoTransacao tipoTransacao;
        private String codigoAtivo;
        private String nomeAtivo;
        private TipoAtivo tipoAtivo;
        private BigDecimal quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal valorTotal;
        private LocalDateTime dataTransacao;
        private String carteiraNome;
        private Long carteiraId;
        
        // Getters e Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public TipoTransacao getTipoTransacao() { return tipoTransacao; }
        public void setTipoTransacao(TipoTransacao tipoTransacao) { this.tipoTransacao = tipoTransacao; }
        
        public String getCodigoAtivo() { return codigoAtivo; }
        public void setCodigoAtivo(String codigoAtivo) { this.codigoAtivo = codigoAtivo; }
        
        public String getNomeAtivo() { return nomeAtivo; }
        public void setNomeAtivo(String nomeAtivo) { this.nomeAtivo = nomeAtivo; }
        
        public TipoAtivo getTipoAtivo() { return tipoAtivo; }
        public void setTipoAtivo(TipoAtivo tipoAtivo) { this.tipoAtivo = tipoAtivo; }
        
        public BigDecimal getQuantidade() { return quantidade; }
        public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
        
        public BigDecimal getPrecoUnitario() { return precoUnitario; }
        public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
        
        public BigDecimal getValorTotal() { return valorTotal; }
        public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
        
        public LocalDateTime getDataTransacao() { return dataTransacao; }
        public void setDataTransacao(LocalDateTime dataTransacao) { this.dataTransacao = dataTransacao; }
        
        public String getCarteiraNome() { return carteiraNome; }
        public void setCarteiraNome(String carteiraNome) { this.carteiraNome = carteiraNome; }
        
        public Long getCarteiraId() { return carteiraId; }
        public void setCarteiraId(Long carteiraId) { this.carteiraId = carteiraId; }
    }
    
    // Classe interna para estatísticas por tipo de ativo
    public static class EstatisticaTipoAtivo {
        private TipoAtivo tipo;
        private Integer quantidade;
        private BigDecimal valorTotalInvestido;
        private BigDecimal valorTotalAtual;
        private BigDecimal rentabilidade;
        private BigDecimal rentabilidadePercentual;
        
        // Getters e Setters
        public TipoAtivo getTipo() { return tipo; }
        public void setTipo(TipoAtivo tipo) { this.tipo = tipo; }
        
        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
        
        public BigDecimal getValorTotalInvestido() { return valorTotalInvestido; }
        public void setValorTotalInvestido(BigDecimal valorTotalInvestido) { this.valorTotalInvestido = valorTotalInvestido; }
        
        public BigDecimal getValorTotalAtual() { return valorTotalAtual; }
        public void setValorTotalAtual(BigDecimal valorTotalAtual) { this.valorTotalAtual = valorTotalAtual; }
        
        public BigDecimal getRentabilidade() { return rentabilidade; }
        public void setRentabilidade(BigDecimal rentabilidade) { this.rentabilidade = rentabilidade; }
        
        public BigDecimal getRentabilidadePercentual() { return rentabilidadePercentual; }
        public void setRentabilidadePercentual(BigDecimal rentabilidadePercentual) { this.rentabilidadePercentual = rentabilidadePercentual; }
    }
    
    // Getters e Setters principais
    public Long getInvestidorId() { return investidorId; }
    public void setInvestidorId(Long investidorId) { this.investidorId = investidorId; }
    
    public String getInvestidorNome() { return investidorNome; }
    public void setInvestidorNome(String investidorNome) { this.investidorNome = investidorNome; }
    
    public String getInvestidorEmail() { return investidorEmail; }
    public void setInvestidorEmail(String investidorEmail) { this.investidorEmail = investidorEmail; }
    
    public LocalDateTime getInvestidorDataCriacao() { return investidorDataCriacao; }
    public void setInvestidorDataCriacao(LocalDateTime investidorDataCriacao) { this.investidorDataCriacao = investidorDataCriacao; }
    
    public LocalDateTime getInvestidorDataAtualizacao() { return investidorDataAtualizacao; }
    public void setInvestidorDataAtualizacao(LocalDateTime investidorDataAtualizacao) { this.investidorDataAtualizacao = investidorDataAtualizacao; }
    
    public Integer getTotalCarteiras() { return totalCarteiras; }
    public void setTotalCarteiras(Integer totalCarteiras) { this.totalCarteiras = totalCarteiras; }
    
    public Integer getTotalAtivos() { return totalAtivos; }
    public void setTotalAtivos(Integer totalAtivos) { this.totalAtivos = totalAtivos; }
    
    public Integer getTotalTransacoes() { return totalTransacoes; }
    public void setTotalTransacoes(Integer totalTransacoes) { this.totalTransacoes = totalTransacoes; }
    
    public BigDecimal getValorTotalInvestido() { return valorTotalInvestido; }
    public void setValorTotalInvestido(BigDecimal valorTotalInvestido) { this.valorTotalInvestido = valorTotalInvestido; }
    
    public BigDecimal getValorTotalAtual() { return valorTotalAtual; }
    public void setValorTotalAtual(BigDecimal valorTotalAtual) { this.valorTotalAtual = valorTotalAtual; }
    
    public BigDecimal getRentabilidadeTotal() { return rentabilidadeTotal; }
    public void setRentabilidadeTotal(BigDecimal rentabilidadeTotal) { this.rentabilidadeTotal = rentabilidadeTotal; }
    
    public BigDecimal getRentabilidadePercentual() { return rentabilidadePercentual; }
    public void setRentabilidadePercentual(BigDecimal rentabilidadePercentual) { this.rentabilidadePercentual = rentabilidadePercentual; }
    
    public List<CarteiraResumo> getCarteiras() { return carteiras; }
    public void setCarteiras(List<CarteiraResumo> carteiras) { this.carteiras = carteiras; }
    
    public List<TransacaoResumo> getTransacoesRecentes() { return transacoesRecentes; }
    public void setTransacoesRecentes(List<TransacaoResumo> transacoesRecentes) { this.transacoesRecentes = transacoesRecentes; }
    
    public Map<TipoAtivo, EstatisticaTipoAtivo> getEstatisticasPorTipo() { return estatisticasPorTipo; }
    public void setEstatisticasPorTipo(Map<TipoAtivo, EstatisticaTipoAtivo> estatisticasPorTipo) { this.estatisticasPorTipo = estatisticasPorTipo; }
    
    public LocalDateTime getDataGeracao() { return dataGeracao; }
    public void setDataGeracao(LocalDateTime dataGeracao) { this.dataGeracao = dataGeracao; }
}

