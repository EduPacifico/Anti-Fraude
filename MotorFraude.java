import java.util.List;

public class MotorFraude {
    private final TreapContas treapContas;
    private final JanelaDeslizante janelaDeslizante;
    private final GrafoTransacional grafoTransacional;
    private final long duracaoJanelaMs;

    // Limiares de Decisão
    private static final double LIMIAR_SUSPEITA = 40.0;
    private static final double LIMIAR_BLOQUEIO = 75.0;

    public MotorFraude(long duracaoJanelaMs) {
        this.duracaoJanelaMs = duracaoJanelaMs;
        this.treapContas = new TreapContas(42L);
        this.janelaDeslizante = new JanelaDeslizante(duracaoJanelaMs);
        this.grafoTransacional = new GrafoTransacional();
    }

    public Transacao processarTransacao(Transacao tx) {
        // 0. Sincronização e expurgo de dados obsoletos na janela e no grafo
        janelaDeslizante.expurgarExpiradas(tx.getTimestamp());
        grafoTransacional.expurgarArestasExpiradas(tx.getTimestamp(), duracaoJanelaMs);

        // =====================================================================
        // CHECAGEM 1: Treap Balanceada (Perfil de Conta e Saldo em O(log N))
        // =====================================================================
        Conta origem = treapContas.obterOuCriar(tx.getIdContaOrigem(), 100_000_00L, 2_000_00L);
        Conta destino = treapContas.obterOuCriar(tx.getIdContaDestino(), 10_000_00L, 2_000_00L);

        // Validação de saldo
        if (origem.getSaldoCentavos() < tx.getValorCentavos()) {
            tx.setStatus(StatusTransacao.BLOQUEADA);
            tx.setScoreFraude(100.0);
            tx.setMotivoFraude("SALDO_INSUFICIENTE");
            return tx;
        }

        double scoreCalculado = 0.0;
        StringBuilder motivos = new StringBuilder();

        // 1.1 Anomalia de valor individual em relação à média diária histórica
        if (tx.getValorCentavos() > 3 * origem.getVolumeMedioDiarioCentavos()) {
            scoreCalculado += 25.0;
            motivos.append("[VALOR_ATIPICO_ACIMA_DA_MEDIA] ");
        }

        // 1.2 Influência do score de risco pré-existente da conta
        if (origem.getScoreRisco() > 40.0) {
            scoreCalculado += (origem.getScoreRisco() * 0.25);
            motivos.append("[CONTA_ORIGEM_RISCO_ELEVADO] ");
        }

        // =====================================================================
        // CHECAGEM 2: Janela Deslizante (Análise Temporal e Smurfing)
        // =====================================================================
        MetricasJanela metricasOrigem = janelaDeslizante.avaliarAtividadeConta(
            tx.getIdContaOrigem(), duracaoJanelaMs, tx.getTimestamp()
        );

        if (metricasOrigem.isAlertaSmurfing()) {
            scoreCalculado += 45.0;
            motivos.append("[SMURFING_FRACIONAMENTO_DETECTADO] ");
        }

        if (metricasOrigem.isAlertaExplosaoVelocidade()) {
            scoreCalculado += 30.0;
            motivos.append("[EXPLOSAO_VELOCIDADE_TEMPORAL] ");
        }

        // =====================================================================
        // CHECAGEM 3: Grafo Transacional (Ciclos, Fan-in e Fan-out)
        // =====================================================================
        // 3.1 Detecção de Anéis / Ciclos via Bounded DFS
        ResultadoCiclo resultadoCiclo = grafoTransacional.verificarCiclo(
            tx.getIdContaOrigem(), 
            tx.getIdContaDestino(), 
            tx.getValorCentavos(), 
            tx.getTimestamp(), 
            4, 
            0.20
        );

        if (resultadoCiclo.isCicloDetectado()) {
            scoreCalculado += 85.0;
            motivos.append("[ANEL_LAVAGEM_CICLO_DETECTADO: ").append(resultadoCiclo.getCaminhoContas()).append("] ");
        }

        // 3.2 Detecção de Concentração em Funil (Fan-in: Múltiplas origens para um destino)
        int grauEntradaDestino = grafoTransacional.getGrauEntrada(tx.getIdContaDestino());
        if (grauEntradaDestino >= 3) {
            scoreCalculado += 45.0;
            motivos.append("[FAN_IN_CONCENTRACAO_DETECTADA: In-Degree=").append(grauEntradaDestino + 1).append("] ");
        }

        // 3.3 Detecção de Pulverização em Leque (Fan-out anômalo)
        int grauSaidaOrigem = grafoTransacional.getGrauSaida(tx.getIdContaOrigem());
        if (grauSaidaOrigem >= 5) {
            scoreCalculado += 20.0;
            motivos.append("[FAN_OUT_DISPERSAO_ELEVADA] ");
        }

        // =====================================================================
        // DECISÃO FINAL E PROPAGAÇÃO DE RISCO NA TREAP
        // =====================================================================
        tx.setScoreFraude(Math.min(100.0, scoreCalculado));

        if (tx.getScoreFraude() >= LIMIAR_BLOQUEIO || resultadoCiclo.isCicloDetectado()) {
            tx.setStatus(StatusTransacao.BLOQUEADA);
            tx.setMotivoFraude(motivos.length() > 0 ? motivos.toString() : "RISCO_CRITICO");
            
            origem.setScoreRisco(Math.min(100.0, Math.max(origem.getScoreRisco() + 30.0, tx.getScoreFraude())));

            // Se for Fan-in crítico, eleva também o risco da conta arrecadadora (Destino)
            if (grauEntradaDestino >= 3) {
                destino.setScoreRisco(Math.max(destino.getScoreRisco(), 75.0));
            }

            // Propaga para as contas do anel de lavagem
            if (resultadoCiclo.isCicloDetectado() && resultadoCiclo.getCaminhoContas() != null) {
                for (int idContaAnel : resultadoCiclo.getCaminhoContas()) {
                    Conta contaAnel = treapContas.buscar(idContaAnel);
                    if (contaAnel != null) {
                        contaAnel.setScoreRisco(Math.max(contaAnel.getScoreRisco(), 85.0));
                    }
                }
            }
        } else if (tx.getScoreFraude() >= LIMIAR_SUSPEITA) {
            tx.setStatus(StatusTransacao.SUSPEITA);
            tx.setMotivoFraude(motivos.toString());
            
            origem.setScoreRisco(Math.min(100.0, Math.max(origem.getScoreRisco() + 15.0, tx.getScoreFraude() * 0.7)));
            if (grauEntradaDestino >= 3) {
                destino.setScoreRisco(Math.max(destino.getScoreRisco(), 60.0));
            }
            efetivarTransacao(tx, origem, destino);
        } else {
            tx.setStatus(StatusTransacao.APROVADA);
            tx.setMotivoFraude("TRANSACAO_LEGITIMA");
            efetivarTransacao(tx, origem, destino);
        }

        return tx;
    }

    private void efetivarTransacao(Transacao tx, Conta origem, Conta destino) {
        origem.setSaldoCentavos(origem.getSaldoCentavos() - tx.getValorCentavos());
        destino.setSaldoCentavos(destino.getSaldoCentavos() + tx.getValorCentavos());
        
        origem.registrarSaida(tx.getValorCentavos());
        destino.registrarEntrada(tx.getValorCentavos());
        origem.setTimestampUltimaTransacao(tx.getTimestamp());

        janelaDeslizante.adicionarTransacao(tx);
        grafoTransacional.adicionarAresta(tx);
    }

    public TreapContas getTreapContas() { return treapContas; }
    public JanelaDeslizante getJanelaDeslizante() { return janelaDeslizante; }
    public GrafoTransacional getGrafoTransacional() { return grafoTransacional; }
}