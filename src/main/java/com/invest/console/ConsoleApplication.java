package com.invest.console;

import java.io.Console;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.invest.model.Carteira;
import com.invest.model.Investidor;
import com.invest.service.AuthService;
import com.invest.service.CarteiraService;
import com.invest.service.InvestidorService;
import com.invest.service.RentabilidadeService;
import com.invest.service.TransacaoService;
import com.invest.service.external.GoogleSheetsService;

/**
 * Aplicação de Console para Sistema de Carteiras
 * Interface amigável para o cliente
 */
@Component
public class ConsoleApplication implements CommandLineRunner {

    @Autowired
    private InvestidorService investidorService;

    @Autowired
    private CarteiraService carteiraService;

    @Autowired
    private TransacaoService transacaoService;

    @Autowired
    private RentabilidadeService rentabilidadeService;

    @Autowired
    private GoogleSheetsService googleSheetsService;

    @Autowired
    private com.invest.repository.AtivoRepository ativoRepository;

    @Autowired
    private com.invest.repository.TransacaoRepository transacaoRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.invest.service.InflacaoService inflacaoService;

    @Autowired
    private com.invest.service.RelatorioExibicaoService relatorioExibicaoService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private com.invest.service.PythonScriptExecutor pythonScriptExecutor;

    private Scanner scanner = new Scanner(System.in);
    private Investidor investidorLogado = null;
    @SuppressWarnings("unused") // Token JWT armazenado para possível uso futuro em requisições autenticadas
    private String jwtToken = null;

    @Override
    public void run(String... args) throws Exception {
        mostrarBanner();
        executarLogin();
    }

    /**
     * Mostra o banner inicial do sistema
     */
    private void mostrarBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                  SISTEMA DE CARTEIRAS                       ║");
        System.out.println("║            Gestao de Investimentos Pessoais                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Executa o processo de login
     */
    private void executarLogin() {
        System.out.println("LOGIN");
        System.out.println("════════");
        System.out.println();

        while (investidorLogado == null) {
            System.out.println("Escolha uma opção:");
            System.out.println("1. Fazer Login");
            System.out.println("2. Criar Nova Conta");
            System.out.println("0. Sair");
            System.out.print("Opção: ");

            int opcao = lerInteiro();
            System.out.println();

            switch (opcao) {
                case 1:
                    fazerLogin();
                    break;
                case 2:
                    criarNovaConta();
                    break;
                case 0:
                    System.out.println("Obrigado por usar o Sistema de Carteiras!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Opcao invalida! Tente novamente.");
                    System.out.println();
            }
        }
        mostrarMenuPrincipal();
    }

    /**
     * Processo de login com autenticação JWT
     */
    private void fazerLogin() {
        System.out.println("LOGIN");
        System.out.println("════════");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Senha: ");
        String senha = lerSenha();

        try {
            // Usa AuthService para autenticação com JWT
            AuthService.AuthResult resultado = authService.authenticate(email, senha);

            if (resultado.isSucesso()) {
                investidorLogado = resultado.getInvestidor();
                jwtToken = resultado.getToken();
                System.out.println();
                System.out.println();
                System.out.println("✅ Login realizado com sucesso!");
                System.out.println();
                System.out.println("Bem-vindo(a), " + investidorLogado.getNome() + "!");
                System.out.println();
                System.out.println();
            } else {
                System.out.println();
                System.out.println("Email ou senha incorretos!");
                System.out.println();
                System.out.println();
                System.out.println("Opções:");
                System.out.println("1. Tentar novamente");
                System.out.println("2. Esqueci a senha");
                System.out.println("0. Voltar ao menu inicial");
                System.out.print("Opção: ");
                
                int opcao = lerInteiro();
                System.out.println();
                
                switch (opcao) {
                    case 1:
                        // Tenta novamente (recursivo)
                        fazerLogin();
                        break;
                    case 2:
                        // Esqueci a senha
                        recuperarSenha(email);
                        break;
                    case 0:
                        // Volta ao menu inicial
                        return;
                    default:
                        System.out.println("Opção inválida!");
                System.out.println();
                }
            }
        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao fazer login: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Recupera/redefine a senha do investidor
     */
    private void recuperarSenha(String email) {
        System.out.println("RECUPERAÇÃO DE SENHA");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        
        // Se o email não foi fornecido, pede
        if (email == null || email.isEmpty()) {
            System.out.print("Digite o email da conta: ");
            email = scanner.nextLine().trim();
        }
        
        try {
            // Verifica se o email existe
            java.util.Optional<com.invest.model.Investidor> investidorOpt = 
                investidorService.getInvestidorByEmail(email);
            
            if (investidorOpt.isEmpty()) {
                System.out.println();
                System.out.println("Email não encontrado no sistema!");
                System.out.println("Verifique o email digitado ou crie uma nova conta.");
                System.out.println();
                System.out.println("Pressione Enter para continuar...");
                scanner.nextLine();
                return;
            }
            
            com.invest.model.Investidor investidor = investidorOpt.get();
            
            System.out.println();
            System.out.println("Email encontrado: " + investidor.getEmail());
            System.out.println("Nome: " + investidor.getNome());
            System.out.println();
            System.out.println("Você pode redefinir sua senha agora.");
            System.out.println();
            
            System.out.print("Nova senha (mínimo 4 caracteres): ");
            String novaSenha = lerSenha();
            
            if (novaSenha.isEmpty()) {
                System.out.println("Senha não pode ser vazia!");
                System.out.println();
                return;
            }
            
            if (novaSenha.length() < 4) {
                System.out.println("Senha deve ter no mínimo 4 caracteres!");
                System.out.println();
                return;
            }
            
            System.out.print("Confirme a nova senha: ");
            String confirmacaoSenha = lerSenha();
            
            if (!novaSenha.equals(confirmacaoSenha)) {
                System.out.println();
                System.out.println("As senhas não coincidem! Tente novamente.");
                System.out.println();
                return;
            }
            
            // Atualiza a senha
            investidor.setSenha(novaSenha);
            investidorService.updateInvestidor(investidor.getId(), investidor);
            
            System.out.println();
            System.out.println();
            System.out.println("✅ Senha redefinida com sucesso!");
            System.out.println();
            System.out.println("Agora você pode fazer login com a nova senha.");
            System.out.println();
            System.out.println();
            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();
            
        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao recuperar senha: " + e.getMessage());
            System.out.println();
            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();
        }
    }

    /**
     * Cria uma nova conta de investidor
     */
    private void criarNovaConta() {
        System.out.println("NOVA CONTA");
        System.out.println("═══════════");
        System.out.print("Nome completo: ");
        String nome = scanner.nextLine().trim();

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Senha (minimo 4 caracteres): ");
        String senha = lerSenha();

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
            System.out.println();
            System.out.println("Nome, email e senha sao obrigatorios!");
            System.out.println();
            return;
        }

        if (senha.length() < 4) {
            System.out.println();
            System.out.println("Senha deve ter no minimo 4 caracteres!");
            System.out.println();
            return;
        }

        try {
            // Verifica se o email já existe
            if (investidorService.getInvestidorByEmail(email).isPresent()) {
                System.out.println();
                System.out.println("Email já cadastrado! Use outro email ou faça login.");
                System.out.println();
                return;
            }

            Investidor novoInvestidor = new Investidor(nome, email, senha);
            investidorLogado = investidorService.createInvestidor(novoInvestidor);
            
            // Gera token JWT após criar a conta
            AuthService.AuthResult resultado = authService.authenticate(email, senha);
            if (resultado.isSucesso()) {
                jwtToken = resultado.getToken();
            }

            System.out.println();
            System.out.println();
            System.out.println("✅ Conta criada com sucesso!");
            System.out.println();
            System.out.println("Bem-vindo(a), " + investidorLogado.getNome() + "!");
            System.out.println();
            System.out.println();
        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao criar conta: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Mostra o menu principal do sistema
     */
    private void mostrarMenuPrincipal() {
        while (true) {
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                        MENU PRINCIPAL                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("Investidor: " + investidorLogado.getNome());
            System.out.println("Email: " + investidorLogado.getEmail());
            System.out.println();
            System.out.println("Escolha uma opção:");
            System.out.println("1. Minhas Carteiras");
            System.out.println("2. Nova Carteira");
            System.out.println("3. Registrar Transação");
            System.out.println("4. Relatório de Rentabilidade Total");
            System.out.println("5. Consultar Ativos");
            System.out.println("6. Configurações");
            System.out.println("7. Relatório de Exibição (JSON)");
            System.out.println("0. Sair");
            System.out.println();
            System.out.print("Opção: ");

            int opcao = lerInteiro();
            System.out.println();

            // Atualiza cotações antes de executar qualquer opção (exceto sair)
            if (opcao != 0 && opcao >= 1 && opcao <= 7) {
                System.out.println("🔄 Atualizando cotações...");
                try {
                    boolean sucesso = pythonScriptExecutor.executarAtualizacaoCotacoes(true); // Modo silencioso
                    if (sucesso) {
                        System.out.println("✅ Cotações atualizadas!");
                    }
                } catch (Exception e) {
                    // Se falhar a atualização, continua mesmo assim
                    System.err.println("⚠️ Aviso: Não foi possível atualizar cotações automaticamente.");
                }
                System.out.println();
            }

            switch (opcao) {
                case 1:
                    mostrarMinhasCarteiras();
                    break;
                case 2:
                    criarNovaCarteira();
                    break;
                case 3:
                    registrarTransacao();
                    break;
                case 4:
                    mostrarRelatorios();
                    break;
                case 5:
                    consultarAtivos();
                    break;
                case 6:
                    mostrarConfiguracoes();
                    break;
                case 7:
                    mostrarRelatorioExibicao();
                    break;
                case 0:
                    System.out.println("Obrigado por usar o Sistema de Carteiras!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Opção inválida! Tente novamente.");
                    System.out.println();
            }
        }
    }

    /**
     * Mostra as carteiras do investidor
     */
    private void mostrarMinhasCarteiras() {
        System.out.println("MINHAS CARTEIRAS");
        System.out.println("═══════════════════");
        System.out.println();

        try {
            List<Carteira> carteiras = carteiraService.getCarteirasByInvestidor(investidorLogado.getId());

            if (carteiras.isEmpty()) {
                System.out.println("Você ainda não possui carteiras.");
                System.out.println("Crie sua primeira carteira no menu principal!");
                System.out.println();
                return;
            }

            System.out.println("Suas carteiras:");
            System.out.println();

            for (int i = 0; i < carteiras.size(); i++) {
                Carteira carteira = carteiras.get(i);
                System.out.println((i + 1) + ". " + carteira.getNome());
                System.out.println("   Descrição: " + carteira.getDescricao());
                System.out.println("   Objetivo: " + (carteira.getObjetivo() != null ? carteira.getObjetivo().getDescricao() : "N/A"));
                System.out.println("   Prazo: " + (carteira.getPrazo() != null ? carteira.getPrazo().getDescricao() : "N/A"));
                System.out.println("   Risco: " + (carteira.getPerfilRisco() != null ? carteira.getPerfilRisco().getDescricao() : "N/A"));
                System.out.println("   Valor Atual: R$ " + formatarValor(carteira.getValorAtual()));
                
                // Busca e exibe os ativos (ações) da carteira
                try {
                    List<com.invest.model.Ativo> ativos = ativoRepository.findByCarteira(carteira);
                    if (ativos != null && !ativos.isEmpty()) {
                        System.out.print("   Ações: ");
                        List<String> codigos = new ArrayList<>();
                        for (com.invest.model.Ativo ativo : ativos) {
                            if (ativo.getQuantidade() != null && ativo.getQuantidade().compareTo(BigDecimal.ZERO) > 0) {
                                codigos.add(ativo.getCodigo());
                            }
                        }
                        if (!codigos.isEmpty()) {
                            System.out.println(String.join(", ", codigos));
                        } else {
                            System.out.println("Nenhuma ação");
                        }
                    } else {
                        System.out.println("   Ações: Nenhuma ação");
                    }
                } catch (Exception e) {
                    System.out.println("   Ações: Erro ao carregar");
                }
                
                System.out.println("   Criada em: " + formatarData(carteira.getDataCriacao()));
                System.out.println();
            }

            System.out.println("Escolha uma carteira para ver detalhes (0 para voltar):");
            System.out.print("Opção: ");
            int opcao = lerInteiro();

            if (opcao == 0) {
                return; // Volta para o menu principal
            }

            if (opcao > 0 && opcao <= carteiras.size()) {
                Carteira carteiraSelecionada = carteiras.get(opcao - 1);
                mostrarDetalhesCarteira(carteiraSelecionada);
            } else {
                System.out.println("Opção inválida! Por favor, escolha um número entre 0 e " + carteiras.size() + ".");
                System.out.println();
            }

        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("No enum constant")) {
                System.out.println("❌ Erro: Alguma carteira possui um valor inválido no banco de dados.");
                System.out.println("   Por favor, atualize os dados da carteira através da interface de edição.");
                System.out.println("   Detalhes: " + e.getMessage());
            } else {
                System.out.println("Erro ao carregar carteiras: " + e.getMessage());
            }
            System.out.println();
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erro ao carregar carteiras: " + e.getMessage());
            System.out.println("Tipo do erro: " + e.getClass().getSimpleName());
            e.printStackTrace();
            System.out.println();
        }
    }

    /**
     * Mostra detalhes de uma carteira específica
     */
    private void mostrarDetalhesCarteira(Carteira carteira) {
        while (true) {
            System.out.println("DETALHES DA CARTEIRA: " + carteira.getNome());
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("Descrição: " + (carteira.getDescricao() != null ? carteira.getDescricao() : "N/A"));
            System.out.println("Objetivo: " + (carteira.getObjetivo() != null ? carteira.getObjetivo().getDescricao() : "N/A"));
            System.out.println("Prazo: " + (carteira.getPrazo() != null ? carteira.getPrazo().getDescricao() : "N/A"));
            System.out.println("Perfil de Risco: " + (carteira.getPerfilRisco() != null ? carteira.getPerfilRisco().getDescricao() : "N/A"));
            System.out.println();
            System.out.println("Valor da Carteira: R$ " + formatarValor(carteira.getValorInicial()));
            System.out.println("Valor Atual: R$ " + formatarValor(carteira.getValorAtual()));
            System.out.println();
            System.out.println("Criada em: " + formatarData(carteira.getDataCriacao()));
            if (carteira.getDataAtualizacao() != null) {
                System.out.println("Modificada em: " + formatarData(carteira.getDataAtualizacao()));
            }
            System.out.println();
            System.out.println();

            System.out.println("Escolha uma opção:");
            System.out.println("1. Registrar Transação");
            System.out.println("2. Ver Rentabilidade");
            System.out.println("3. Ver Transações");
            System.out.println("4. Ver Ativos com Rentabilidade");
            System.out.println("5. Editar Carteira");
            System.out.println("6. Histórico da Carteira");
            System.out.println("7. Análise de Inflação e Valores Deflacionados");
            System.out.println("0. Voltar");
            System.out.println();
            System.out.print("Opção: ");

            int opcao = lerInteiro();
            System.out.println();

            switch (opcao) {
                case 1:
                    registrarTransacaoCarteira(carteira);
                    break;
                case 2:
                    mostrarRentabilidadeCarteira(carteira);
                    break;
                case 3:
                    mostrarTransacoesCarteira(carteira);
                    break;
                case 4:
                    mostrarAtivosCarteira(carteira);
                    break;
                case 5:
                    editarCarteira(carteira);
                    break;
                case 6:
                    mostrarHistoricoCarteira(carteira);
                    break;
                case 7:
                    mostrarAnaliseInflacao(carteira);
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Opção inválida! Tente novamente.");
                    System.out.println();
            }
        }
    }

    /**
     * Cria uma nova carteira
     */
    private void criarNovaCarteira() {
        System.out.println("NOVA CARTEIRA");
        System.out.println("═══════════════");
        System.out.println();
        System.out.println("(Digite '0' ou 'cancelar' a qualquer momento para voltar)");
        System.out.println();

        System.out.print("Nome da carteira (ou 0 para cancelar): ");
        String nome = scanner.nextLine().trim();
        if (nome.equalsIgnoreCase("0") || nome.equalsIgnoreCase("cancelar")) {
            System.out.println("Criação de carteira cancelada.");
            System.out.println();
            return;
        }

        System.out.print("Descrição (opcional, ou 0 para cancelar): ");
        String descricao = scanner.nextLine().trim();
        if (descricao.equalsIgnoreCase("0") || descricao.equalsIgnoreCase("cancelar")) {
            System.out.println("Criação de carteira cancelada.");
            System.out.println();
            return;
        }

        System.out.println();
        System.out.println("Escolha o perfil de risco:");
        System.out.println("1. Baixo Risco");
        System.out.println("2. Moderado Risco");
        System.out.println("3. Alto Risco");
        System.out.println("0. Cancelar");
        System.out.print("Opção: ");

        int riscoOpcao = lerInteiro();
        if (riscoOpcao == 0) {
            System.out.println("Criação de carteira cancelada.");
            System.out.println();
            return;
        }
        com.invest.model.PerfilRisco perfilRisco = obterPerfilRisco(riscoOpcao);

        System.out.println();
        System.out.println("Escolha o objetivo:");
        System.out.println("1. Aposentadoria");
        System.out.println("2. Reserva de Emergência");
        System.out.println("3. Valorização Rápida");
        System.out.println("4. Renda Passiva");
        System.out.println("5. Educação");
        System.out.println("6. Casa Própria");
        System.out.println("7. Viagem");
        System.out.println("8. Outros");
        System.out.println("0. Cancelar");
        System.out.print("Opção: ");

        int objetivoOpcao = lerInteiro();
        if (objetivoOpcao == 0) {
            System.out.println("Criação de carteira cancelada.");
            System.out.println();
            return;
        }
        com.invest.model.ObjetivoCarteira objetivo = obterObjetivoCarteira(objetivoOpcao);

        System.out.println();
        System.out.println("Escolha o prazo:");
        System.out.println("1. Curto Prazo");
        System.out.println("2. Medio Prazo");
        System.out.println("3. Longo Prazo");
        System.out.println("0. Cancelar");
        System.out.print("Opção: ");

        int prazoOpcao = lerInteiro();
        if (prazoOpcao == 0) {
            System.out.println("Criação de carteira cancelada.");
            System.out.println();
            return;
        }
        com.invest.model.PrazoCarteira prazo = obterPrazoCarteira(prazoOpcao);

        System.out.print("Valor da carteira (R$, opcional, ou 0 para cancelar): ");
        String valorInicialStr = scanner.nextLine().trim();
        if (valorInicialStr.equalsIgnoreCase("cancelar")) {
            System.out.println("Criação de carteira cancelada.");
            System.out.println();
            return;
        }
        BigDecimal valorInicial = null;
        if (!valorInicialStr.isEmpty() && !valorInicialStr.equalsIgnoreCase("0")) {
            try {
                valorInicial = new BigDecimal(valorInicialStr.replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido. Usando valor zero para a carteira.");
                valorInicial = BigDecimal.ZERO;
            }
        }

        try {
            com.invest.dto.CarteiraRequest request = new com.invest.dto.CarteiraRequest();
            request.setNome(nome);
            request.setDescricao(descricao);
            request.setObjetivo(objetivo);
            request.setPerfilRisco(perfilRisco);
            request.setPrazo(prazo);
            request.setValorInicial(valorInicial);

            Carteira novaCarteira = carteiraService.createCarteira(investidorLogado.getId(), request);

            System.out.println();
            System.out.println("Carteira criada com sucesso!");
            System.out.println("ID: " + novaCarteira.getId());
            System.out.println("Nome: " + novaCarteira.getNome());
            System.out.println();

        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao criar carteira: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Registra uma nova transação
     */
    private void registrarTransacao() {
        System.out.println("REGISTRAR TRANSAÇÃO");
        System.out.println("══════════════════════");
        System.out.println();

        try {
            List<Carteira> carteiras = carteiraService.getCarteirasByInvestidor(investidorLogado.getId());

            if (carteiras.isEmpty()) {
                System.out.println("Você não possui carteiras.");
                System.out.println("Crie uma carteira primeiro!");
                System.out.println();
                return;
            }

            System.out.println("Escolha a carteira:");
            for (int i = 0; i < carteiras.size(); i++) {
                System.out.println((i + 1) + ". " + carteiras.get(i).getNome());
            }
            System.out.println("0. Voltar");
            System.out.print("Opção: ");

            int carteiraOpcao = lerInteiro();
            if (carteiraOpcao == 0) {
                return;
            }
            if (carteiraOpcao < 1 || carteiraOpcao > carteiras.size()) {
                System.out.println("Carteira inválida!");
                return;
            }

            Carteira carteira = carteiras.get(carteiraOpcao - 1);
            registrarTransacaoCarteira(carteira);

        } catch (Exception e) {
            System.out.println("Erro ao registrar transação: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Registra transação em uma carteira específica
     */
    private void registrarTransacaoCarteira(Carteira carteira) {
        System.out.println("REGISTRAR TRANSAÇÃO - " + carteira.getNome());
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        System.out.println("Tipo de transação:");
        System.out.println("1. Compra");
        System.out.println("2. Venda");
        System.out.println("0. Voltar");
        System.out.print("Opção: ");

        int tipoOpcao = lerInteiro();
        if (tipoOpcao == 0) {
            return;
        }
        com.invest.model.TipoTransacao tipoTransacao = obterTipoTransacao(tipoOpcao);

        // Se for compra, mostra lista de ações disponíveis do JSON
        if (tipoTransacao == com.invest.model.TipoTransacao.COMPRA) {
            mostrarListaAcoesEComprar(carteira);
            return;
        }

        // Se for venda, mostra lista de ações que o usuário possui
        if (tipoTransacao == com.invest.model.TipoTransacao.VENDA) {
            mostrarListaAcoesEVender(carteira);
            return;
        }

        // Para outros tipos de transação, mostra tabela de ações disponíveis
        System.out.println();
        System.out.println("AÇÕES DISPONÍVEIS");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        try {
            Map<String, BigDecimal> cotacoes = googleSheetsService.getAllCotacoes();

            if (cotacoes.isEmpty()) {
                System.out.println("Nenhuma cotação disponível no momento.");
                System.out.println();
            } else {
                // Ordenar por código
                List<String> codigos = new ArrayList<>(cotacoes.keySet());
                Collections.sort(codigos);

                System.out.println("┌─────┬──────────────┬──────────────────────┐");
                System.out.println("│ Op  │ Código       │ Preço               │");
                System.out.println("├─────┼──────────────┼──────────────────────┤");

                int index = 1;
                Map<Integer, String> mapaOpcoes = new HashMap<>();
                for (String codigo : codigos) {
                    BigDecimal preco = cotacoes.get(codigo);
                    mapaOpcoes.put(index, codigo);
                    System.out.printf("│ %-3d │ %-12s │ R$ %-17s │\n", index, codigo, formatarValor(preco));
                    index++;
                }
                System.out.println("└─────┴──────────────┴──────────────────────┘");
                System.out.println();

                System.out.println("Escolha a ação (0 para digitar manualmente):");
                System.out.print("Opção: ");
                int opcao = lerInteiro();

                String codigoAtivo;
                String nomeAtivo;
                BigDecimal precoAtual = null;

                if (opcao == 0) {
                    // Permite digitar manualmente
                    System.out.print("Código do ativo (ex: PETR4) ou 0 para voltar: ");
                    codigoAtivo = scanner.nextLine().trim().toUpperCase();
                    if (codigoAtivo.equals("0")) {
                        return;
                    }
                    System.out.print("Nome do ativo (ex: Petrobras) ou 0 para voltar: ");
                    nomeAtivo = scanner.nextLine().trim();
                    if (nomeAtivo.equals("0")) {
                        return;
                    }
                } else {
                    if (!mapaOpcoes.containsKey(opcao)) {
                        System.out.println("Opção inválida!");
                        System.out.println();
                        return;
                    }
                    codigoAtivo = mapaOpcoes.get(opcao);
                    nomeAtivo = codigoAtivo;
                    precoAtual = cotacoes.get(codigoAtivo);
                    System.out.println();
                    System.out.println("Ação selecionada: " + codigoAtivo);
                    if (precoAtual != null) {
                        System.out.println("Preço atual: R$ " + formatarValor(precoAtual));
                    }
                    System.out.println();
                }

                // Continua com o restante do processo de registro
                System.out.println("Tipo do ativo:");
                System.out.println("1. Ação");
                System.out.println("2. FII");
                System.out.println("3. ETF");
                System.out.println("4. CDB");
                System.out.println("5. LCI/LCA");
                System.out.println("6. Tesouro");
                System.out.println("7. Criptomoeda");
                System.out.print("Opção: ");
                int tipoAtivoOpcao = lerInteiro();
                com.invest.model.TipoAtivo tipoAtivo = obterTipoAtivo(tipoAtivoOpcao);

                System.out.print("Quantidade (ou 0 para voltar): ");
                BigDecimal quantidade = lerDecimal();
                if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
                    if (quantidade.compareTo(BigDecimal.ZERO) == 0) {
                        return;
                    } else {
                        System.out.println("Quantidade deve ser positiva!");
                        return;
                    }
                }

                BigDecimal precoUnitario;
                if (precoAtual != null) {
                    System.out.print("Preço unitário (R$) [Enter para usar preço atual: R$ " + formatarValor(precoAtual) + "]: ");
                    String precoInput = scanner.nextLine().trim();
                    if (precoInput.isEmpty()) {
                        precoUnitario = precoAtual;
                    } else {
                        try {
                            precoUnitario = new BigDecimal(precoInput.replace(",", "."));
                        } catch (NumberFormatException e) {
                            System.out.println("Preço inválido! Usando preço atual.");
                            precoUnitario = precoAtual;
                        }
                    }
                } else {
                    System.out.print("Preço unitário (R$) ou 0 para voltar: ");
                    precoUnitario = lerDecimal();
                    if (precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
                        if (precoUnitario.compareTo(BigDecimal.ZERO) == 0) {
                            return;
                        } else {
                            System.out.println("Preço deve ser positivo!");
                            return;
                        }
                    }
                }

                System.out.print("Taxas de corretagem (R$) [Enter para 0]: ");
                String taxasInput = scanner.nextLine().trim();
                BigDecimal taxas = BigDecimal.ZERO;
                if (!taxasInput.isEmpty()) {
                    try {
                        taxas = new BigDecimal(taxasInput.replace(",", "."));
                    } catch (NumberFormatException e) {
                        System.out.println("Taxa inválida! Usando R$ 0,00.");
                    }
                }

                System.out.print("Impostos (R$) [Enter para 0]: ");
                String impostosInput = scanner.nextLine().trim();
                BigDecimal impostos = BigDecimal.ZERO;
                if (!impostosInput.isEmpty()) {
                    try {
                        impostos = new BigDecimal(impostosInput.replace(",", "."));
                    } catch (NumberFormatException e) {
                        System.out.println("Imposto inválido! Usando R$ 0,00.");
                    }
                }

                System.out.print("Observações (opcional): ");
                String observacoes = scanner.nextLine().trim();

                com.invest.dto.TransacaoRequest request = new com.invest.dto.TransacaoRequest();
                request.setTipoTransacao(tipoTransacao);
                request.setCodigoAtivo(codigoAtivo);
                request.setNomeAtivo(nomeAtivo);
                request.setTipoAtivo(tipoAtivo);
                request.setQuantidade(quantidade);
                request.setPrecoUnitario(precoUnitario);
                request.setTaxasCorretagem(taxas);
                request.setImpostos(impostos);
                request.setObservacoes(observacoes);

                com.invest.model.Transacao transacao = transacaoService.createTransacao(carteira.getId(), request);

                System.out.println();
                System.out.println("Transação registrada com sucesso!");
                System.out.println("Valor total: R$ " + formatarValor(transacao.getValorTotal()));
                System.out.println("Valor líquido: R$ " + formatarValor(transacao.getValorLiquido()));
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao buscar cotações: " + e.getMessage());
            System.out.println();
            // Se houver erro, permite digitar manualmente
            System.out.print("Código do ativo (ex: PETR4) ou 0 para voltar: ");
        String codigoAtivo = scanner.nextLine().trim().toUpperCase();
            if (codigoAtivo.equals("0")) {
                return;
            }

            System.out.print("Nome do ativo (ex: Petrobras) ou 0 para voltar: ");
        String nomeAtivo = scanner.nextLine().trim();
            if (nomeAtivo.equals("0")) {
                return;
            }

        System.out.println();
        System.out.println("Tipo do ativo:");
        System.out.println("1. Ação");
        System.out.println("2. FII");
        System.out.println("3. ETF");
        System.out.println("4. CDB");
        System.out.println("5. LCI/LCA");
        System.out.println("6. Tesouro");
        System.out.println("7. Criptomoeda");
            System.out.println("0. Voltar");
        System.out.print("Opção: ");

        int ativoOpcao = lerInteiro();
            if (ativoOpcao == 0) {
                return;
            }
        com.invest.model.TipoAtivo tipoAtivo = obterTipoAtivo(ativoOpcao);

            System.out.print("Quantidade (ou 0 para voltar): ");
        BigDecimal quantidade = lerDecimal();
            if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
                if (quantidade.compareTo(BigDecimal.ZERO) == 0) {
                    System.out.println("Operação cancelada.");
                    return;
                } else {
                    System.out.println("Quantidade deve ser positiva!");
                    return;
                }
            }

            System.out.print("Preço unitário (R$) ou 0 para voltar: ");
        BigDecimal precoUnitario = lerDecimal();
            if (precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
                if (precoUnitario.compareTo(BigDecimal.ZERO) == 0) {
                    System.out.println("Operação cancelada.");
                    return;
                } else {
                    System.out.println("Preço deve ser positivo!");
                    return;
                }
            }

        // Calcula taxas automaticamente (0,5% do valor total da transação)
        BigDecimal valorTotal = quantidade.multiply(precoUnitario);
        BigDecimal taxas = calcularTaxasCorretagem(valorTotal);
        System.out.println("Taxas/corretagem calculadas automaticamente: R$ " + formatarValor(taxas));

        System.out.print("Observações (opcional): ");
        String observacoes = scanner.nextLine().trim();

        try {
            com.invest.dto.TransacaoRequest request = new com.invest.dto.TransacaoRequest();
            request.setTipoTransacao(tipoTransacao);
            request.setCodigoAtivo(codigoAtivo);
            request.setNomeAtivo(nomeAtivo);
            request.setTipoAtivo(tipoAtivo);
            request.setQuantidade(quantidade);
            request.setPrecoUnitario(precoUnitario);
            request.setTaxasCorretagem(taxas);
            request.setObservacoes(observacoes);

            com.invest.model.Transacao transacao = transacaoService.createTransacao(carteira.getId(), request);

            System.out.println();
            System.out.println("Transação registrada com sucesso!");
            System.out.println("Valor total: R$ " + formatarValor(transacao.getValorTotal()));
            System.out.println("Valor líquido: R$ " + formatarValor(transacao.getValorLiquido()));
            System.out.println();

            } catch (Exception ex) {
            System.out.println();
                System.out.println("Erro ao registrar transação: " + ex.getMessage());
            System.out.println();
            }
        }
    }

    /**
     * Mostra lista de ações disponíveis e permite comprar
     */
    private void mostrarListaAcoesEComprar(Carteira carteira) {
        // Recarrega a carteira do banco para garantir dados atualizados
        carteira = carteiraService.getCarteiraById(carteira.getId());
        
        System.out.println();
        System.out.println("AÇÕES DISPONÍVEIS PARA COMPRA");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        try {
            Map<String, BigDecimal> cotacoes = googleSheetsService.getAllCotacoes();

            if (cotacoes.isEmpty()) {
                System.out.println("Nenhuma cotação disponível no momento.");
                System.out.println();
                return;
            }

            // Ordenar por código
            List<String> codigos = new ArrayList<>(cotacoes.keySet());
            Collections.sort(codigos);

            System.out.println("┌─────┬──────────────┬──────────────────────┐");
            System.out.println("│ Op  │ Código       │ Preço               │");
            System.out.println("├─────┼──────────────┼──────────────────────┤");

            int index = 1;
            Map<Integer, String> mapaOpcoes = new HashMap<>();
            for (String codigo : codigos) {
                BigDecimal preco = cotacoes.get(codigo);
                mapaOpcoes.put(index, codigo);
                System.out.printf("│ %-3d │ %-12s │ R$ %-17s │\n", index, codigo, formatarValor(preco));
                index++;
            }
            System.out.println("└─────┴──────────────┴──────────────────────┘");
            System.out.println();

            System.out.println("Escolha a ação para comprar (0 para voltar):");
            System.out.print("Opção: ");
            int opcao = lerInteiro();

            if (opcao == 0) {
                return;
            }

            if (!mapaOpcoes.containsKey(opcao)) {
                System.out.println("Opção inválida!");
                System.out.println();
                return;
            }

            String codigoAtivo = mapaOpcoes.get(opcao);
            BigDecimal precoAtual = cotacoes.get(codigoAtivo);

            System.out.println();
            System.out.println("Você selecionou: " + codigoAtivo);
            System.out.println("Preço atual: R$ " + formatarValor(precoAtual));
            System.out.println();

            System.out.println("Escolha o tipo de compra:");
            System.out.println("1. Comprar por quantidade de ações");
            System.out.println("2. Comprar por valor total (R$)");
            System.out.println("0. Voltar");
            System.out.print("Opção: ");

            int tipoCompraOpcao = lerInteiro();
            if (tipoCompraOpcao == 0) {
                return;
            }
            
            BigDecimal quantidade;
            BigDecimal precoUnitario = precoAtual;

            if (tipoCompraOpcao == 1) {
                // Compra por quantidade
                System.out.print("Quantidade de ações (ou 0 para voltar): ");
                quantidade = lerDecimal();
                if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
                    if (quantidade.compareTo(BigDecimal.ZERO) == 0) {
                        return;
                    } else {
                        System.out.println("Quantidade deve ser positiva!");
                        return;
                    }
                }
            } else if (tipoCompraOpcao == 2) {
                // Compra por valor total
                System.out.print("Valor total a investir (R$) ou 0 para voltar: ");
                BigDecimal valorTotal = lerDecimal();
                if (valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
                    if (valorTotal.compareTo(BigDecimal.ZERO) == 0) {
                        return;
                    } else {
                        System.out.println("Valor deve ser positivo!");
                        return;
                    }
                }
                quantidade = valorTotal.divide(precoAtual, 4, java.math.RoundingMode.HALF_UP);
                System.out.println("Quantidade calculada: " + formatarQuantidade(quantidade));
            } else {
                System.out.println("Opção inválida!");
                return;
            }

            // Calcula taxas automaticamente (0,5% do valor total da transação)
            BigDecimal valorTotal = quantidade.multiply(precoAtual);
            BigDecimal taxas = calcularTaxasCorretagem(valorTotal);
            BigDecimal valorLiquido = valorTotal.add(taxas);

            // Recalcula valor disponível na carteira (recarrega do banco para garantir dados atualizados)
            carteira = carteiraService.getCarteiraById(carteira.getId());
            BigDecimal valorInicial = carteira.getValorInicial() != null ? carteira.getValorInicial() : BigDecimal.ZERO;
            BigDecimal valorTotalCompras = transacaoRepository.calcularValorTotalCompras(carteira);
            BigDecimal valorTotalTaxas = transacaoRepository.findByCarteira(carteira).stream()
                    .filter(t -> t.getTipoTransacao() == com.invest.model.TipoTransacao.COMPRA)
                    .map(t -> t.getTaxasCorretagem() != null ? t.getTaxasCorretagem() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal valorInvestido = valorTotalCompras.add(valorTotalTaxas);
            BigDecimal valorDisponivel = valorInicial.subtract(valorInvestido);

            // Valida se há saldo suficiente
            if (valorDisponivel.compareTo(valorLiquido) < 0) {
                System.out.println();
                System.out.println("⚠️ SALDO INSUFICIENTE!");
                System.out.println("═══════════════════════════════════════════════════════════════");
                System.out.println();
                System.out.println("Valor disponível na carteira: R$ " + formatarValor(valorDisponivel));
                System.out.println("Valor necessário para esta compra: R$ " + formatarValor(valorLiquido));
                System.out.println("Valor em falta: R$ " + formatarValor(valorLiquido.subtract(valorDisponivel)));
                System.out.println();
                System.out.println();
                System.out.println("Opções:");
                System.out.println("1. Ajustar valor da compra para o saldo disponível");
                System.out.println("2. Alterar valor da carteira e tentar novamente");
                System.out.println("0. Cancelar compra");
                System.out.print("Opção: ");
                
                int opcaoSaldo = lerInteiro();
                System.out.println();
                
                if (opcaoSaldo == 1) {
                    // Ajustar valor da compra para o saldo disponível
                    // Calcula o valor máximo que pode ser investido (considerando taxas B3)
                    // valorDisponivel = valorTotal + taxas
                    // taxas = valorTotal * 0.000325 (taxas B3: 0,0325%)
                    // valorDisponivel = valorTotal + (valorTotal * 0.000325)
                    // valorDisponivel = valorTotal * 1.000325
                    // valorTotal = valorDisponivel / 1.000325
                    BigDecimal taxaB3Percentual = new BigDecimal("0.000325"); // Taxas B3: 0,0325%
                    BigDecimal valorMaximoInvestimento = valorDisponivel.divide(BigDecimal.ONE.add(taxaB3Percentual), 2, java.math.RoundingMode.DOWN);
                    
                    // Verifica se a taxa mínima não excede o valor disponível
                    BigDecimal taxaMinima = new BigDecimal("0.01");
                    if (valorDisponivel.compareTo(taxaMinima) < 0) {
                        System.out.println("Saldo insuficiente mesmo para a taxa mínima de R$ 0,01.");
                        System.out.println();
                        return;
                    }
                    
                    // Ajusta a quantidade baseada no novo valor máximo
                    BigDecimal novaQuantidade = valorMaximoInvestimento.divide(precoAtual, 4, java.math.RoundingMode.DOWN);
                    
                    if (novaQuantidade.compareTo(BigDecimal.ZERO) <= 0) {
                        System.out.println("Não é possível comprar nenhuma ação com o saldo disponível.");
                        System.out.println();
                        return;
                    }
                    
                    // Recalcula com a nova quantidade
                    quantidade = novaQuantidade;
                    valorTotal = quantidade.multiply(precoAtual);
                    taxas = calcularTaxasCorretagem(valorTotal);
                    valorLiquido = valorTotal.add(taxas);
                    
                    System.out.println();
                    System.out.println("✅ Valor da compra ajustado automaticamente!");
                    System.out.println();
                    System.out.println("Nova quantidade: " + formatarQuantidade(quantidade));
                    System.out.println("Novo valor total: R$ " + formatarValor(valorTotal));
                    System.out.println("Taxas/corretagem: R$ " + formatarValor(taxas));
                    System.out.println("Valor líquido: R$ " + formatarValor(valorLiquido));
                    System.out.println();
                    System.out.println();
                    
                    // Continua com o processo de compra com os valores ajustados
                } else if (opcaoSaldo == 2) {
                    // Alterar valor da carteira
                    System.out.print("Novo valor da carteira (R$): ");
                    BigDecimal novoValorInicial = lerDecimal();
                    
                    // Calcula o valor total investido (compras + taxas) - recarrega para garantir dados atualizados
                    carteira = carteiraService.getCarteiraById(carteira.getId());
                    BigDecimal valorTotalComprasValidacao = transacaoRepository.calcularValorTotalCompras(carteira);
                    BigDecimal valorTotalTaxasValidacao = transacaoRepository.findByCarteira(carteira).stream()
                            .filter(t -> t.getTipoTransacao() == com.invest.model.TipoTransacao.COMPRA)
                            .map(t -> t.getTaxasCorretagem() != null ? t.getTaxasCorretagem() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal valorTotalInvestido = valorTotalComprasValidacao.add(valorTotalTaxasValidacao);
                    
                    // Valida se o novo valor não é menor que o total investido
                    if (novoValorInicial.compareTo(valorTotalInvestido) < 0) {
                        System.out.println();
                        System.out.println();
                        System.out.println("⚠️ VALOR INVÁLIDO!");
                        System.out.println("═══════════════════════════════════════════════════════════════");
                        System.out.println();
                        System.out.println("O valor da carteira não pode ser menor que o total investido.");
                        System.out.println();
                        System.out.println("Valor total investido (compras + taxas): R$ " + formatarValor(valorTotalInvestido));
                        System.out.println("Valor informado: R$ " + formatarValor(novoValorInicial));
                        System.out.println("Valor mínimo permitido: R$ " + formatarValor(valorTotalInvestido));
                        System.out.println();
                        System.out.println();
                        return;
                    }
                    
                    try {
                        com.invest.dto.CarteiraRequest requestUpdate = new com.invest.dto.CarteiraRequest();
                        requestUpdate.setNome(carteira.getNome());
                        requestUpdate.setDescricao(carteira.getDescricao());
                        requestUpdate.setObjetivo(carteira.getObjetivo());
                        requestUpdate.setPerfilRisco(carteira.getPerfilRisco());
                        requestUpdate.setPrazo(carteira.getPrazo());
                        requestUpdate.setValorInicial(novoValorInicial);
                        
                        carteiraService.updateCarteira(carteira.getId(), requestUpdate);
                        // Recarrega a carteira do banco para ter os dados atualizados
                        carteira = carteiraService.getCarteiraById(carteira.getId());
                        
                        System.out.println();
                        System.out.println("✅ Valor da carteira atualizado para R$ " + formatarValor(novoValorInicial));
                        System.out.println();
                        System.out.println();
                        System.out.println("Reiniciando processo de compra...");
                        System.out.println();
                        System.out.println();
                        
                        // Reinicia o processo de compra com a carteira atualizada
                        mostrarListaAcoesEComprar(carteira);
                        return;
                    } catch (Exception e) {
                        System.out.println("Erro ao atualizar valor da carteira: " + e.getMessage());
                        System.out.println();
                        return;
                    }
                } else if (opcaoSaldo == 0) {
                    System.out.println("Compra cancelada.");
                    System.out.println();
                    return;
                } else {
                    System.out.println("Opção inválida!");
                    System.out.println();
                    return;
                }
            }

            System.out.print("Observações (opcional): ");
            String observacoes = scanner.nextLine().trim();

            // Confirmar compra
            System.out.println();
            System.out.println();
            System.out.println("RESUMO DA COMPRA:");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("Ação: " + codigoAtivo);
            System.out.println("Quantidade: " + formatarQuantidade(quantidade));
            System.out.println("Preço unitário: R$ " + formatarValor(precoAtual));
            System.out.println();
            System.out.println("Valor total: R$ " + formatarValor(valorTotal));
            System.out.println("Taxas/corretagem (calculadas automaticamente): R$ " + formatarValor(taxas));
            System.out.println("Valor líquido: R$ " + formatarValor(valorLiquido));
            System.out.println();
            System.out.println("Valor disponível na carteira: R$ " + formatarValor(valorDisponivel));
            System.out.println("Valor restante após compra: R$ " + formatarValor(valorDisponivel.subtract(valorLiquido)));
            System.out.println();
            System.out.println();
            System.out.print("Confirmar compra? (S/N): ");
            String confirmacao = scanner.nextLine().trim().toUpperCase();

            if (!confirmacao.equals("S")) {
                System.out.println();
                System.out.println("Compra cancelada.");
                System.out.println();
                System.out.println();
                return;
            }

            // Registrar compra
            com.invest.dto.TransacaoRequest request = new com.invest.dto.TransacaoRequest();
            request.setTipoTransacao(com.invest.model.TipoTransacao.COMPRA);
            request.setCodigoAtivo(codigoAtivo);
            request.setNomeAtivo(codigoAtivo);
            request.setTipoAtivo(com.invest.model.TipoAtivo.ACAO);
            request.setQuantidade(quantidade);
            request.setPrecoUnitario(precoUnitario);
            request.setTaxasCorretagem(taxas);
            request.setObservacoes(observacoes);

            com.invest.model.Transacao transacao = transacaoService.createTransacao(carteira.getId(), request);

            System.out.println();
            System.out.println();
            System.out.println("✅ Compra registrada com sucesso!");
            System.out.println();
            System.out.println("Valor total: R$ " + formatarValor(transacao.getValorTotal()));
            System.out.println("Valor líquido: R$ " + formatarValor(transacao.getValorLiquido()));
            System.out.println();
            System.out.println();

        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao processar compra: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Mostra lista de ações que o usuário possui e permite vender
     */
    private void mostrarListaAcoesEVender(Carteira carteira) {
        // Recarrega a carteira do banco para garantir dados atualizados
        carteira = carteiraService.getCarteiraById(carteira.getId());
        
        System.out.println();
        System.out.println("AÇÕES DISPONÍVEIS PARA VENDA");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        try {
            // Busca os ativos da carteira que têm quantidade > 0
            List<com.invest.model.Ativo> ativosCarteira = ativoRepository.findByCarteira(carteira);
            List<com.invest.model.Ativo> ativosDisponiveis = new ArrayList<>();
            
            for (com.invest.model.Ativo ativo : ativosCarteira) {
                if (ativo.getQuantidade() != null && ativo.getQuantidade().compareTo(BigDecimal.ZERO) > 0) {
                    ativosDisponiveis.add(ativo);
                }
            }

            if (ativosDisponiveis.isEmpty()) {
                System.out.println("Você não possui ações para vender nesta carteira.");
                System.out.println();
                System.out.println("Pressione Enter para continuar...");
                scanner.nextLine();
                System.out.println();
                return;
            }

            // Busca cotações atualizadas
            Map<String, BigDecimal> cotacoes = googleSheetsService.getAllCotacoes();

            // Ordenar por código
            ativosDisponiveis.sort((a1, a2) -> a1.getCodigo().compareTo(a2.getCodigo()));

            System.out.println("┌─────┬──────────────┬──────────────────────┬──────────────────────┬──────────────────────┐");
            System.out.println("│ Op  │ Código       │ Quantidade          │ Preço Médio          │ Preço Atual          │");
            System.out.println("├─────┼──────────────┼──────────────────────┼──────────────────────┼──────────────────────┤");

            int index = 1;
            Map<Integer, com.invest.model.Ativo> mapaOpcoes = new HashMap<>();
            for (com.invest.model.Ativo ativo : ativosDisponiveis) {
                mapaOpcoes.put(index, ativo);
                BigDecimal precoAtual = cotacoes.getOrDefault(ativo.getCodigo(), ativo.getPrecoAtual());
                if (precoAtual == null) {
                    precoAtual = ativo.getPrecoCompra();
                }
                System.out.printf("│ %-3d │ %-12s │ %-20s │ R$ %-17s │ R$ %-17s │\n", 
                    index, 
                    ativo.getCodigo(), 
                    formatarQuantidade(ativo.getQuantidade()),
                    formatarValor(ativo.getPrecoCompra()),
                    formatarValor(precoAtual));
                index++;
            }
            System.out.println("└─────┴──────────────┴──────────────────────┴──────────────────────┴──────────────────────┘");
            System.out.println();

            System.out.println("Escolha a ação para vender (0 para voltar):");
            System.out.print("Opção: ");
            int opcao = lerInteiro();

            if (opcao == 0) {
                return;
            }

            if (!mapaOpcoes.containsKey(opcao)) {
                System.out.println("Opção inválida!");
                System.out.println();
                return;
            }

            com.invest.model.Ativo ativoSelecionado = mapaOpcoes.get(opcao);
            String codigoAtivo = ativoSelecionado.getCodigo();
            BigDecimal quantidadeDisponivel = ativoSelecionado.getQuantidade();
            BigDecimal precoMedio = ativoSelecionado.getPrecoCompra();
            
            // Busca preço atual do JSON ou usa o preço do ativo
            BigDecimal precoAtual = cotacoes.getOrDefault(codigoAtivo, ativoSelecionado.getPrecoAtual());
            if (precoAtual == null) {
                precoAtual = precoMedio;
            }

            System.out.println();
            System.out.println("Você selecionou: " + codigoAtivo);
            System.out.println("Quantidade disponível: " + formatarQuantidade(quantidadeDisponivel));
            System.out.println("Preço médio de compra: R$ " + formatarValor(precoMedio));
            System.out.println("Preço atual: R$ " + formatarValor(precoAtual));
            System.out.println();

            System.out.println("Escolha o tipo de venda:");
            System.out.println("1. Vender por quantidade de ações");
            System.out.println("2. Vender por valor total (R$)");
            System.out.println("0. Voltar");
            System.out.print("Opção: ");

            int tipoVendaOpcao = lerInteiro();
            if (tipoVendaOpcao == 0) {
                return;
            }
            
            BigDecimal quantidade;
            BigDecimal precoUnitario = precoAtual;

            if (tipoVendaOpcao == 1) {
                // Venda por quantidade
                System.out.print("Quantidade de ações para vender (ou 0 para voltar): ");
                quantidade = lerDecimal();
                if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
                    if (quantidade.compareTo(BigDecimal.ZERO) == 0) {
                        return;
                    } else {
                        System.out.println();
                        System.out.println("Quantidade deve ser positiva!");
                        System.out.println();
                        return;
                    }
                }
                
                // Valida se tem quantidade suficiente
                if (quantidade.compareTo(quantidadeDisponivel) > 0) {
                    System.out.println();
                    System.out.println("⚠️ QUANTIDADE INSUFICIENTE!");
                    System.out.println("═══════════════════════════════════════════════════════════════");
                    System.out.println();
                    System.out.println("Quantidade disponível: " + formatarQuantidade(quantidadeDisponivel));
                    System.out.println("Quantidade solicitada: " + formatarQuantidade(quantidade));
                    System.out.println("Quantidade em falta: " + formatarQuantidade(quantidade.subtract(quantidadeDisponivel)));
                    System.out.println();
                    System.out.println();
                    System.out.println("Opções:");
                    System.out.println("1. Ajustar quantidade para o disponível");
                    System.out.println("0. Cancelar venda");
                    System.out.print("Opção: ");
                    
                    int opcaoQuantidade = lerInteiro();
                    System.out.println();
                    
                    if (opcaoQuantidade == 1) {
                        quantidade = quantidadeDisponivel;
                        System.out.println();
                        System.out.println("✅ Quantidade ajustada para " + formatarQuantidade(quantidade));
                        System.out.println();
                    } else {
                        System.out.println("Venda cancelada.");
                        System.out.println();
                        return;
                    }
                }
            } else if (tipoVendaOpcao == 2) {
                // Venda por valor total
                System.out.print("Valor total a receber (R$) ou 0 para voltar: ");
                BigDecimal valorTotal = lerDecimal();
                if (valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
                    if (valorTotal.compareTo(BigDecimal.ZERO) == 0) {
                        return;
                    } else {
                        System.out.println();
                        System.out.println("Valor deve ser positivo!");
                        System.out.println();
                        return;
                    }
                }
                quantidade = valorTotal.divide(precoAtual, 4, java.math.RoundingMode.DOWN);
                System.out.println("Quantidade calculada: " + formatarQuantidade(quantidade));
                
                // Valida se tem quantidade suficiente
                if (quantidade.compareTo(quantidadeDisponivel) > 0) {
                    System.out.println();
                    System.out.println("⚠️ QUANTIDADE INSUFICIENTE!");
                    System.out.println("═══════════════════════════════════════════════════════════════");
                    System.out.println();
                    System.out.println("Quantidade disponível: " + formatarQuantidade(quantidadeDisponivel));
                    System.out.println("Quantidade necessária: " + formatarQuantidade(quantidade));
                    System.out.println();
                    System.out.println("Valor máximo que pode receber: R$ " + 
                        formatarValor(quantidadeDisponivel.multiply(precoAtual)));
                    System.out.println();
                    System.out.println();
                    System.out.println("Opções:");
                    System.out.println("1. Ajustar valor para o máximo disponível");
                    System.out.println("0. Cancelar venda");
                    System.out.print("Opção: ");
                    
                    int opcaoValor = lerInteiro();
                    System.out.println();
                    
                    if (opcaoValor == 1) {
                        valorTotal = quantidadeDisponivel.multiply(precoAtual);
                        quantidade = quantidadeDisponivel;
                        System.out.println();
                        System.out.println("✅ Valor ajustado para R$ " + formatarValor(valorTotal));
                        System.out.println("Quantidade: " + formatarQuantidade(quantidade));
                        System.out.println();
                    } else {
                        System.out.println("Venda cancelada.");
                        System.out.println();
                        return;
                    }
                }
            } else {
                System.out.println("Opção inválida!");
                System.out.println();
                return;
            }

            // Calcula taxas automaticamente (0,5% do valor total da transação)
            BigDecimal valorTotal = quantidade.multiply(precoAtual);
            BigDecimal taxas = calcularTaxasCorretagem(valorTotal);
            BigDecimal valorLiquido = valorTotal.subtract(taxas);

            System.out.print("Observações (opcional): ");
            String observacoes = scanner.nextLine().trim();

            // Confirmar venda
            System.out.println();
            System.out.println();
            System.out.println("RESUMO DA VENDA:");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("Ação: " + codigoAtivo);
            System.out.println("Quantidade: " + formatarQuantidade(quantidade));
            System.out.println("Preço unitário: R$ " + formatarValor(precoAtual));
            System.out.println("Preço médio de compra: R$ " + formatarValor(precoMedio));
            System.out.println();
            System.out.println("Valor total: R$ " + formatarValor(valorTotal));
            System.out.println("Taxas/corretagem (calculadas automaticamente): R$ " + formatarValor(taxas));
            System.out.println("Valor líquido a receber: R$ " + formatarValor(valorLiquido));
            System.out.println();
            
            // Calcula lucro/prejuízo
            BigDecimal valorInvestido = quantidade.multiply(precoMedio);
            BigDecimal lucroPrejuizo = valorLiquido.subtract(valorInvestido);
            String sinal = lucroPrejuizo.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            System.out.println("Valor investido (custo): R$ " + formatarValor(valorInvestido));
            System.out.println("Lucro/Prejuízo: R$ " + sinal + formatarValor(lucroPrejuizo));
            System.out.println();
            System.out.println();
            System.out.print("Confirmar venda? (S/N): ");
            String confirmacao = scanner.nextLine().trim().toUpperCase();

            if (!confirmacao.equals("S")) {
                System.out.println();
                System.out.println("Venda cancelada.");
                System.out.println();
                System.out.println();
                return;
            }

            // Registrar venda
            com.invest.dto.TransacaoRequest request = new com.invest.dto.TransacaoRequest();
            request.setTipoTransacao(com.invest.model.TipoTransacao.VENDA);
            request.setCodigoAtivo(codigoAtivo);
            request.setNomeAtivo(ativoSelecionado.getNome());
            request.setTipoAtivo(ativoSelecionado.getTipo());
            request.setQuantidade(quantidade);
            request.setPrecoUnitario(precoUnitario);
            request.setTaxasCorretagem(taxas);
            request.setObservacoes(observacoes);

            com.invest.model.Transacao transacao = transacaoService.createTransacao(carteira.getId(), request);

            System.out.println();
            System.out.println();
            System.out.println("✅ Venda registrada com sucesso!");
            System.out.println();
            System.out.println("Valor total: R$ " + formatarValor(transacao.getValorTotal()));
            System.out.println("Valor líquido recebido: R$ " + formatarValor(transacao.getValorLiquido()));
            System.out.println();
            System.out.println();

        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao processar venda: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();
        }
    }

    /**
     * Mostra relatório consolidado de rentabilidade de todas as carteiras
     */
    private void mostrarRelatorios() {
        System.out.println("RELATÓRIO DE RENTABILIDADE - CONSOLIDADO");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        try {
            List<Carteira> carteiras = carteiraService.getCarteirasByInvestidor(investidorLogado.getId());

            if (carteiras.isEmpty()) {
                System.out.println("Você não possui carteiras.");
                System.out.println();
                System.out.println("Pressione Enter para continuar...");
                scanner.nextLine();
                return;
            }

            // Variáveis para consolidar todas as carteiras
            BigDecimal valorTotalInvestidoGeral = BigDecimal.ZERO;
            BigDecimal valorAtualMercadoGeral = BigDecimal.ZERO;
            BigDecimal valorAtualComProventosGeral = BigDecimal.ZERO;
            BigDecimal totalComprasGeral = BigDecimal.ZERO;
            BigDecimal totalVendasGeral = BigDecimal.ZERO;
            BigDecimal totalProventosGeral = BigDecimal.ZERO;
            BigDecimal totalTaxasGeral = BigDecimal.ZERO;
            BigDecimal totalImpostosGeral = BigDecimal.ZERO;
            int totalCarteiras = carteiras.size();
            int totalAtivos = 0;
            int ativosPositivos = 0;
            int ativosNegativos = 0;

            System.out.println("RESUMO POR CARTEIRA:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println();

            // Calcula rentabilidade de cada carteira e consolida
            for (Carteira carteira : carteiras) {
                try {
                    com.invest.dto.CarteiraRentabilidadeResponse rentabilidade = 
                        rentabilidadeService.calcularRentabilidadeCarteira(carteira.getId());

                    BigDecimal valorInvestido = rentabilidade.getValorTotalInvestido() != null 
                        ? rentabilidade.getValorTotalInvestido() : BigDecimal.ZERO;
                    BigDecimal valorMercado = rentabilidade.getValorAtualMercado() != null 
                        ? rentabilidade.getValorAtualMercado() : BigDecimal.ZERO;
                    BigDecimal valorComProventos = rentabilidade.getValorAtualComProventos() != null 
                        ? rentabilidade.getValorAtualComProventos() : BigDecimal.ZERO;
                    BigDecimal compras = rentabilidade.getValorTotalCompras() != null 
                        ? rentabilidade.getValorTotalCompras() : BigDecimal.ZERO;
                    BigDecimal vendas = rentabilidade.getValorTotalVendas() != null 
                        ? rentabilidade.getValorTotalVendas() : BigDecimal.ZERO;
                    BigDecimal proventos = rentabilidade.getValorTotalProventos() != null 
                        ? rentabilidade.getValorTotalProventos() : BigDecimal.ZERO;
                    BigDecimal taxas = rentabilidade.getTotalTaxasCorretagem() != null 
                        ? rentabilidade.getTotalTaxasCorretagem() : BigDecimal.ZERO;
                    BigDecimal impostos = rentabilidade.getTotalImpostos() != null 
                        ? rentabilidade.getTotalImpostos() : BigDecimal.ZERO;

                    valorTotalInvestidoGeral = valorTotalInvestidoGeral.add(valorInvestido);
                    valorAtualMercadoGeral = valorAtualMercadoGeral.add(valorMercado);
                    valorAtualComProventosGeral = valorAtualComProventosGeral.add(valorComProventos);
                    totalComprasGeral = totalComprasGeral.add(compras);
                    totalVendasGeral = totalVendasGeral.add(vendas);
                    totalProventosGeral = totalProventosGeral.add(proventos);
                    totalTaxasGeral = totalTaxasGeral.add(taxas);
                    totalImpostosGeral = totalImpostosGeral.add(impostos);

                    totalAtivos += rentabilidade.getTotalAtivos() != null ? rentabilidade.getTotalAtivos() : 0;
                    ativosPositivos += rentabilidade.getAtivosPositivos() != null ? rentabilidade.getAtivosPositivos() : 0;
                    ativosNegativos += rentabilidade.getAtivosNegativos() != null ? rentabilidade.getAtivosNegativos() : 0;

                    // Exibe resumo da carteira
                    System.out.println("📊 " + carteira.getNome() + ":");
                    System.out.println("   Investido: R$ " + formatarValor(valorInvestido));
                    System.out.println("   Valor Atual: R$ " + formatarValor(valorMercado));
                    
                    BigDecimal rentabilidadeCarteira = valorMercado.subtract(valorInvestido);
                    BigDecimal percentualCarteira = BigDecimal.ZERO;
                    if (valorInvestido.compareTo(BigDecimal.ZERO) > 0) {
                        percentualCarteira = rentabilidadeCarteira
                            .divide(valorInvestido, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    }
                    String sinal = rentabilidadeCarteira.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                    System.out.println("   Rentabilidade: R$ " + sinal + formatarValor(rentabilidadeCarteira) + 
                                     " (" + sinal + formatarPercentual(percentualCarteira) + "%)");
                    System.out.println();

                } catch (Exception e) {
                    System.out.println("⚠️ Erro ao calcular rentabilidade da carteira '" + carteira.getNome() + "': " + e.getMessage());
                    System.out.println();
            }
            }

            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("RESUMO GERAL CONSOLIDADO");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();

            System.out.println("INFORMAÇÕES GERAIS:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Total de Carteiras: " + totalCarteiras);
            System.out.println("Total de Ativos: " + totalAtivos);
            System.out.println("Ativos com Rentabilidade Positiva: " + ativosPositivos);
            System.out.println("Ativos com Rentabilidade Negativa: " + ativosNegativos);
            System.out.println();

            System.out.println("VALORES CONSOLIDADOS:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Valor Total Investido: R$ " + formatarValor(valorTotalInvestidoGeral));
            System.out.println("Valor Atual de Mercado: R$ " + formatarValor(valorAtualMercadoGeral));
            System.out.println("Valor com Proventos: R$ " + formatarValor(valorAtualComProventosGeral));
            System.out.println();

            System.out.println("MOVIMENTAÇÃO:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Total de Compras: R$ " + formatarValor(totalComprasGeral));
            System.out.println("Total de Vendas: R$ " + formatarValor(totalVendasGeral));
            System.out.println("Total de Proventos: R$ " + formatarValor(totalProventosGeral));
            System.out.println();

            System.out.println("CUSTOS:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Total de Taxas: R$ " + formatarValor(totalTaxasGeral));
            System.out.println("Total de Impostos: R$ " + formatarValor(totalImpostosGeral));
            BigDecimal totalCustos = totalTaxasGeral.add(totalImpostosGeral);
            System.out.println("Total de Custos: R$ " + formatarValor(totalCustos));
            System.out.println();

            System.out.println("RENTABILIDADE CONSOLIDADA:");
            System.out.println("───────────────────────────────────────────────────────────");
            BigDecimal rentabilidadeBruta = valorAtualMercadoGeral.subtract(valorTotalInvestidoGeral);
            BigDecimal rentabilidadeLiquida = valorAtualComProventosGeral.subtract(valorTotalInvestidoGeral).subtract(totalCustos);
            
            BigDecimal percentualBruto = BigDecimal.ZERO;
            BigDecimal percentualLiquido = BigDecimal.ZERO;
            if (valorTotalInvestidoGeral.compareTo(BigDecimal.ZERO) > 0) {
                percentualBruto = rentabilidadeBruta
                    .divide(valorTotalInvestidoGeral, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                percentualLiquido = rentabilidadeLiquida
                    .divide(valorTotalInvestidoGeral, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            }

            String sinalBruto = rentabilidadeBruta.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            String sinalLiquido = rentabilidadeLiquida.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            
            System.out.println("Rentabilidade Bruta: R$ " + sinalBruto + formatarValor(rentabilidadeBruta) + 
                             " (" + sinalBruto + formatarPercentual(percentualBruto) + "%)");
            System.out.println("Rentabilidade Líquida: R$ " + sinalLiquido + formatarValor(rentabilidadeLiquida) + 
                             " (" + sinalLiquido + formatarPercentual(percentualLiquido) + "%)");
            System.out.println();

            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();
            System.out.println();

        } catch (Exception e) {
            System.out.println("Erro ao gerar relatório: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();
        }
    }

    /**
     * Mostra rentabilidade de uma carteira
     */
    private void mostrarRentabilidadeCarteira(Carteira carteira) {
        System.out.println("RENTABILIDADE - " + carteira.getNome());
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        try {
            com.invest.dto.CarteiraRentabilidadeResponse rentabilidade = 
                rentabilidadeService.calcularRentabilidadeCarteira(carteira.getId());

            System.out.println("RESUMO FINANCEIRO");
            System.out.println("────────────────────");
            System.out.println("Valor Total Investido: R$ " + formatarValor(rentabilidade.getValorTotalInvestido()));
            System.out.println("Valor Atual de Mercado: R$ " + formatarValor(rentabilidade.getValorAtualMercado()));
            System.out.println("Valor com Proventos: R$ " + formatarValor(rentabilidade.getValorAtualComProventos()));
            System.out.println();

            System.out.println("RENTABILIDADE");
            System.out.println("────────────────");
            System.out.println("Rentabilidade Bruta: R$ " + formatarValor(rentabilidade.getRentabilidadeBruta()));
            System.out.println("Rentabilidade Líquida: R$ " + formatarValor(rentabilidade.getRentabilidadeLiquida()));
            System.out.println("Rentabilidade % Bruta: " + formatarPercentual(rentabilidade.getRentabilidadePercentualBruta()) + "%");
            System.out.println("Rentabilidade % Líquida: " + formatarPercentual(rentabilidade.getRentabilidadePercentualLiquida()) + "%");
            System.out.println();

            System.out.println("CUSTOS");
            System.out.println("─────────");
            System.out.println("Total Taxas: R$ " + formatarValor(rentabilidade.getTotalTaxasCorretagem()));
            System.out.println("Total Impostos: R$ " + formatarValor(rentabilidade.getTotalImpostos()));
            System.out.println("Total Custos: R$ " + formatarValor(rentabilidade.getTotalCustos()));
            System.out.println();

            System.out.println("COMPOSIÇÃO");
            System.out.println("─────────────");
            System.out.println("Ações: " + formatarPercentual(rentabilidade.getPercentualAcoes()) + "%");
            System.out.println("FIIs: " + formatarPercentual(rentabilidade.getPercentualFIIs()) + "%");
            System.out.println("ETFs: " + formatarPercentual(rentabilidade.getPercentualETFs()) + "%");
            System.out.println("Renda Fixa: " + formatarPercentual(rentabilidade.getPercentualRendaFixa()) + "%");
            System.out.println();

            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();

        } catch (Exception e) {
            System.out.println("Erro ao calcular rentabilidade: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Consulta ativos
     */
    private void consultarAtivos() {
        System.out.println("CONSULTAR ATIVOS");
        System.out.println("═══════════════════");
        System.out.println();

        try {
            List<Carteira> carteiras = carteiraService.getCarteirasByInvestidor(investidorLogado.getId());

            if (carteiras.isEmpty()) {
                System.out.println("Você não possui carteiras.");
                System.out.println();
                return;
            }

            System.out.println("Escolha a carteira:");
            for (int i = 0; i < carteiras.size(); i++) {
                System.out.println((i + 1) + ". " + carteiras.get(i).getNome());
            }
            System.out.println("0. Voltar");
            System.out.print("Opção: ");

            int opcao = lerInteiro();
            if (opcao == 0) {
                return;
            }
            if (opcao < 1 || opcao > carteiras.size()) {
                System.out.println("Carteira inválida!");
                return;
            }

            Carteira carteira = carteiras.get(opcao - 1);
            mostrarAtivosCarteira(carteira);

        } catch (Exception e) {
            System.out.println("Erro ao consultar ativos: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Mostra ativos de uma carteira com rentabilidade individual
     */
    private void mostrarAtivosCarteira(Carteira carteira) {
        System.out.println("ATIVOS COM RENTABILIDADE - " + carteira.getNome());
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        try {
            // Atualiza preços dos ativos antes de exibir
            System.out.println("Atualizando preços dos ativos...");
            carteiraService.atualizarPrecosCarteira(carteira.getId());
            // Recarrega a carteira para ter os dados atualizados
            carteira = carteiraService.getCarteiraById(carteira.getId());
            System.out.println("Preços atualizados!");
            System.out.println();

            // Busca ativos diretamente do repository para evitar LazyInitializationException
            List<com.invest.model.Ativo> ativos = ativoRepository.findByCarteira(carteira);

            if (ativos.isEmpty()) {
                System.out.println("Esta carteira não possui ativos.");
                System.out.println("Registre uma transação ou compre ações do mercado para adicionar ativos!");
                System.out.println();
                return;
            }

            System.out.println("┌─────────────────────────────────────────────────────────────────────────────────────┐");
            System.out.println("│                           ATIVOS DA CARTEIRA                                         │");
            System.out.println("├─────┬──────────┬──────────────┬────────────┬────────────┬──────────────┬───────────┤");
            System.out.println("│ #   │ Código   │ Nome         │ Qtd        │ Preço Méd. │ Preço Atual  │ Rentab. % │");
            System.out.println("├─────┼──────────┼──────────────┼────────────┼────────────┼──────────────┼───────────┤");

            BigDecimal valorTotalInvestido = BigDecimal.ZERO;
            BigDecimal valorTotalAtual = BigDecimal.ZERO;
            BigDecimal rentabilidadeTotal = BigDecimal.ZERO;

            for (int i = 0; i < ativos.size(); i++) {
                com.invest.model.Ativo ativo = ativos.get(i);
                
                // Calcula rentabilidade do ativo
                com.invest.dto.RentabilidadeResponse rentabilidade = null;
                try {
                    rentabilidade = rentabilidadeService.calcularRentabilidadeAtivo(ativo.getId());
                } catch (Exception e) {
                    System.err.println("Erro ao calcular rentabilidade do ativo " + ativo.getCodigo() + ": " + e.getMessage());
                }

                String codigo = ativo.getCodigo() != null ? ativo.getCodigo() : "N/A";
                String nome = ativo.getNome() != null ? (ativo.getNome().length() > 12 ? ativo.getNome().substring(0, 9) + "..." : ativo.getNome()) : "N/A";
                String quantidade = formatarQuantidade(ativo.getQuantidade());
                String precoMedio = formatarValor(ativo.getPrecoCompra());
                String precoAtual = formatarValor(ativo.getPrecoAtual());
                
                String rentabilidadeStr = "N/A";
                if (rentabilidade != null && rentabilidade.getRentabilidadePercentualBruta() != null) {
                    BigDecimal rent = rentabilidade.getRentabilidadePercentualBruta();
                    rentabilidadeStr = formatarPercentual(rent);
                    if (rent.compareTo(BigDecimal.ZERO) > 0) {
                        rentabilidadeStr = "+" + rentabilidadeStr;
                    }
                }

                System.out.printf("│ %-3d │ %-8s │ %-12s │ %-10s │ %-10s │ %-12s │ %-9s │%n", 
                    (i + 1), codigo, nome, quantidade, precoMedio, precoAtual, rentabilidadeStr);
                
                // Acumula totais
                if (rentabilidade != null) {
                    if (rentabilidade.getValorTotalInvestido() != null) {
                        valorTotalInvestido = valorTotalInvestido.add(rentabilidade.getValorTotalInvestido());
                    }
                    if (rentabilidade.getValorAtualMercado() != null) {
                        valorTotalAtual = valorTotalAtual.add(rentabilidade.getValorAtualMercado());
                    }
                }
            }

            System.out.println("└─────┴──────────┴──────────────┴────────────┴────────────┴──────────────┴───────────┘");
            System.out.println();

            // Mostra resumo detalhado de cada ativo
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("DETALHES DE RENTABILIDADE POR ATIVO:");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();

            for (int i = 0; i < ativos.size(); i++) {
                com.invest.model.Ativo ativo = ativos.get(i);
                
                com.invest.dto.RentabilidadeResponse rentabilidade = null;
                try {
                    rentabilidade = rentabilidadeService.calcularRentabilidadeAtivo(ativo.getId());
                } catch (Exception e) {
                    continue;
                }

                if (rentabilidade == null) continue;

                System.out.println((i + 1) + ". " + ativo.getCodigo() + " - " + ativo.getNome());
                System.out.println("   Tipo: " + ativo.getTipo().getDescricao());
                System.out.println("   Quantidade: " + formatarQuantidade(ativo.getQuantidade()));
                System.out.println("   Preço Médio de Compra: R$ " + formatarValor(ativo.getPrecoCompra()));
                System.out.println("   Preço Atual: R$ " + formatarValor(ativo.getPrecoAtual()));
                
                if (rentabilidade.getValorTotalInvestido() != null) {
                    System.out.println("   Valor Total Investido: R$ " + formatarValor(rentabilidade.getValorTotalInvestido()));
                }
                if (rentabilidade.getValorAtualMercado() != null) {
                    System.out.println("   Valor Atual de Mercado: R$ " + formatarValor(rentabilidade.getValorAtualMercado()));
                }
                if (rentabilidade.getRentabilidadeBruta() != null) {
                    String sinal = rentabilidade.getRentabilidadeBruta().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                    System.out.println("   Rentabilidade Bruta: R$ " + sinal + formatarValor(rentabilidade.getRentabilidadeBruta()));
                }
                if (rentabilidade.getRentabilidadePercentualBruta() != null) {
                    String sinal = rentabilidade.getRentabilidadePercentualBruta().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                    System.out.println("   Rentabilidade Percentual: " + sinal + formatarPercentual(rentabilidade.getRentabilidadePercentualBruta()) + "%");
                }
                if (rentabilidade.getVariacaoPercentual() != null) {
                    String sinal = rentabilidade.getVariacaoPercentual().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                    System.out.println("   Variação Preço (%): " + sinal + formatarPercentual(rentabilidade.getVariacaoPercentual()) + "%");
                }
                
                System.out.println("   Última Atualização: " + formatarData(ativo.getDataAtualizacao()));
                System.out.println();
            }

            // Mostra resumo geral
            rentabilidadeTotal = valorTotalAtual.subtract(valorTotalInvestido);
            BigDecimal rentabilidadePercentualTotal = BigDecimal.ZERO;
            if (valorTotalInvestido.compareTo(BigDecimal.ZERO) > 0) {
                rentabilidadePercentualTotal = rentabilidadeTotal
                    .divide(valorTotalInvestido, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }

            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("RESUMO GERAL:");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("Total Investido: R$ " + formatarValor(valorTotalInvestido));
            System.out.println("Valor Atual Total: R$ " + formatarValor(valorTotalAtual));
            String sinalTotal = rentabilidadeTotal.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            System.out.println("Rentabilidade Total: R$ " + sinalTotal + formatarValor(rentabilidadeTotal));
            System.out.println("Rentabilidade % Total: " + sinalTotal + formatarPercentual(rentabilidadePercentualTotal) + "%");
            System.out.println();

            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();

        } catch (Exception e) {
            System.out.println("Erro ao carregar ativos: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();
        }
    }

    /**
     * Mostra transações de uma carteira
     */
    private void mostrarTransacoesCarteira(Carteira carteira) {
        System.out.println("TRANSAÇÕES - " + carteira.getNome());
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        try {
            List<com.invest.model.Transacao> transacoes = transacaoService.getTransacoesByCarteira(carteira.getId());

            if (transacoes.isEmpty()) {
                System.out.println("Esta carteira não possui transações.");
                System.out.println();
                return;
            }

            System.out.println("Transações da carteira:");
            System.out.println();

            for (int i = 0; i < transacoes.size(); i++) {
                com.invest.model.Transacao transacao = transacoes.get(i);
                System.out.println((i + 1) + ". " + transacao.getTipoTransacao().getDescricao() + " - " + transacao.getCodigoAtivo());
                System.out.println("   Ativo: " + transacao.getNomeAtivo());
                System.out.println("   Quantidade: " + formatarQuantidade(transacao.getQuantidade()));
                System.out.println("   Preço Unitário: R$ " + formatarValor(transacao.getPrecoUnitario()));
                System.out.println("   Valor Total: R$ " + formatarValor(transacao.getValorTotal()));
                System.out.println("   Data: " + formatarData(transacao.getDataTransacao()));
                if (transacao.getObservacoes() != null && !transacao.getObservacoes().isEmpty()) {
                    System.out.println("   Observações: " + transacao.getObservacoes());
                }
                System.out.println();
            }

            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();

        } catch (Exception e) {
            System.out.println("Erro ao carregar transações: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Mostra configurações
     */
    private void mostrarConfiguracoes() {
        System.out.println(" CONFIGURAÇÕES");
        System.out.println("═════════════════");
        System.out.println();
        System.out.println("1. Alterar Senha");
        System.out.println("2. Dados Pessoais");
        System.out.println("0. Voltar");
        System.out.println();
        System.out.print("Opção: ");

        int opcao = lerInteiro();
        System.out.println();

        switch (opcao) {
            case 1:
                alterarSenha();
                break;
            case 2:
                verDadosPessoaisEValorInvestido();
                break;
            case 0:
                return;
            default:
                System.out.println("Opção inválida!");
                System.out.println();
        }
    }

    /**
     * Altera senha do investidor
     */
    private void alterarSenha() {
        System.out.println("ALTERAR SENHA");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        
        System.out.print("Nova senha (mínimo 4 caracteres): ");
        String novaSenha = lerSenha();
        
        if (novaSenha.isEmpty()) {
            System.out.println();
            System.out.println("Senha não pode ser vazia!");
            System.out.println();
            System.out.println();
            return;
        }
        
        if (novaSenha.length() < 4) {
            System.out.println();
            System.out.println("Senha deve ter no mínimo 4 caracteres!");
            System.out.println();
            System.out.println();
            return;
        }
        
        System.out.print("Confirme a nova senha: ");
        String confirmacaoSenha = lerSenha();
        
        if (!novaSenha.equals(confirmacaoSenha)) {
            System.out.println();
            System.out.println("As senhas não coincidem! Tente novamente.");
            System.out.println();
            System.out.println();
            return;
        }
        
        try {
            // Faz hash da nova senha antes de atualizar
            investidorLogado.setSenha(novaSenha);
            investidorService.updateInvestidor(investidorLogado.getId(), investidorLogado);
            
            // Reautentica com a nova senha para atualizar o token JWT
            AuthService.AuthResult resultado = authService.authenticate(investidorLogado.getEmail(), novaSenha);
            if (resultado.isSucesso()) {
                jwtToken = resultado.getToken();
            }
            
            System.out.println();
            System.out.println("✅ Senha alterada com sucesso!");
            System.out.println();
        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao alterar senha: " + e.getMessage());
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Mostra dados pessoais do investidor e valor total investido
     */
    private void verDadosPessoaisEValorInvestido() {
        System.out.println(" DADOS PESSOAIS E VALOR TOTAL INVESTIDO");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        
        try {
            // Recarrega o investidor do banco para garantir dados atualizados
            investidorLogado = investidorService.getInvestidorById(investidorLogado.getId());
            
            // Exibe dados pessoais
            System.out.println("DADOS PESSOAIS:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Nome: " + investidorLogado.getNome());
            System.out.println("Email: " + investidorLogado.getEmail());
            System.out.println("ID: " + investidorLogado.getId());
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            if (investidorLogado.getDataCriacao() != null) {
                System.out.println("Data de Criação: " + investidorLogado.getDataCriacao().format(formatter));
            }
            if (investidorLogado.getDataAtualizacao() != null) {
                System.out.println("Última Atualização: " + investidorLogado.getDataAtualizacao().format(formatter));
        }

            System.out.println();
            
            // Calcula valor total investido em todas as carteiras
            List<Carteira> carteiras = carteiraService.getCarteirasByInvestidor(investidorLogado.getId());
            BigDecimal valorTotalInvestido = BigDecimal.ZERO;
            BigDecimal valorAtualMercado = BigDecimal.ZERO;
            int totalCarteiras = carteiras.size();
            
            System.out.println("INVESTIMENTOS:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Total de Carteiras: " + totalCarteiras);
            System.out.println();
            
            if (carteiras.isEmpty()) {
                System.out.println("Nenhuma carteira cadastrada ainda.");
            } else {
                System.out.println("Resumo por Carteira:");
                System.out.println();
                
                for (Carteira carteira : carteiras) {
                    try {
                        com.invest.dto.CarteiraRentabilidadeResponse rentabilidade = 
                            rentabilidadeService.calcularRentabilidadeCarteira(carteira.getId());
                        
                        BigDecimal valorInvestidoCarteira = rentabilidade.getValorTotalInvestido() != null 
                            ? rentabilidade.getValorTotalInvestido() 
                            : BigDecimal.ZERO;
                        BigDecimal valorMercadoCarteira = rentabilidade.getValorAtualMercado() != null 
                            ? rentabilidade.getValorAtualMercado() 
                            : BigDecimal.ZERO;
                        
                        valorTotalInvestido = valorTotalInvestido.add(valorInvestidoCarteira);
                        valorAtualMercado = valorAtualMercado.add(valorMercadoCarteira);
                        
                        System.out.println("  • " + carteira.getNome() + ":");
                        
                        // Busca e exibe os nomes das ações
                        List<com.invest.model.Ativo> ativos = ativoRepository.findByCarteira(carteira);
                        if (ativos != null && !ativos.isEmpty()) {
                            System.out.print("    Ações: ");
                            List<String> nomesAcoes = new ArrayList<>();
                            for (com.invest.model.Ativo ativo : ativos) {
                                if (ativo.getNome() != null && !ativo.getNome().trim().isEmpty()) {
                                    nomesAcoes.add(ativo.getNome());
                                }
                            }
                            if (!nomesAcoes.isEmpty()) {
                                System.out.println(String.join(", ", nomesAcoes));
                            } else {
                                System.out.println("Nenhuma ação cadastrada");
                            }
                        } else {
                            System.out.println("    Ações: Nenhuma ação cadastrada");
                        }
                        
                        System.out.println("    Valor Investido: R$ " + formatarValor(valorInvestidoCarteira));
                        System.out.println("    Valor Atual: R$ " + formatarValor(valorMercadoCarteira));
                        System.out.println();
        } catch (Exception e) {
                        System.out.println("  • " + carteira.getNome() + ": Erro ao calcular rentabilidade");
                        System.out.println();
                    }
                }
                
                System.out.println("TOTAL GERAL:");
                System.out.println("───────────────────────────────────────────────────────────");
                System.out.println("Valor Total Investido: R$ " + formatarValor(valorTotalInvestido));
                System.out.println("Valor Atual no Mercado: R$ " + formatarValor(valorAtualMercado));
                
                if (valorTotalInvestido.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal rentabilidadeTotal = valorAtualMercado.subtract(valorTotalInvestido);
                    BigDecimal percentualRentabilidade = rentabilidadeTotal
                        .divide(valorTotalInvestido, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    
                    System.out.println("Rentabilidade: R$ " + formatarValor(rentabilidadeTotal) + 
                                     " (" + formatarValor(percentualRentabilidade) + "%)");
                }
            }
            
        } catch (Exception e) {
            System.out.println("Erro ao buscar dados: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
        System.out.println("Pressione Enter para continuar...");
        scanner.nextLine();
        System.out.println();
    }

    /**
     * Edita uma carteira
     */
    private void editarCarteira(Carteira carteira) {
        System.out.println(" EDITAR CARTEIRA - " + carteira.getNome());
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("1. Alterar Nome");
        System.out.println("2. 📄 Alterar Descrição");
        System.out.println("3. Alterar Objetivo");
        System.out.println("4.  Alterar Perfil de Risco");
        System.out.println("5. Alterar Valor da Carteira");
        System.out.println("0. Voltar");
        System.out.println();
        System.out.print("Opção: ");

        int opcao = lerInteiro();
        System.out.println();

        try {
            com.invest.dto.CarteiraRequest request = new com.invest.dto.CarteiraRequest();
            request.setNome(carteira.getNome());
            request.setDescricao(carteira.getDescricao());
            request.setObjetivo(carteira.getObjetivo());
            request.setPerfilRisco(carteira.getPerfilRisco());
            request.setValorInicial(carteira.getValorInicial());

            switch (opcao) {
                case 1:
                    System.out.print("Novo nome (ou 0 para cancelar): ");
                    String novoNome = scanner.nextLine().trim();
                    if (novoNome.equals("0")) {
                        return;
                    }
                    request.setNome(novoNome);
                    break;
                case 2:
                    System.out.print("Nova descrição (ou 0 para cancelar): ");
                    String novaDescricao = scanner.nextLine().trim();
                    if (novaDescricao.equals("0")) {
                        return;
                    }
                    request.setDescricao(novaDescricao);
                    break;
                case 3:
                    System.out.println("Novo objetivo:");
                    System.out.println("1. Aposentadoria 2. Reserva de Emergência 3. Valorização Rápida 4. Renda Passiva 5. Educação 6. Casa Própria 7. Viagem 8. Outros");
                    System.out.println("0. Voltar");
                    System.out.print("Opção: ");
                    int objetivoOpcao = lerInteiro();
                    if (objetivoOpcao == 0) {
                        return;
                    }
                    request.setObjetivo(obterObjetivoCarteira(objetivoOpcao));
                    break;
                case 4:
                    System.out.println("Novo perfil de risco:");
                    System.out.println("1. Baixo Risco 2. Moderado Risco 3. Alto Risco");
                    System.out.println("0. Voltar");
                    System.out.print("Opção: ");
                    int perfilOpcao = lerInteiro();
                    if (perfilOpcao == 0) {
                        return;
                    }
                    request.setPerfilRisco(obterPerfilRisco(perfilOpcao));
                    break;
                case 5:
                    System.out.print("Novo valor da carteira (R$) ou 0 para cancelar: ");
                    BigDecimal novoValor = lerDecimal();
                    if (novoValor.compareTo(BigDecimal.ZERO) < 0) {
                        System.out.println("Valor não pode ser negativo!");
                        return;
                    }
                    if (novoValor.compareTo(BigDecimal.ZERO) == 0) {
                        System.out.println("Operação cancelada.");
                        return;
                    }
                    
                    // Calcula o valor total investido (compras + taxas)
                    BigDecimal valorTotalCompras = transacaoRepository.calcularValorTotalCompras(carteira);
                    BigDecimal valorTotalTaxas = transacaoRepository.findByCarteira(carteira).stream()
                            .filter(t -> t.getTipoTransacao() == com.invest.model.TipoTransacao.COMPRA)
                            .map(t -> t.getTaxasCorretagem() != null ? t.getTaxasCorretagem() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal valorTotalInvestido = valorTotalCompras.add(valorTotalTaxas);
                    
                    // Valida se o novo valor não é menor que o total investido
                    if (novoValor.compareTo(valorTotalInvestido) < 0) {
                        System.out.println();
                        System.out.println();
                        System.out.println("⚠️ VALOR INVÁLIDO!");
                        System.out.println("═══════════════════════════════════════════════════════════════");
                        System.out.println();
                        System.out.println("O valor da carteira não pode ser menor que o total investido.");
                        System.out.println();
                        System.out.println("Valor total investido (compras + taxas): R$ " + formatarValor(valorTotalInvestido));
                        System.out.println("Valor informado: R$ " + formatarValor(novoValor));
                        System.out.println("Valor mínimo permitido: R$ " + formatarValor(valorTotalInvestido));
                        System.out.println();
                        System.out.println();
                        return;
                    }
                    
                    request.setValorInicial(novoValor);
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Opção inválida!");
                    return;
            }

            carteiraService.updateCarteira(carteira.getId(), request);
            System.out.println();
            System.out.println("✅ Carteira atualizada com sucesso!");
            System.out.println();
            System.out.println();

        } catch (Exception e) {
            System.out.println("Erro ao atualizar carteira: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Mostra análise completa de inflação e valores deflacionados da carteira
     */
    private void mostrarAnaliseInflacao(Carteira carteira) {
        analiseCompletaCarteira(carteira);
    }

    private void analiseCompletaCarteira(Carteira carteira) {
        System.out.println("ANÁLISE COMPLETA DE INFLAÇÃO - " + carteira.getNome());
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        
        try {
            // Busca todas as transações
            List<com.invest.model.Transacao> transacoes = transacaoRepository.findByCarteira(carteira);
            
            if (transacoes.isEmpty()) {
                System.out.println("Nenhuma transação registrada nesta carteira.");
                System.out.println();
                System.out.println("Pressione Enter para continuar...");
                scanner.nextLine();
                return;
            }
            
            // Calcula valores totais
            BigDecimal valorTotalCompras = transacaoRepository.calcularValorTotalCompras(carteira);
            BigDecimal valorTotalVendas = transacaoRepository.calcularValorTotalVendas(carteira);
            BigDecimal valorAtualMercado = carteira.getValorAtual() != null ? carteira.getValorAtual() : BigDecimal.ZERO;
            
            // Data da primeira transação e data atual
            java.time.LocalDate dataPrimeiraTransacao = transacoes.stream()
                .map(t -> t.getDataTransacao().toLocalDate())
                .min(java.util.Comparator.naturalOrder())
                .orElse(java.time.LocalDate.now());
            
            java.time.LocalDate dataAtual = java.time.LocalDate.now();
            
            // Calcula inflação acumulada
            BigDecimal inflacaoAcumulada = inflacaoService.calcularInflacaoAcumulada(dataPrimeiraTransacao, dataAtual);
            
            // Valor investido deflacionado
            BigDecimal valorInvestidoDeflacionado = inflacaoService.calcularValorDeflacionado(
                valorTotalCompras, dataAtual, dataPrimeiraTransacao);
            
            // Valor atual deflacionado
            BigDecimal valorAtualDeflacionado = inflacaoService.calcularValorDeflacionado(
                valorAtualMercado, dataAtual, dataPrimeiraTransacao);
            
            // Ganho real
            BigDecimal ganhoReal = inflacaoService.calcularGanhoReal(
                valorTotalCompras, valorAtualMercado, dataPrimeiraTransacao, dataAtual);
            
            System.out.println("PERÍODO DE ANÁLISE:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Data da primeira transação: " + formatarData(dataPrimeiraTransacao.atStartOfDay()));
            System.out.println("Data atual: " + formatarData(dataAtual.atStartOfDay()));
            System.out.println();
            
            System.out.println("VALORES NOMINAIS:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Total investido (compras): R$ " + formatarValor(valorTotalCompras));
            System.out.println("Total vendido: R$ " + formatarValor(valorTotalVendas));
            System.out.println("Valor atual de mercado: R$ " + formatarValor(valorAtualMercado));
            System.out.println();
            
            System.out.println("INFLAÇÃO:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Inflação acumulada no período: " + formatarPercentual(inflacaoAcumulada.multiply(new BigDecimal("100"))));
            System.out.println();
            
            System.out.println("VALORES DEFLACIONADOS (poder de compra na data inicial):");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Valor investido deflacionado: R$ " + formatarValor(valorInvestidoDeflacionado));
            System.out.println("Valor atual deflacionado: R$ " + formatarValor(valorAtualDeflacionado));
            System.out.println();
            
            System.out.println("GANHO REAL:");
            System.out.println("───────────────────────────────────────────────────────────");
            BigDecimal ganhoNominal = valorAtualMercado.subtract(valorTotalCompras);
            BigDecimal ganhoNominalPercentual = BigDecimal.ZERO;
            if (valorTotalCompras.compareTo(BigDecimal.ZERO) > 0) {
                ganhoNominalPercentual = ganhoNominal.divide(valorTotalCompras, 4, java.math.RoundingMode.HALF_UP)
                                                     .multiply(new BigDecimal("100"));
            }
            
            System.out.println("Ganho nominal: R$ " + formatarValor(ganhoNominal) + 
                             " (" + formatarPercentual(ganhoNominalPercentual) + "%)");
            System.out.println("Ganho real: " + formatarPercentual(ganhoReal.multiply(new BigDecimal("100"))));
            System.out.println();
            
            if (ganhoReal.compareTo(BigDecimal.ZERO) > 0) {
                System.out.println("✅ A carteira teve ganho real positivo!");
            } else if (ganhoReal.compareTo(BigDecimal.ZERO) < 0) {
                System.out.println("⚠️ A carteira teve perda real (ganho não superou a inflação)");
            } else {
                System.out.println("➡️ A carteira manteve o poder de compra");
            }
            System.out.println();
            System.out.println();
            
        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao realizar análise: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
        
        System.out.println("Pressione Enter para continuar...");
        scanner.nextLine();
        System.out.println();
    }

    /**
     * Mostra o histórico completo da carteira com todas as alterações de valores
     */
    private void mostrarHistoricoCarteira(Carteira carteira) {
        System.out.println("HISTÓRICO DA CARTEIRA - " + carteira.getNome());
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        
        try {
            // Recarrega a carteira para garantir dados atualizados
            carteira = carteiraService.getCarteiraById(carteira.getId());
            
            // Busca todas as transações ordenadas por data
            List<com.invest.model.Transacao> transacoes = transacaoRepository.findByCarteira(carteira);
            transacoes.sort((t1, t2) -> {
                if (t1.getDataTransacao() == null && t2.getDataTransacao() == null) return 0;
                if (t1.getDataTransacao() == null) return 1;
                if (t2.getDataTransacao() == null) return -1;
                return t1.getDataTransacao().compareTo(t2.getDataTransacao());
            });
            
            // Calcula valores acumulados ao longo do tempo
            BigDecimal valorTotalInvestido = BigDecimal.ZERO;
            BigDecimal valorTotalVendas = BigDecimal.ZERO;
            BigDecimal valorTotalTaxas = BigDecimal.ZERO;
            BigDecimal taxasCompras = BigDecimal.ZERO;
            BigDecimal taxasVendas = BigDecimal.ZERO;
            
            System.out.println("CRIAÇÃO DA CARTEIRA:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Data: " + formatarData(carteira.getDataCriacao()));
            System.out.println("Valor inicial da carteira: R$ " + formatarValor(carteira.getValorInicial()));
            System.out.println();
            System.out.println();
            
            if (transacoes.isEmpty()) {
                System.out.println("Nenhuma transação registrada ainda.");
                System.out.println();
            } else {
                System.out.println("TRANSAÇÕES E ALTERAÇÕES:");
                System.out.println("───────────────────────────────────────────────────────────");
                System.out.println();
                
                for (com.invest.model.Transacao transacao : transacoes) {
                    System.out.println("📅 " + formatarData(transacao.getDataTransacao()));
                    System.out.println("   Tipo: " + transacao.getTipoTransacao().getDescricao());
                    System.out.println("   Ativo: " + transacao.getCodigoAtivo() + " (" + transacao.getNomeAtivo() + ")");
                    System.out.println("   Quantidade: " + formatarQuantidade(transacao.getQuantidade()));
                    System.out.println("   Preço unitário: R$ " + formatarValor(transacao.getPrecoUnitario()));
                    System.out.println("   Valor total: R$ " + formatarValor(transacao.getValorTotal()));
                    
                    if (transacao.getTipoTransacao() == com.invest.model.TipoTransacao.COMPRA) {
                        valorTotalInvestido = valorTotalInvestido.add(transacao.getValorTotal());
                        if (transacao.getTaxasCorretagem() != null) {
                            BigDecimal taxa = transacao.getTaxasCorretagem();
                            valorTotalTaxas = valorTotalTaxas.add(taxa);
                            taxasCompras = taxasCompras.add(taxa);
                            System.out.println("   Taxas: R$ " + formatarValor(taxa));
                        }
                        System.out.println("   → Compra realizada");
                    } else if (transacao.getTipoTransacao() == com.invest.model.TipoTransacao.VENDA) {
                        valorTotalVendas = valorTotalVendas.add(transacao.getValorTotal());
                        if (transacao.getTaxasCorretagem() != null) {
                            BigDecimal taxa = transacao.getTaxasCorretagem();
                            valorTotalTaxas = valorTotalTaxas.add(taxa);
                            taxasVendas = taxasVendas.add(taxa);
                            System.out.println("   Taxas: R$ " + formatarValor(taxa));
                        }
                        System.out.println("   → Venda realizada");
                    }
                    
                    if (transacao.getObservacoes() != null && !transacao.getObservacoes().trim().isEmpty()) {
                        System.out.println("   Observações: " + transacao.getObservacoes());
                    }
                    
                    System.out.println();
                }
            }
            
            // Mostra alterações no valor da carteira (quando foi editado)
            if (carteira.getDataAtualizacao() != null && 
                carteira.getDataAtualizacao().isAfter(carteira.getDataCriacao())) {
                System.out.println();
                System.out.println("ALTERAÇÕES NO VALOR DA CARTEIRA:");
                System.out.println("───────────────────────────────────────────────────────────");
                System.out.println("Data da última alteração: " + formatarData(carteira.getDataAtualizacao()));
                System.out.println("Valor atual da carteira: R$ " + formatarValor(carteira.getValorInicial()));
                System.out.println();
            }
            
            // Resumo final
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("RESUMO DO HISTÓRICO:");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("Valor inicial da carteira: R$ " + formatarValor(carteira.getValorInicial()));
            System.out.println("Total de compras: R$ " + formatarValor(valorTotalInvestido));
            System.out.println("Total de vendas: R$ " + formatarValor(valorTotalVendas));
            System.out.println("Total de taxas: R$ " + formatarValor(valorTotalTaxas));
            System.out.println();
            
            // Cálculo correto do valor líquido investido:
            // - Compras aumentam o investimento (incluindo taxas)
            // - Vendas reduzem o investimento (descontando taxas, pois você recebe menos)
            // Fórmula: (compras + taxas de compras) - (vendas - taxas de vendas)
            BigDecimal valorInvestidoComTaxas = valorTotalInvestido.add(taxasCompras);
            BigDecimal valorRecebidoVendas = valorTotalVendas.subtract(taxasVendas);
            BigDecimal valorLiquidoInvestido = valorInvestidoComTaxas.subtract(valorRecebidoVendas);
            System.out.println("Valor líquido investido: R$ " + formatarValor(valorLiquidoInvestido));
            System.out.println("Valor atual de mercado: R$ " + formatarValor(carteira.getValorAtual()));
            System.out.println();
            
            if (carteira.getValorAtual() != null && valorLiquidoInvestido.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rentabilidade = carteira.getValorAtual().subtract(valorLiquidoInvestido);
                BigDecimal percentual = rentabilidade
                    .divide(valorLiquidoInvestido, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                String sinal = rentabilidade.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                System.out.println("Rentabilidade: R$ " + sinal + formatarValor(rentabilidade) + 
                                 " (" + sinal + formatarPercentual(percentual) + "%)");
            }
            
            System.out.println();
            System.out.println("Total de transações: " + transacoes.size());
            System.out.println();
            System.out.println();
            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();
            System.out.println();
            
        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao exibir histórico: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
            System.out.println("Pressione Enter para continuar...");
            scanner.nextLine();
        }
    }

    // Métodos auxiliares

    private int lerInteiro() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Digite um número válido: ");
            }
        }
    }

    private BigDecimal lerDecimal() {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    return BigDecimal.ZERO;
                }
                return new BigDecimal(input.replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.print("Digite um valor válido: ");
            }
        }
    }

    private com.invest.model.ObjetivoCarteira obterObjetivoCarteira(int opcao) {
        switch (opcao) {
            case 1: return com.invest.model.ObjetivoCarteira.APOSENTADORIA;
            case 2: return com.invest.model.ObjetivoCarteira.RESERVA_EMERGENCIAL;
            case 3: return com.invest.model.ObjetivoCarteira.VALORIZACAO_RAPIDA;
            case 4: return com.invest.model.ObjetivoCarteira.RENDA_PASSIVA;
            case 5: return com.invest.model.ObjetivoCarteira.EDUCACAO;
            case 6: return com.invest.model.ObjetivoCarteira.CASA_PROPIA;
            case 7: return com.invest.model.ObjetivoCarteira.VIAGEM;
            case 8: return com.invest.model.ObjetivoCarteira.OUTROS;
            default: return com.invest.model.ObjetivoCarteira.APOSENTADORIA;
        }
    }

    private com.invest.model.PerfilRisco obterPerfilRisco(int opcao) {
        switch (opcao) {
            case 1: return com.invest.model.PerfilRisco.BAIXO_RISCO;
            case 2: return com.invest.model.PerfilRisco.MODERADO_RISCO;
            case 3: return com.invest.model.PerfilRisco.ALTO_RISCO;
            default: return com.invest.model.PerfilRisco.BAIXO_RISCO;
        }
    }

    private com.invest.model.PrazoCarteira obterPrazoCarteira(int opcao) {
        switch (opcao) {
            case 1: return com.invest.model.PrazoCarteira.CURTO_PRAZO;
            case 2: return com.invest.model.PrazoCarteira.MEDIO_PRAZO;
            case 3: return com.invest.model.PrazoCarteira.LONGO_PRAZO;
            default: return com.invest.model.PrazoCarteira.MEDIO_PRAZO;
        }
    }

    private com.invest.model.TipoTransacao obterTipoTransacao(int opcao) {
        switch (opcao) {
            case 1: return com.invest.model.TipoTransacao.COMPRA;
            case 2: return com.invest.model.TipoTransacao.VENDA;
            default: return com.invest.model.TipoTransacao.COMPRA;
        }
    }

    private com.invest.model.TipoAtivo obterTipoAtivo(int opcao) {
        switch (opcao) {
            case 1: return com.invest.model.TipoAtivo.ACAO;
            case 2: return com.invest.model.TipoAtivo.FII;
            case 3: return com.invest.model.TipoAtivo.ETF;
            case 4: return com.invest.model.TipoAtivo.CDB;
            case 5: return com.invest.model.TipoAtivo.LCI;
            case 6: return com.invest.model.TipoAtivo.TESOURO;
            case 7: return com.invest.model.TipoAtivo.CRIPTOMOEDA;
            default: return com.invest.model.TipoAtivo.ACAO;
        }
    }

    private String formatarValor(BigDecimal valor) {
        if (valor == null) return "0,00";
        return String.format("%.2f", valor).replace(".", ",");
    }

    private String formatarPercentual(BigDecimal percentual) {
        if (percentual == null) return "0,00";
        return String.format("%.2f", percentual).replace(".", ",");
    }

    private String formatarQuantidade(BigDecimal quantidade) {
        if (quantidade == null) return "0";
        return String.format("%.4f", quantidade).replace(".", ",");
    }

    private String formatarData(LocalDateTime data) {
        if (data == null) return "N/A";
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Calcula as taxas de corretagem e operacionais baseadas no mercado brasileiro atual
     * Considera:
     * - Corretagem: R$ 0,00 (muitas corretoras são gratuitas hoje)
     * - Taxas B3 (obrigatórias): 0,0325% do valor da operação
     *   * Emolumentos: 0,005%
     *   * Liquidação: 0,0275%
     */
    private BigDecimal calcularTaxasCorretagem(BigDecimal valorTotal) {
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Taxas da B3 (obrigatórias): Emolumentos (0,005%) + Liquidação (0,0275%) = 0,0325%
        BigDecimal taxaB3Percentual = new BigDecimal("0.000325"); // 0,0325%
        BigDecimal taxasB3 = valorTotal.multiply(taxaB3Percentual);
        
        // Corretagem: R$ 0,00 (muitas corretoras são gratuitas atualmente)
        // Se quiser simular corretoras que cobram, pode adicionar:
        // BigDecimal corretagem = new BigDecimal("4.90"); // Ex: XP Investimentos
        BigDecimal corretagem = BigDecimal.ZERO;
        
        // Total de taxas = Taxas B3 + Corretagem
        BigDecimal taxas = taxasB3.add(corretagem);
        
        // Taxa mínima: R$ 0,01 (para garantir que operações pequenas tenham algum custo mínimo)
        BigDecimal taxaMinima = new BigDecimal("0.01");
        if (taxas.compareTo(taxaMinima) < 0) {
            taxas = taxaMinima;
        }
        
        // Não há taxa máxima (as taxas B3 são proporcionais ao valor)
        
        // Arredonda para 2 casas decimais
        return taxas.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Lê a senha do terminal mostrando asteriscos em vez dos caracteres
     * @return A senha digitada pelo usuário (valor real, não os asteriscos)
     */
    private String lerSenha() {
        // Tenta usar System.console() primeiro (funciona em terminais reais)
        // Este é o método mais seguro e funciona perfeitamente em terminais
        Console console = System.console();
        if (console != null) {
            char[] passwordArray = console.readPassword();
            return new String(passwordArray).trim();
        }
        
        // Fallback para IDEs e ambientes onde System.console() retorna null
        // Lê caractere por caractere usando System.in diretamente
        StringBuilder senha = new StringBuilder();
        try {
            // Configura o terminal para modo raw (se possível)
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWindows = os.contains("win");
            
            int caractere;
            System.out.flush(); // Garante que o prompt foi exibido
            
            while (true) {
                caractere = System.in.read();
                
                if (caractere == '\n' || caractere == '\r') {
                    break; 
                } else if (caractere == 8 || caractere == 127 || (isWindows && caractere == 224 && System.in.read() == 75)) {
                    if (senha.length() > 0) {
                        senha.deleteCharAt(senha.length() - 1);
                        System.out.print("\b \b"); 
                        System.out.flush();
                    }
                } else if (caractere >= 32 && caractere < 127) {
                    senha.append((char) caractere);
                    System.out.print('*');
                    System.out.flush();
                }
            }
            System.out.println();
        } catch (IOException e) {
            System.out.println();
            System.out.println("(Aviso: não foi possível ocultar a senha - usando modo normal)");
            return scanner.nextLine().trim();
        }
        
        return senha.toString().trim();
    }

    /**
     * Mostra o relatório de exibição em formato JSON
     */
    private void mostrarRelatorioExibicao() {
        System.out.println("RELATÓRIO DE EXIBIÇÃO (JSON)");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        
        try {
            com.invest.dto.RelatorioExibicaoResponse relatorio = 
                relatorioExibicaoService.gerarRelatorioExibicao(investidorLogado.getId());
            
            // Converte para JSON usando ObjectMapper do Spring Boot (já configurado com JavaTimeModule)
            // Cria uma cópia com indentação habilitada
            com.fasterxml.jackson.databind.ObjectMapper mapper = objectMapper.copy();
            mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            
            String json = mapper.writeValueAsString(relatorio);
            
            System.out.println("JSON do Relatório:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println();
            System.out.println(json);
            System.out.println();
            System.out.println();
            
        } catch (Exception e) {
            System.out.println();
            System.out.println("Erro ao gerar relatório: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
        
        System.out.println("Pressione Enter para continuar...");
        scanner.nextLine();
        System.out.println();
    }
}