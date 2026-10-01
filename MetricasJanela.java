public class MetricasJanela {
    private final int qtdTransacoes;
    private final long valorTotalCentavos;
    private final boolean alertaSmurfing;
    private final boolean alertaExplosaoVelocidade;

    public MetricasJanela(int qtdTransacoes, long valorTotalCentavos, boolean alertaSmurfing, boolean alertaExplosaoVelocidade) {
        this.qtdTransacoes = qtdTransacoes;
        this.valorTotalCentavos = valorTotalCentavos;
        this.alertaSmurfing = alertaSmurfing;
        this.alertaExplosaoVelocidade = alertaExplosaoVelocidade;
    }

    public int getQtdTransacoes() {
        return qtdTransacoes;
    }

    public long getValorTotalCentavos() {
        return valorTotalCentavos;
    }

    public boolean isAlertaSmurfing() {
        return alertaSmurfing;
    }

    public boolean isAlertaExplosaoVelocidade() {
        return alertaExplosaoVelocidade;
    }
}