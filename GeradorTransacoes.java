import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeradorTransacoes {
    private final int totalContas;
    private final Random random;
    private long idTransacaoAtual;
    private long timestampAtual;

    public GeradorTransacoes(int totalContas, long timestampInicial) {
        this.totalContas = totalContas;
        this.random = new Random(42); // Seed fixa para reprodutibilidade
        this.idTransacaoAtual = 1000L;
        this.timestampAtual = timestampInicial;
    }

    // Gera uma transação legítima com valores comuns
    public Transacao gerarTransacaoLegitima() {
        int origem = random.nextInt(totalContas) + 1;
        int destino;
        do {
            destino = random.nextInt(totalContas) + 1;
        } while (destino == origem);

        long valorCentavos = (random.nextInt(1490) + 10) * 100L; // R$ 10,00 a R$ 1.500,00
        timestampAtual += (random.nextInt(5) + 1) * 1000L;       // Avanço de 1 a 5 segundos

        return new Transacao(idTransacaoAtual++, origem, destino, valorCentavos, timestampAtual);
    }

    // Injeta um ciclo de lavagem (A -> B -> C -> A)
    public List<Transacao> injetarPadraoCiclo(int contaA, int contaB, int contaC, long valorBaseCentavos) {
        List<Transacao> ciclo = new ArrayList<>();

        // Passo 1: A -> B
        timestampAtual += 30_000L; // +30 segundos
        ciclo.add(new Transacao(idTransacaoAtual++, contaA, contaB, valorBaseCentavos, timestampAtual));

        // Passo 2: B -> C (descontando taxa de 2%)
        timestampAtual += 45_000L; // +45 segundos
        long taxa1 = (long) (valorBaseCentavos * 0.02);
        ciclo.add(new Transacao(idTransacaoAtual++, contaB, contaC, valorBaseCentavos - taxa1, timestampAtual));

        // Passo 3: C -> A (descontando mais 2% e fechando o ciclo)
        timestampAtual += 40_000L; // +40 segundos
        long taxa2 = (long) (valorBaseCentavos * 0.04);
        ciclo.add(new Transacao(idTransacaoAtual++, contaC, contaA, valorBaseCentavos - taxa2, timestampAtual));

        return ciclo;
    }

    // Injeta uma rajada de smurfing (fracionamento de R$ 9.600 para não estourar R$ 10.000)
    public List<Transacao> injetarRajadaSmurfing(int contaOrigem, int contaDestino) {
        List<Transacao> rajada = new ArrayList<>();
        long valorSmurfCentavos = 9_600_00L; // R$ 9.600,00

        for (int i = 0; i < 5; i++) {
            timestampAtual += 15_000L; // Disparos a cada 15 segundos
            rajada.add(new Transacao(idTransacaoAtual++, contaOrigem, contaDestino, valorSmurfCentavos, timestampAtual));
        }
        return rajada;
    }
}