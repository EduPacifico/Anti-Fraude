import java.util.ArrayList;
import java.util.List;

public class SuiteTestes {
    private static int totalTestes = 0;
    private static int testesPassaram = 0;
    private static int testesFalharam = 0;
    private static final long JANELA_10_MIN = 10 * 60 * 1000L;

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("             SUITE DE TESTES UNITARIOS E DE INTEGRACAO - MOTOR ANTIFRAUDE                ");
        System.out.println("=========================================================================================\n");

        long inicio = System.currentTimeMillis();

        executar("1. Validacao de Saldo Insuficiente na Treap", SuiteTestes::testarSaldoInsuficiente);
        executar("2. Aprovacao de Transacao Regular e Fluxo Legitimo", SuiteTestes::testarFluxoLegitimo);
        executar("3. Deteccao de Fracionamento (Smurfing) via Janela Deslizante", SuiteTestes::testarDeteccaoSmurfing);
        executar("4. Bloqueio de Anel de Lavagem via Bounded DFS no Grafo", SuiteTestes::testarAnelDeLavagem);
        executar("5. Deteccao de Concentracao de Recursos (Fan-in In-Degree)", SuiteTestes::testarConcentracaoFanIn);
        executar("6. Deteccao de Explosao de Velocidade Temporal (Burst)", SuiteTestes::testarExplosaoVelocidade);
        executar("7. Expurgo Temporal de Arestas e Transacoes Expiradas", SuiteTestes::testarExpurgoTemporal);
        executar("8. Imutabilidade e Integridade Criptografica da Arvore de Merkle", SuiteTestes::testarArvoreMerkle);

        long duracao = System.currentTimeMillis() - inicio;

        System.out.println("\n=========================================================================================");
        System.out.printf(" RESULTADO FINAL: %d Executados | %d Aprovados | %d Falhas | Tempo: %d ms\n", 
                totalTestes, testesPassaram, testesFalharam, duracao);
        System.out.println("=========================================================================================");

        if (testesFalharam > 0) {
            System.exit(1);
        }
    }

    private static void testarSaldoInsuficiente() {
        MotorFraude motor = new MotorFraude(JANELA_10_MIN);
        motor.getTreapContas().obterOuCriar(10, 1_000_00L, 2_000_00L);
        motor.getTreapContas().obterOuCriar(20, 10_000_00L, 2_000_00L);

        Transacao tx = new Transacao(1001L, 10, 20, 1_500_00L, System.currentTimeMillis());
        Transacao processada = motor.processarTransacao(tx);

        afirmar(processada.getStatus() == StatusTransacao.BLOQUEADA, "A transacao deveria ter sido BLOQUEADA");
        afirmar(processada.getScoreFraude() == 100.0, "O score deveria ser 100.0");
        afirmar(processada.getMotivoFraude().contains("SALDO_INSUFICIENTE"), "Motivo deve apontar saldo insuficiente");
    }

    private static void testarFluxoLegitimo() {
        MotorFraude motor = new MotorFraude(JANELA_10_MIN);
        motor.getTreapContas().obterOuCriar(10, 50_000_00L, 2_000_00L);
        motor.getTreapContas().obterOuCriar(20, 10_000_00L, 2_000_00L);

        Transacao tx = new Transacao(1002L, 10, 20, 150_00L, System.currentTimeMillis());
        Transacao processada = motor.processarTransacao(tx);

        afirmar(processada.getStatus() == StatusTransacao.APROVADA, "Transacao regular deve ser APROVADA");
        afirmar(processada.getScoreFraude() < 40.0, "Score deve ser menor que o limiar de suspeita (40.0)");
        afirmar(motor.getTreapContas().buscar(10).getSaldoCentavos() == 49_850_00L, "Saldo da origem deve debitar R$ 150,00");
        afirmar(motor.getTreapContas().buscar(20).getSaldoCentavos() == 10_150_00L, "Saldo do destino deve creditar R$ 150,00");
    }

    private static void testarDeteccaoSmurfing() {
        MotorFraude motor = new MotorFraude(JANELA_10_MIN);
        motor.getTreapContas().obterOuCriar(15, 100_000_00L, 2_000_00L);
        motor.getTreapContas().obterOuCriar(40, 10_000_00L, 2_000_00L);

        long baseTempo = System.currentTimeMillis();
        Transacao ultimaTx = null;

        for (int i = 0; i < 5; i++) {
            Transacao tx = new Transacao(2000L + i, 15, 40, 9_600_00L, baseTempo + (i * 10_000L));
            ultimaTx = motor.processarTransacao(tx);
        }

        afirmar(ultimaTx != null, "A ultima transacao nao deve ser nula");
        afirmar(ultimaTx.getStatus() == StatusTransacao.SUSPEITA || ultimaTx.getStatus() == StatusTransacao.BLOQUEADA,
                "A 5a transacao de smurfing deve ser marcada como SUSPEITA ou BLOQUEADA");
        afirmar(ultimaTx.getMotivoFraude().contains("SMURFING"), "Diagnostico deve acusar SMURFING");
        afirmar(motor.getTreapContas().buscar(15).getScoreRisco() > 0.0, "Score permanente da conta 15 deve subir na Treap");
    }

    private static void testarAnelDeLavagem() {
        MotorFraude motor = new MotorFraude(JANELA_10_MIN);
        motor.getTreapContas().obterOuCriar(20, 100_000_00L, 2_000_00L);
        motor.getTreapContas().obterOuCriar(30, 100_000_00L, 2_000_00L);
        motor.getTreapContas().obterOuCriar(40, 100_000_00L, 2_000_00L);

        long t0 = System.currentTimeMillis();

        Transacao tx1 = motor.processarTransacao(new Transacao(3001L, 20, 30, 50_000_00L, t0));
        afirmar(tx1.getStatus() == StatusTransacao.APROVADA, "Perna 1 deve ser aprovada");

        Transacao tx2 = motor.processarTransacao(new Transacao(3002L, 30, 40, 49_000_00L, t0 + 20_000L));
        afirmar(tx2.getStatus() == StatusTransacao.APROVADA, "Perna 2 deve ser aprovada");

        Transacao tx3 = motor.processarTransacao(new Transacao(3003L, 40, 20, 48_000_00L, t0 + 40_000L));

        afirmar(tx3.getStatus() == StatusTransacao.BLOQUEADA, "Tentativa de fechar ciclo DEVE ser BLOQUEADA");
        afirmar(tx3.getScoreFraude() == 100.0, "Score do fechamento de ciclo deve ser 100.0");
        afirmar(tx3.getMotivoFraude().contains("ANEL_LAVAGEM_CICLO_DETECTADO"), "Motivo deve identificar o anel de lavagem");

        afirmar(motor.getTreapContas().buscar(20).getScoreRisco() >= 85.0, "Conta 20 deve ter score de risco >= 85.0");
        afirmar(motor.getTreapContas().buscar(30).getScoreRisco() >= 85.0, "Conta 30 deve ter score de risco >= 85.0");
        afirmar(motor.getTreapContas().buscar(40).getScoreRisco() >= 85.0, "Conta 40 deve ter score de risco >= 85.0");
    }

    private static void testarConcentracaoFanIn() {
        MotorFraude motor = new MotorFraude(JANELA_10_MIN);
        int[] origens = {10, 15, 20, 25, 30};
        int destino = 80;

        for (int orig : origens) {
            motor.getTreapContas().obterOuCriar(orig, 50_000_00L, 2_000_00L);
        }
        motor.getTreapContas().obterOuCriar(destino, 10_000_00L, 2_000_00L);

        long t0 = System.currentTimeMillis();
        Transacao ultima = null;

        for (int i = 0; i < origens.length; i++) {
            Transacao tx = new Transacao(4000L + i, origens[i], destino, 8_000_00L, t0 + (i * 5_000L));
            ultima = motor.processarTransacao(tx);
        }

        afirmar(ultima != null, "Ultima transacao de Fan-in nao pode ser nula");
        afirmar(motor.getGrafoTransacional().getGrauEntrada(destino) >= 4, "In-Degree da conta 80 deve ser >= 4");
        afirmar(ultima.getMotivoFraude().contains("FAN_IN_CONCENTRACAO_DETECTADA"), "Motivo deve apontar Fan-in");
    }

    private static void testarExplosaoVelocidade() {
        MotorFraude motor = new MotorFraude(JANELA_10_MIN);
        motor.getTreapContas().obterOuCriar(25, 100_000_00L, 2_000_00L);
        motor.getTreapContas().obterOuCriar(50, 10_000_00L, 2_000_00L);

        long t0 = System.currentTimeMillis();
        Transacao flagBurst = null;

        // Dispara 5 transações com 1 segundo de intervalo (rajada rápida)
        for (int i = 0; i < 5; i++) {
            Transacao tx = new Transacao(5000L + i, 25, 50, 7_000_00L, t0 + (i * 1_000L));
            Transacao processada = motor.processarTransacao(tx);
            if (processada.getMotivoFraude().contains("EXPLOSAO_VELOCIDADE_TEMPORAL")) {
                flagBurst = processada;
            }
        }

        afirmar(flagBurst != null, "A rajada de alta cadencia deve acionar o alerta de explosao de velocidade");
    }

    private static void testarExpurgoTemporal() {
        MotorFraude motor = new MotorFraude(JANELA_10_MIN);
        long t0 = 1_000_000_000L;

        Transacao txAntiga = new Transacao(6001L, 10, 20, 500_00L, t0);
        motor.processarTransacao(txAntiga);

        afirmar(motor.getJanelaDeslizante().getQuantidadeTransacoesAtivas() == 1, "Janela deve conter 1 transacao");

        long tFuturo = t0 + (15 * 60 * 1000L);
        Transacao txNova = new Transacao(6002L, 10, 20, 200_00L, tFuturo);
        motor.processarTransacao(txNova);

        afirmar(motor.getJanelaDeslizante().getQuantidadeTransacoesAtivas() == 1, "Apenas a nova transacao deve restar na janela");
        afirmar(motor.getGrafoTransacional().getGrauSaida(10) == 1, "Aresta expirada deve ser removida do grafo");
    }

    private static void testarArvoreMerkle() {
        List<Transacao> loteOriginal = new ArrayList<>();
        loteOriginal.add(new Transacao(7001L, 10, 20, 100_00L, 1000L));
        loteOriginal.add(new Transacao(7002L, 20, 30, 200_00L, 2000L));

        ArvoreMerkle arvore1 = new ArvoreMerkle(loteOriginal);
        ArvoreMerkle arvore2 = new ArvoreMerkle(loteOriginal);

        afirmar(!arvore1.getRaizMerkle().isEmpty(), "Raiz Merkle nao pode ser vazia");
        afirmar(arvore1.getRaizMerkle().equals(arvore2.getRaizMerkle()), "Calculo SHA-256 da raiz deve ser deterministico");

        List<Transacao> loteAdulterado = new ArrayList<>();
        loteAdulterado.add(new Transacao(7001L, 10, 20, 100_01L, 1000L));
        loteAdulterado.add(new Transacao(7002L, 20, 30, 200_00L, 2000L));

        ArvoreMerkle arvoreAdulterada = new ArvoreMerkle(loteAdulterado);
        afirmar(!arvore1.getRaizMerkle().equals(arvoreAdulterada.getRaizMerkle()), 
                "Adulteracao de 1 centavo DEVE alterar completamente a Raiz Merkle");
    }

    private static void executar(String nomeTeste, Runnable teste) {
        totalTestes++;
        System.out.printf("▶ %-75s", nomeTeste);
        try {
            teste.run();
            testesPassaram++;
            System.out.println(" [OK]");
        } catch (AssertionError | Exception e) {
            testesFalharam++;
            System.out.println(" [FALHOU]");
            System.out.println("   └─ Motivo: " + e.getMessage());
        }
    }

    private static void afirmar(boolean condicao, String mensagemErro) {
        if (!condicao) {
            throw new AssertionError(mensagemErro);
        }
    }
}