import java.util.List;

public class TesteJanelaDeslizante {
    public static void main(String[] args) {
        long timestampInicial = 1700000000000L; // Timestamp base em milissegundos
        long dezMinutosMs = 10 * 60 * 1000L;    // Janela de 10 minutos (600.000 ms)

        JanelaDeslizante janela = new JanelaDeslizante(dezMinutosMs);
        GeradorTransacoes gerador = new GeradorTransacoes(50, timestampInicial);

        System.out.println("=== 1. Inserindo 5 Transações de Smurfing (Conta 42 -> Conta 99) ===");
        List<Transacao> rajadaSmurfing = gerador.injetarRajadaSmurfing(42, 99);

        for (Transacao tx : rajadaSmurfing) {
            janela.adicionarTransacao(tx);
            
            // Avalia a atividade recente da conta 42 nos últimos 10 minutos a cada inserção
            MetricasJanela metricas = janela.avaliarAtividadeConta(42, dezMinutosMs, tx.getTimestamp());
            System.out.println("Transação Inserida: " + tx);
            System.out.println("  -> " + metricas);
        }

        System.out.println("\nQuantidade de transações ativas na janela: " + janela.getQuantidadeTransacoesAtivas());

        System.out.println("\n=== 2. Simulando o Avanço do Tempo (15 minutos depois) ===");
        // Cria uma nova transação ocorrendo 15 minutos após a última da rajada para forçar o expurgo
        long tempoAvancado = rajadaSmurfing.get(rajadaSmurfing.size() - 1).getTimestamp() + (15 * 60 * 1000L);
        Transacao transacaoFutura = new Transacao(9999L, 5, 8, 100_00L, tempoAvancado);
        
        janela.adicionarTransacao(transacaoFutura);

        System.out.println("Transação futura inserida: " + transacaoFutura);
        System.out.println("Transações ativas após descarte automático: " + janela.getQuantidadeTransacoesAtivas());
        
        // Reavalia o histórico da conta 42 no novo instante de tempo
        MetricasJanela metricasAposAvanco = janela.avaliarAtividadeConta(42, dezMinutosMs, tempoAvancado);
        System.out.println("Métricas da Conta 42 após o avanço do tempo: " + metricasAposAvanco);
    }
}