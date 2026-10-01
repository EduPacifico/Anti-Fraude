import java.util.List;

public class ResultadoCiclo {
    private final boolean cicloDetectado;
    private final List<Integer> caminhoContas;
    private final String descricao;

    public ResultadoCiclo(boolean cicloDetectado, List<Integer> caminhoContas, String descricao) {
        this.cicloDetectado = cicloDetectado;
        this.caminhoContas = caminhoContas;
        this.descricao = descricao;
    }

    public boolean isCicloDetectado() { return cicloDetectado; }
    public List<Integer> getCaminhoContas() { return caminhoContas; }
    public String getDescricao() { return descricao; }

    @Override
    public String toString() {
        if (!cicloDetectado) {
            return "[CICLO NÃO DETECTADO] Transação regular.";
        }
        return String.format("🚨 [ALERTA DE CICLO DE LAVAGEM] %s | Caminho: %s", 
                descricao, caminhoContas.toString());
    }
}