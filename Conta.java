public class Conta {
    private final int idConta;
    private long saldoCentavos;
    private long volumeMedioDiarioCentavos;
    private double scoreRisco;
    private long timestampUltimaTransacao;

    // Métricas temporais para identificar Contas Laranja / Contas de Passagem
    private long totalRecebidoJanelaCentavos;
    private long totalEnviadoJanelaCentavos;
    private int qtdTransacoesJanela;

    public Conta(int idConta, long saldoInicialCentavos, long volumeMedioDiarioCentavos) {
        this.idConta = idConta;
        this.saldoCentavos = saldoInicialCentavos;
        this.volumeMedioDiarioCentavos = volumeMedioDiarioCentavos;
        this.scoreRisco = 0.0;
        this.timestampUltimaTransacao = 0;
        this.totalRecebidoJanelaCentavos = 0;
        this.totalEnviadoJanelaCentavos = 0;
        this.qtdTransacoesJanela = 0;
    }

    // Taxa de retenção: 0.0 = repassou todo o dinheiro (laranja); 1.0 = reteve tudo
    public double calcularTaxaRetencao() {
        if (totalRecebidoJanelaCentavos == 0) return 1.0;
        long retido = totalRecebidoJanelaCentavos - totalEnviadoJanelaCentavos;
        if (retido <= 0) return 0.0;
        return (double) retido / totalRecebidoJanelaCentavos;
    }

    public void registrarEntrada(long valor) {
        this.totalRecebidoJanelaCentavos += valor;
        this.qtdTransacoesJanela++;
    }

    public void registrarSaida(long valor) {
        this.totalEnviadoJanelaCentavos += valor;
        this.qtdTransacoesJanela++;
    }

    public void resetarMetricasJanela() {
        this.totalRecebidoJanelaCentavos = 0;
        this.totalEnviadoJanelaCentavos = 0;
        this.qtdTransacoesJanela = 0;
    }

    // Getters e Setters
    public int getIdConta() { return idConta; }
    public long getSaldoCentavos() { return saldoCentavos; }
    public void setSaldoCentavos(long saldoCentavos) { this.saldoCentavos = saldoCentavos; }
    public long getVolumeMedioDiarioCentavos() { return volumeMedioDiarioCentavos; }
    public double getScoreRisco() { return scoreRisco; }
    public void setScoreRisco(double scoreRisco) { this.scoreRisco = Math.min(100.0, Math.max(0.0, scoreRisco)); }
    public long getTimestampUltimaTransacao() { return timestampUltimaTransacao; }
    public void setTimestampUltimaTransacao(long timestamp) { this.timestampUltimaTransacao = timestamp; }
}