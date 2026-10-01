// Principal.java
import java.util.List;

public class Principal {
    public static void main(String[] args) {
        long timestampInicial = System.currentTimeMillis();
        GeradorTransacoes gerador = new GeradorTransacoes(100, timestampInicial);

        System.out.println("=== 1. Gerando Transações Legítimas ===");
        for (int i = 0; i < 3; i++) {
            Transacao tx = gerador.gerarTransacaoLegitima();
            System.out.println(tx);
        }

        System.out.println("\n=== 2. Injetando Anel de Fraude (Ciclo: Conta 10 -> 25 -> 80 -> 10) ===");
        // R$ 50.000,00 passando por laranjas e voltando para a origem
        List<Transacao> ciclo = gerador.injetarPadraoCiclo(10, 25, 80, 50_000_00L);
        for (Transacao tx : ciclo) {
            System.out.println(tx);
        }

        System.out.println("\n=== 3. Injetando Rajada de Smurfing (Conta 42 -> Conta 99) ===");
        // 5 transações fracionadas de R$ 9.600,00
        List<Transacao> rajadaSmurfing = gerador.injetarRajadaSmurfing(42, 99);
        for (Transacao tx : rajadaSmurfing) {
            System.out.println(tx);
        }
    }
}