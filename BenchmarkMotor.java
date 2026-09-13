import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public class BenchmarkMotor {
    private static final int[] CONTAS = {10, 15, 20, 25, 30, 40, 50, 60, 70, 80};
    private static final long JANELA_MS = 10 * 60 * 1000L; // 10 minutos
    private static final Random random = new Random(42);

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        System.out.println("=========================================================================================");
        System.out.println("             BENCHMARK DE DESEMPENHO E ESCALABILIDADE DO MOTOR ANTIFRAUDE                ");
        System.out.println("=========================================================================================\n");

        // 1. Fase de Aquecimento (JIT Warm-up)
        System.out.print("⚡ Executando aquecimento da JVM (Warm-up com 10.000 transações)... ");
        executarBateria(10_000, true);
        System.out.println("Concluído!\n");

        // 2. Baterias Oficiais de Teste
        int[] cargas = {1_000, 10_000, 50_000, 100_000, 500_000};
        
        System.out.println("+------------+-------------+----------------+-------------+-------------+-------------+");
        System.out.println("| Volume Txs | Tempo Total | Throughput     | Latência Média | Latência P95| Latência P99|");
        System.out.println("+------------+-------------+----------------+-------------+-------------+-------------+");

        for (int volume : cargas) {
            ResultadoBenchmark res = executarBateria(volume, false);
            System.out.printf("| %10d | %8.2f ms | %11.2f tx/s | %8.2f µs | %8.2f µs | %8.2f µs |\n",
                    res.volume, res.tempoTotalMs, res.throughput, res.latenciaMediaUs, res.p95Us, res.p99Us);
        }
        System.out.println("+------------+-------------+----------------+-------------+-------------+-------------+\n");

        System.out.println("💡 Detalhamento de Complexidade Teórica vs Prática:");
        System.out.println(" • Treap (Perfil/Saldos):   O(log N) -> Acesso em sub-microssegundos.");
        System.out.println(" • Janela Deslizante:       O(log K) -> Busca binária temporal no buffer.");
        System.out.println(" • Grafo & Bounded DFS:     O(V + E) podado (Profundidade <= 4).");
        System.out.println(" • Consumo Total por Tx:    O(log N + log K + b^D), mantendo latência sub-milissegundo.");
    }

    private static ResultadoBenchmark executarBateria(int volume, boolean warmup) {
        MotorFraude motor = new MotorFraude(JANELA_MS);
        long[] latenciasNs = new long[volume];
        long timestampBase = 1700000000000L;
        long idTransacao = 1000L;

        // Pré-carrega as contas na Treap
        for (int c : CONTAS) {
            motor.getTreapContas().obterOuCriar(c, 10_000_000_00L, 2_000_00L);
        }

        long inicioTotal = System.nanoTime();

        for (int i = 0; i < volume; i++) {
            // Mix de carga: 80% legítimas, 10% rajadas/smurfing, 10% tentativas de anel
            Transacao tx = gerarTransacaoSintetica(idTransacao++, timestampBase + (i * 500L));

            long t0 = System.nanoTime();
            motor.processarTransacao(tx);
            long t1 = System.nanoTime();

            latenciasNs[i] = (t1 - t0);
        }

        long fimTotal = System.nanoTime();
        double tempoTotalMs = (fimTotal - inicioTotal) / 1_000_000.0;
        double throughput = (volume / (tempoTotalMs / 1000.0));

        // Ordena o array para calcular os percentis (P50, P95, P99)
        Arrays.sort(latenciasNs);

        double latenciaMediaUs = Arrays.stream(latenciasNs).average().orElse(0.0) / 1_000.0;
        double p50Us = latenciasNs[(int) (volume * 0.50)] / 1_000.0;
        double p95Us = latenciasNs[(int) (volume * 0.95)] / 1_000.0;
        double p99Us = latenciasNs[(int) (volume * 0.99)] / 1_000.0;

        return new ResultadoBenchmark(volume, tempoTotalMs, throughput, latenciaMediaUs, p50Us, p95Us, p99Us);
    }

    private static Transacao gerarTransacaoSintetica(long id, long timestamp) {
        int tipo = random.nextInt(100);

        if (tipo < 80) {
            // 80% Legítimas: Valores moderados entre contas aleatórias
            int orig = CONTAS[random.nextInt(CONTAS.length)];
            int dest;
            do {
                dest = CONTAS[random.nextInt(CONTAS.length)];
            } while (dest == orig);

            long valor = (random.nextInt(500) + 20) * 100L; // R$ 20,00 a R$ 520,00
            return new Transacao(id, orig, dest, valor, timestamp);
        } else if (tipo < 90) {
            // 10% Smurfing: Transferências fracionadas repetitivas
            return new Transacao(id, 15, 40, 9_600_00L, timestamp);
        } else {
            // 10% Padrão de Ciclo: Simula pernas de lavagem (20 -> 30 -> 40 -> 20)
            int passo = (int) (id % 3);
            if (passo == 0) return new Transacao(id, 20, 30, 50_000_00L, timestamp);
            if (passo == 1) return new Transacao(id, 30, 40, 49_000_00L, timestamp);
            return new Transacao(id, 40, 20, 48_000_00L, timestamp);
        }
    }

    private static class ResultadoBenchmark {
        final int volume;
        final double tempoTotalMs;
        final double throughput;
        final double latenciaMediaUs;
        final double p50Us;
        final double p95Us;
        final double p99Us;

        ResultadoBenchmark(int volume, double tempoTotalMs, double throughput,
                           double latenciaMediaUs, double p50Us, double p95Us, double p99Us) {
            this.volume = volume;
            this.tempoTotalMs = tempoTotalMs;
            this.throughput = throughput;
            this.latenciaMediaUs = latenciaMediaUs;
            this.p50Us = p50Us;
            this.p95Us = p95Us;
            this.p99Us = p99Us;
        }
    }
}