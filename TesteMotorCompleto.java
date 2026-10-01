import java.util.List;

public class TesteMotorCompleto {
    public static void main(String[] args) {
        long timestampBase = 1700000000000L;
        long dezMinutosMs = 10 * 60 * 1000L;

        MotorFraude motor = new MotorFraude(dezMinutosMs);
        GeradorTransacoes gerador = new GeradorTransacoes(50, timestampBase);

        System.out.println("================================================================================");
        System.out.println("              SIMULAÇÃO DO MOTOR DE DETECÇÃO DE FRAUDES                         ");
        System.out.println("================================================================================\n");

        // 1. Processando transações comuns legítimas
        System.out.println("--- 1. PROCESSANDO FLUXO LEGÍTIMO ---");
        for (int i = 0; i < 3; i++) {
            Transacao tx = gerador.gerarTransacaoLegitima();
            Transacao processada = motor.processarTransacao(tx);
            imprimirResultado(processada);
        }

        // 2. Injetando e processando padrão de Smurfing (Fracionamento)
        System.out.println("\n--- 2. INJETANDO PADRÃO DE SMURFING (Conta 15 -> Conta 40) ---");
        List<Transacao> rajadaSmurfing = gerador.injetarRajadaSmurfing(15, 40);
        for (Transacao tx : rajadaSmurfing) {
            Transacao processada = motor.processarTransacao(tx);
            imprimirResultado(processada);
        }

        // 3. Injetando e processando Anel de Fraude (Ciclo A -> B -> C -> A)
        System.out.println("\n--- 3. INJETANDO ANEL DE LAVAGEM (Ciclo: 20 -> 30 -> 40 -> 20) ---");
        List<Transacao> ciclo = gerador.injetarPadraoCiclo(20, 30, 40, 50_000_00L);
        for (Transacao tx : ciclo) {
            Transacao processada = motor.processarTransacao(tx);
            imprimirResultado(processada);
        }
    }

    private static void imprimirResultado(Transacao tx) {
        String icone = "";
switch (tx.getStatus()) {
    case APROVADA: 
        icone = "✅"; 
        break;
    case SUSPEITA: 
        icone = "⚠️"; 
        break;
    case BLOQUEADA: 
        icone = "🛑"; 
        break;
    default: 
        icone = "❓"; 
        break;
}

        System.out.printf("%s Tx #%d | %d -> %d | R$ %8.2f | Score: %5.1f | Status: %-9s | Motivo: %s\n",
                icone, tx.getId(), tx.getIdContaOrigem(), tx.getIdContaDestino(),
                tx.getValorCentavos() / 100.0, tx.getScoreFraude(), tx.getStatus(), tx.getMotivoFraude());
    }
}