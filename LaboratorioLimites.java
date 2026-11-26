import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * Laboratório de Exploração de Limites e Sensibilidade do Motor Antifraude.
 * Avalia empiricamente o trade-off entre precisão de detecção e custo computacional:
 *  1. Limite de Profundidade da Bounded DFS (D_max = 2 até 8 saltos) -> O(b^D)
 *  2. Limite de Retenção da Janela Deslizante (1 min, 10 min, 1 hora, 6 horas, 24 horas) -> O(log K + K_ativo)
 *  3. Sensibilidade da Poda de Preservação de Valor (±5%, ±10%, ±20%, ±35%, ±50%)
 */
public class LaboratorioLimites {

    private static final int VOLUME_PADRAO = 20_000;

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        System.out.println("=====================================================================================================");
        System.out.println("          LABORATORIO DE EXPLORACAO DE LIMITES E TRADE-OFFS DO MOTOR ANTIFRAUDE (IN-MEMORY)          ");
        System.out.println("=====================================================================================================\n");

        // Warm-up da JVM
        System.out.print("⚡ Aquecendo JIT Compiler da JVM... ");
        simularCenario(5_000, 10 * 60 * 1000L, 4, 0.20, 6);
        System.out.println("Pronto!\n");

        experimento1ProfundidadeDfs();
        experimento2JanelaTemporal();
        experimento3ToleranciaValor();
    }

    // =================================================================================================
    // EXPERIMENTO 1: Variação da Profundidade da Bounded DFS (D = 2, 3, 4, 5, 6, 7, 8)
    // =================================================================================================
    private static void experimento1ProfundidadeDfs() {
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.println(" EXPERIMENTO 1: IMPACTO DA PROFUNDIDADE DA BOUNDED DFS (D_max) — Complexidade O(b^D)");
        System.out.println(" Cenário: Anéis longos de lavagem (6 contas: C1->C2->C3->C4->C5->C6->C1) misturados ao tráfego denso");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.println("+---------+--------------+---------------+--------------+-----------------+-------------------------+");
        System.out.println("|  D_max  |  Throughput  | Latência Média| Latência P99 | Arestas DFS/Tx  | Ciclos Longos (6 saltos)|");
        System.out.println("+---------+--------------+---------------+--------------+-----------------+-------------------------+");

        int[] profundidades = {2, 3, 4, 5, 6, 7, 8};
        long janela10Min = 10 * 60 * 1000L;

        for (int d : profundidades) {
            ResultadoSimulacao r = simularCenario(VOLUME_PADRAO, janela10Min, d, 0.20, 6);
            String statusCiclo6 = r.ciclosDetectados > 0
                    ? String.format("DETECTADO (%d anéis)", r.ciclosDetectados)
                    : "FALSO NEGATIVO (Cego)";
            System.out.printf("| D <= %-2d | %9.0f/s | %10.2f µs | %9.2f µs | %15.1f | %-23s |\n",
                    d, r.throughput, r.latenciaMediaUs, r.p99Us, r.mediaArestasDfsPorTx, statusCiclo6);
        }
        System.out.println("+---------+--------------+---------------+--------------+-----------------+-------------------------+");
        System.out.println("📌 Conclusão 1: Com D <= 4 o motor não enxerga anéis de 6 contas (exige 5 saltos de busca reversa).");
        System.out.println("   Ao subir para D = 5 ou 6, captura anéis complexos mantendo latência na casa de microssegundos;\n"
                         + "   a partir de D >= 7 o número de arestas visitadas pela DFS cresce exponencialmente (O(b^D)).\n");
    }

    // =================================================================================================
    // EXPERIMENTO 2: Variação da Janela Deslizante (1 min, 10 min, 30 min, 1 hora, 6 horas, 24 horas)
    // =================================================================================================
    private static void experimento2JanelaTemporal() {
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.println(" EXPERIMENTO 2: IMPACTO DO TAMANHO DA JANELA DESLIZANTE (Delta t) — Memória & Varredura");
        System.out.println(" Cenário: Smurfing lento (parcelas de R$ 9.600 espaçadas a cada 12 minutos para burlar janela de 10m)");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.println("+---------------+--------------+---------------+--------------+----------------+--------------------+");
        System.out.println("| Janela (min)  |  Throughput  | Latência Média| Buffer Ativo | Arestas Grafo  | Smurfing Lento     |");
        System.out.println("+---------------+--------------+---------------+--------------+----------------+--------------------+");

        long[] janelasMinutos = {1, 10, 30, 60, 360, 1440};

        for (long min : janelasMinutos) {
            long janelaMs = min * 60 * 1000L;
            ResultadoSimulacao r = simularJanelaLonga(VOLUME_PADRAO, janelaMs, 4, 0.20);
            String labelJanela = min >= 60 ? (min / 60) + "h (" + min + "m)" : min + " min";
            String smurfingStatus = r.smurfingLentoDetectado ? "PEGO (Bloqueado)" : "ESCAPOU (Expirou)";
            System.out.printf("| %-13s | %9.0f/s | %10.2f µs | %8d txs | %8d ares. | %-18s |\n",
                    labelJanela, r.throughput, r.latenciaMediaUs, r.bufferAtivoFinal, r.arestasAtivasFinal, smurfingStatus);
        }
        System.out.println("+---------------+--------------+---------------+--------------+----------------+--------------------+");
        System.out.println("📌 Conclusão 2: Janelas de 1h (60 min) capturam fraudadores pacientes que espaçam transferências a cada");
        System.out.println("   12 minutos, mas retêm ~6x mais transações no ArrayList da Janela e listas de adjacência do Grafo.\n");
    }

    // =================================================================================================
    // EXPERIMENTO 3: Variação da Tolerância da Poda de Valor (±5%, ±10%, ±20%, ±35%, ±50%)
    // =================================================================================================
    private static void experimento3ToleranciaValor() {
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.println(" EXPERIMENTO 3: SENSIBILIDADE DA PODA DE PRESERVACAO DE VALOR (±%) NA BOUNDED DFS");
        System.out.println(" Cenário: Anéis onde laranjas retêm 12% de comissão por salto vs. Coincidências comerciais legítimas");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.println("+----------------+--------------+---------------+--------------------+------------------------------+");
        System.out.println("| Tolerância (%) |  Throughput  | Arestas DFS   | Anel (Taxa 12%/nó) | Falsos Positivos Comerciais  |");
        System.out.println("+----------------+--------------+---------------+--------------------+------------------------------+");

        double[] tolerancias = {0.05, 0.10, 0.20, 0.35, 0.50};

        for (double tol : tolerancias) {
            ResultadoSimulacao r = simularSensibilidadeValor(VOLUME_PADRAO, tol);
            String anel12 = r.ciclosDetectados > 0 ? "DETECTADO" : "ESCAPOU (Podado)";
            System.out.printf("|     ±%4.0f%%     | %9.0f/s | %11.1f   | %-18s | %14d bloqueios     |\n",
                    tol * 100.0, r.throughput, r.mediaArestasDfsPorTx, anel12, r.falsosPositivos);
        }
        System.out.println("+----------------+--------------+---------------+--------------------+------------------------------+");
        System.out.println("📌 Conclusão 3: Tolerância muito rígida (±5% ou ±10%) sofre Falsos Negativos quando o laranja desconta");
        System.out.println("   comissões maiores (12%). Tolerância frouxa (±50%) reduz a eficácia da poda e eleva Falsos Positivos.\n");
    }

    // =================================================================================================
    // Método Público para Execução via API do ServidorVisual
    // =================================================================================================
    public static ResultadoSimulacao executarSimulacaoCustomizada(int volume, long duracaoJanelaMs,
                                                                  int profundidadeDfs, double toleranciaValor,
                                                                  int tamanhoAnelTeste) {
        return simularCenario(volume, duracaoJanelaMs, profundidadeDfs, toleranciaValor, tamanhoAnelTeste);
    }

    private static ResultadoSimulacao simularCenario(int volume, long duracaoJanelaMs,
                                                     int profundidadeDfs, double toleranciaValor,
                                                     int tamanhoAnel) {
        MotorFraude motor = new MotorFraude(duracaoJanelaMs, profundidadeDfs, toleranciaValor);
        Random rng = new Random(42L);
        int numContasTotal = 120;

        for (int c = 1; c <= numContasTotal; c++) {
            motor.getTreapContas().obterOuCriar(c, 500_000_000_00L, 50_000_00L);
        }

        long[] latenciasNs = new long[volume];
        long timestamp = 1_700_000_000_000L;
        long idTx = 1000L;
        long somaArestasDfs = 0;
        int aneisAlvoDetectados = 0;
        int aneisAlvoInjetados = 0;
        int bloqueadas = 0;

        long tInicio = System.nanoTime();

        for (int i = 0; i < volume; i++) {
            timestamp += 400L;
            Transacao tx;
            boolean ehFechoAnelAlvo = false;

            // Contas 1..tamanhoAnel são dedicadas ao anel longo sob teste (ex: 1->2->3->4->5->6->1)
            // A cada 500 transações, injeta um anel completo espaçado do anterior
            int posBloco = i % 500;
            if (posBloco < tamanhoAnel) {
                if (posBloco == 0) {
                    // Avança o tempo para expurgar o anel anterior ou usa valores distintos por lote
                    timestamp += duracaoJanelaMs + 1000L;
                }
                int origem = posBloco + 1;
                int destino = (posBloco == tamanhoAnel - 1) ? 1 : (posBloco + 2);
                long valor = 50_000_00L - (posBloco * 400_00L);
                tx = new Transacao(idTx++, origem, destino, valor, timestamp);
                if (posBloco == tamanhoAnel - 1) {
                    ehFechoAnelAlvo = true;
                    aneisAlvoInjetados++;
                }
            } else {
                // Tráfego denso de fundo entre contas 20..120 (árvore/DAG ramificada para estressar a DFS)
                int origem = 20 + rng.nextInt(80);
                int destino = Math.min(120, origem + 1 + rng.nextInt(15));
                long valor = (45_000 + rng.nextInt(10_000)) * 100L;
                tx = new Transacao(idTx++, origem, destino, valor, timestamp);
            }

            long t0 = System.nanoTime();
            motor.processarTransacao(tx);
            long t1 = System.nanoTime();

            latenciasNs[i] = (t1 - t0);
            somaArestasDfs += motor.getGrafoTransacional().getUltimoArestasAvaliadasDfs();

            if (ehFechoAnelAlvo && tx.getMotivoFraude().contains("ANEL_LAVAGEM_CICLO_DETECTADO")) {
                aneisAlvoDetectados++;
            }
            if (tx.getStatus() == StatusTransacao.BLOQUEADA) {
                bloqueadas++;
            }
        }

        long tFim = System.nanoTime();
        double tempoTotalMs = (tFim - tInicio) / 1_000_000.0;
        double throughput = volume / (tempoTotalMs / 1000.0);

        Arrays.sort(latenciasNs);
        double latMediaUs = Arrays.stream(latenciasNs).average().orElse(0.0) / 1_000.0;
        double p99Us = latenciasNs[(int) (volume * 0.99)] / 1_000.0;
        double mediaArestasDfs = (double) somaArestasDfs / volume;

        return new ResultadoSimulacao(
                volume, tempoTotalMs, throughput, latMediaUs, p99Us,
                mediaArestasDfs, aneisAlvoDetectados, bloqueadas,
                motor.getJanelaDeslizante().getQuantidadeTransacoesAtivas(),
                motor.getGrafoTransacional().getTotalArestasAtivas(),
                false, 0
        );
    }

    private static ResultadoSimulacao simularJanelaLonga(int volume, long duracaoJanelaMs,
                                                         int profundidadeDfs, double toleranciaValor) {
        MotorFraude motor = new MotorFraude(duracaoJanelaMs, profundidadeDfs, toleranciaValor);
        Random rng = new Random(42L);
        int numContas = 200;

        for (int c = 1; c <= numContas; c++) {
            motor.getTreapContas().obterOuCriar(c, 50_000_000_00L, 5_000_00L);
        }

        long[] latenciasNs = new long[volume];
        long timestamp = 1_700_000_000_000L;
        long idTx = 1000L;
        boolean smurfingLentoDetectado = false;
        int parcelasSmurfingEnviadas = 0;

        long tInicio = System.nanoTime();

        for (int i = 0; i < volume; i++) {
            // Cada transação avança 2 segundos no relógio simulado (20.000 txs = ~11 horas simuladas)
            timestamp += 2_000L;
            Transacao tx;

            // A cada 360 txs (720 segundos = 12 minutos), a Conta 199 dispara R$ 9.600 (Smurfing Lento)
            if (i > 0 && i % 360 == 0 && parcelasSmurfingEnviadas < 5) {
                parcelasSmurfingEnviadas++;
                tx = new Transacao(idTx++, 199, 200, 9_600_00L, timestamp);
            } else {
                int origem = rng.nextInt(190) + 1;
                int destino = rng.nextInt(190) + 1;
                if (destino == origem) destino = (origem % 190) + 1;
                long valor = (rng.nextInt(800) + 50) * 100L;
                tx = new Transacao(idTx++, origem, destino, valor, timestamp);
            }

            long t0 = System.nanoTime();
            motor.processarTransacao(tx);
            long t1 = System.nanoTime();
            latenciasNs[i] = (t1 - t0);

            if (tx.getIdContaOrigem() == 199 && tx.getMotivoFraude().contains("SMURFING")) {
                smurfingLentoDetectado = true;
            }
        }

        long tFim = System.nanoTime();
        double tempoTotalMs = (tFim - tInicio) / 1_000_000.0;
        double throughput = volume / (tempoTotalMs / 1000.0);

        Arrays.sort(latenciasNs);
        double latMediaUs = Arrays.stream(latenciasNs).average().orElse(0.0) / 1_000.0;
        double p99Us = latenciasNs[(int) (volume * 0.99)] / 1_000.0;

        return new ResultadoSimulacao(
                volume, tempoTotalMs, throughput, latMediaUs, p99Us,
                0.0, 0, 0,
                motor.getJanelaDeslizante().getQuantidadeTransacoesAtivas(),
                motor.getGrafoTransacional().getTotalArestasAtivas(),
                smurfingLentoDetectado, 0
        );
    }

    private static ResultadoSimulacao simularSensibilidadeValor(int volume, double toleranciaValor) {
        MotorFraude motor = new MotorFraude(10 * 60 * 1000L, 4, toleranciaValor);
        Random rng = new Random(99L);
        int numContas = 60;

        for (int c = 1; c <= numContas; c++) {
            motor.getTreapContas().obterOuCriar(c, 50_000_000_00L, 5_000_00L);
        }

        long timestamp = 1_700_000_000_000L;
        long idTx = 1000L;
        long somaArestasDfs = 0;
        int ciclosReaisDetectados = 0;
        int falsosPositivos = 0;

        long tInicio = System.nanoTime();

        // 1. Injeta um anel real onde cada laranja desconta ~12% sobre a etapa anterior:
        // 10->20 (R$ 50.000) -> 20->30 (R$ 44.000) -> 30->10 (R$ 39.000)
        // Na DFS reversa: parte de 10 com valorReferencia = 39.000 e olha aresta 10->20 (50.000 = +28.2% sobre 39.000)
        motor.processarTransacao(new Transacao(idTx++, 10, 20, 50_000_00L, timestamp += 1000L));
        motor.processarTransacao(new Transacao(idTx++, 20, 30, 44_000_00L, timestamp += 1000L));
        Transacao fechoAnel = new Transacao(idTx++, 30, 10, 39_000_00L, timestamp += 1000L);
        motor.processarTransacao(fechoAnel);
        if (fechoAnel.getMotivoFraude().contains("ANEL_LAVAGEM_CICLO_DETECTADO")) {
            ciclosReaisDetectados++;
        }

        // 2. Tráfego comercial legítimo com valores variados (R$ 1.000 a R$ 3.500) entre fornecedores
        for (int i = 0; i < volume; i++) {
            timestamp += 500L;
            int origem = rng.nextInt(40) + 15;
            int destino = rng.nextInt(40) + 15;
            if (destino == origem) destino = origem + 1;
            long valor = (1_000 + rng.nextInt(2_500)) * 100L;

            Transacao tx = new Transacao(idTx++, origem, destino, valor, timestamp);
            motor.processarTransacao(tx);
            somaArestasDfs += motor.getGrafoTransacional().getUltimoArestasAvaliadasDfs();

            if (tx.getMotivoFraude().contains("ANEL_LAVAGEM_CICLO_DETECTADO")) {
                falsosPositivos++;
            }
        }

        long tFim = System.nanoTime();
        double tempoTotalMs = (tFim - tInicio) / 1_000_000.0;
        double throughput = volume / (tempoTotalMs / 1000.0);

        return new ResultadoSimulacao(
                volume, tempoTotalMs, throughput, 0.0, 0.0,
                (double) somaArestasDfs / volume, ciclosReaisDetectados, falsosPositivos,
                motor.getJanelaDeslizante().getQuantidadeTransacoesAtivas(),
                motor.getGrafoTransacional().getTotalArestasAtivas(),
                false, falsosPositivos
        );
    }

    public static class ResultadoSimulacao {
        public final int volume;
        public final double tempoTotalMs;
        public final double throughput;
        public final double latenciaMediaUs;
        public final double p99Us;
        public final double mediaArestasDfsPorTx;
        public final int ciclosDetectados;
        public final int bloqueadas;
        public final int bufferAtivoFinal;
        public final int arestasAtivasFinal;
        public final boolean smurfingLentoDetectado;
        public final int falsosPositivos;

        public ResultadoSimulacao(int volume, double tempoTotalMs, double throughput,
                                  double latenciaMediaUs, double p99Us, double mediaArestasDfsPorTx,
                                  int ciclosDetectados, int bloqueadas, int bufferAtivoFinal,
                                  int arestasAtivasFinal, boolean smurfingLentoDetectado, int falsosPositivos) {
            this.volume = volume;
            this.tempoTotalMs = tempoTotalMs;
            this.throughput = throughput;
            this.latenciaMediaUs = latenciaMediaUs;
            this.p99Us = p99Us;
            this.mediaArestasDfsPorTx = mediaArestasDfsPorTx;
            this.ciclosDetectados = ciclosDetectados;
            this.bloqueadas = bloqueadas;
            this.bufferAtivoFinal = bufferAtivoFinal;
            this.arestasAtivasFinal = arestasAtivasFinal;
            this.smurfingLentoDetectado = smurfingLentoDetectado;
            this.falsosPositivos = falsosPositivos;
        }
    }
}
