import java.util.ArrayList;
import java.util.List;

public class JanelaDeslizante {
    private final List<Transacao> buffer;
    private final long duracaoJanelaMs;

    public JanelaDeslizante(long duracaoJanelaMs) {
        this.duracaoJanelaMs = duracaoJanelaMs;
        this.buffer = new ArrayList<>();
    }

    public void adicionarTransacao(Transacao tx) {
        buffer.add(tx);
    }

    public void expurgarExpiradas(long timestampAtual) {
        long limite = timestampAtual - duracaoJanelaMs;
        int idx = lowerBound(limite);
        if (idx > 0) {
            buffer.subList(0, idx).clear();
        }
    }

    public int lowerBound(long timestampLimite) {
        int inicio = 0;
        int fim = buffer.size();
        while (inicio < fim) {
            int meio = (inicio + fim) >>> 1;
            if (buffer.get(meio).getTimestamp() >= timestampLimite) {
                fim = meio;
            } else {
                inicio = meio + 1;
            }
        }
        return inicio;
    }

    public MetricasJanela avaliarAtividadeConta(int idConta, long duracaoJanelaMs, long timestampAtual) {
        long limiteInferior = timestampAtual - duracaoJanelaMs;
        int idxInicio = lowerBound(limiteInferior);

        int qtdTotal = 0;
        long valorTotalCentavos = 0;
        int qtdJanelaCurta = 0;
        long limiteCurto = timestampAtual - (60 * 1000L); // Janela curta de 1 minuto (60s)

        for (int i = idxInicio; i < buffer.size(); i++) {
            Transacao t = buffer.get(i);
            if (t.getIdContaOrigem() == idConta) {
                qtdTotal++;
                valorTotalCentavos += t.getValorCentavos();
                if (t.getTimestamp() >= limiteCurto) {
                    qtdJanelaCurta++;
                }
            }
        }

        // Limiares Operacionais
        boolean alertaSmurfing = (qtdTotal >= 4 && valorTotalCentavos >= 25_000_00L);
        boolean alertaExplosaoVelocidade = (qtdJanelaCurta >= 4); // Dispara ao atingir 4+ disparos em 1 minuto

        return new MetricasJanela(qtdTotal, valorTotalCentavos, alertaSmurfing, alertaExplosaoVelocidade);
    }

    public int getQuantidadeTransacoesAtivas() {
        return buffer.size();
    }
}