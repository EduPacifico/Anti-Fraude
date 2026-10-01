import java.util.Random;

public class NoTreap {
    private Conta conta;
    private final int prioridade; // Valor aleatório que obedece à propriedade de Max-Heap
    private NoTreap esquerda;
    private NoTreap direita;

    public NoTreap(Conta conta, Random random) {
        this.conta = conta;
        this.prioridade = random.nextInt(Integer.MAX_VALUE);
        this.esquerda = null;
        this.direita = null;
    }

    // Getters e Setters
    public Conta getConta() { return conta; }
    public void setConta(Conta conta) { this.conta = conta; }
    public int getPrioridade() { return prioridade; }
    public NoTreap getEsquerda() { return esquerda; }
    public void setEsquerda(NoTreap esquerda) { this.esquerda = esquerda; }
    public NoTreap getDireita() { return direita; }
    public void setDireita(NoTreap direita) { this.direita = direita; }
}