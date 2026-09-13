import java.util.List;

public class TesteGrafoTransacional {
    public static void main(String[] args) {
        GrafoTransacional grafo = new GrafoTransacional();
        GeradorTransacoes gerador = new GeradorTransacoes(50, 1700000000000L);

        System.out.println("=== 1. Montando o Grafo com o Anel de Fraude ===");
        // Gera o ciclo: Conta 10 -> Conta 25 -> Conta 80 -> Conta 10 (R$ 50.000,00)
        List<Transacao> ciclo = gerador.injetarPadraoCiclo(10, 25, 80, 50_000_00L);

        Transacao tx1 = ciclo.get(0); // 10 -> 25
        Transacao tx2 = ciclo.get(1); // 25 -> 80
        Transacao tx3 = ciclo.get(2); // 80 -> 10 (Tentativa de fecho)

        // Adiciona as duas primeiras pernas ao grafo
        grafo.adicionarAresta(tx1);
        System.out.println("Inserida no grafo: " + tx1);

        grafo.adicionarAresta(tx2);
        System.out.println("Inserida no grafo: " + tx2);

        System.out.println("\n=== 2. Testando Transação Legítima Paralela ===");
        Transacao txLegitima = new Transacao(5000L, 80, 99, 50_000_00L, tx3.getTimestamp());
        ResultadoCiclo resultadoLegitimo = grafo.verificarCiclo(
            txLegitima.getIdContaOrigem(), 
            txLegitima.getIdContaDestino(), 
            txLegitima.getValorCentavos(), 
            txLegitima.getTimestamp(), 
            4, 
            0.20 // 20% de tolerância
        );
        System.out.println("Tentativa de 80 -> 99: " + resultadoLegitimo);

        System.out.println("\n=== 3. Avaliando a Tentativa de Fechamento do Ciclo (80 -> 10) ===");
        ResultadoCiclo resultadoFraude = grafo.verificarCiclo(
            tx3.getIdContaOrigem(), 
            tx3.getIdContaDestino(), 
            tx3.getValorCentavos(), 
            tx3.getTimestamp(), 
            4, 
            0.20
        );
        System.out.println("Tentativa de 80 -> 10: " + resultadoFraude);

        if (resultadoFraude.isCicloDetectado()) {
            tx3.setStatus(StatusTransacao.BLOQUEADA);
            tx3.setMotivoFraude("ANEL_LAVAGEM_DETECTADO");
            System.out.println("-> Decisão do Motor: " + tx3);
        }

        System.out.println("\n=== 4. Métricas Topológicas (Graus) ===");
        System.out.println("Grau de Saída da Conta 25 (Fan-out): " + grafo.getGrauSaida(25));
        System.out.println("Grau de Entrada da Conta 80 (Fan-in): " + grafo.getGrauEntrada(80));
    }
}