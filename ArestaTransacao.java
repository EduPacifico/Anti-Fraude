public class ArestaTransacao {
    private final long idTransacao;
    private final int idContaDestino;
    private final long valorCentavos;
    private final long timestamp;

    public ArestaTransacao(long idTransacao, int idContaDestino, long valorCentavos, long timestamp) {
        this.idTransacao = idTransacao;
        this.idContaDestino = idContaDestino;
        this.valorCentavos = valorCentavos;
        this.timestamp = timestamp;
    }

    public long getIdTransacao() { return idTransacao; }
    public int getIdContaDestino() { return idContaDestino; }
    public long getValorCentavos() { return valorCentavos; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("-> [Conta %d | R$ %.2f | t=%d]", 
                idContaDestino, valorCentavos / 100.0, timestamp);
    }
}