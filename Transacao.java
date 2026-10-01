public class Transacao {
    private final long id;
    private final int idContaOrigem;
    private final int idContaDestino;
    private final long valorCentavos; // Ex: R$ 150,50 -> 15050L
    private final long timestamp;     // Carimbo de tempo em milissegundos

    private double scoreFraude;
    private StatusTransacao status;
    private String motivoFraude;

    public Transacao(long id, int idContaOrigem, int idContaDestino, long valorCentavos, long timestamp) {
        this.id = id;
        this.idContaOrigem = idContaOrigem;
        this.idContaDestino = idContaDestino;
        this.valorCentavos = valorCentavos;
        this.timestamp = timestamp;
        this.scoreFraude = 0.0;
        this.status = StatusTransacao.PENDENTE;
        this.motivoFraude = "NENHUM";
    }

    // Serialização em formato de texto para alimentar o cálculo de Hash da Árvore de Merkle
    public String serializarParaHash() {
        return id + "|" + idContaOrigem + "|" + idContaDestino + "|" 
             + valorCentavos + "|" + timestamp + "|" + status.name();
    }

    // Getters e Setters
    public long getId() { return id; }
    public int getIdContaOrigem() { return idContaOrigem; }
    public int getIdContaDestino() { return idContaDestino; }
    public long getValorCentavos() { return valorCentavos; }
    public long getTimestamp() { return timestamp; }
    public double getScoreFraude() { return scoreFraude; }
    public void setScoreFraude(double scoreFraude) { this.scoreFraude = scoreFraude; }
    public StatusTransacao getStatus() { return status; }
    public void setStatus(StatusTransacao status) { this.status = status; }
    public String getMotivoFraude() { return motivoFraude; }
    public void setMotivoFraude(String motivoFraude) { this.motivoFraude = motivoFraude; }

    @Override
    public String toString() {
        return String.format("Tx[#%d | Conta %d -> Conta %d | R$ %.2f | %s]",
                id, idContaOrigem, idContaDestino, valorCentavos / 100.0, status);
    }
}