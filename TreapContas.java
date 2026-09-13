import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TreapContas {
    private NoTreap raiz;
    private final Random random;
    private int quantidadeContas;

    public TreapContas() {
        this.raiz = null;
        this.random = new Random();
        this.quantidadeContas = 0;
    }

    // Construtor com seed fixa para testes e benchmarks reproduzíveis
    public TreapContas(long seed) {
        this.raiz = null;
        this.random = new Random(seed);
        this.quantidadeContas = 0;
    }

    // ==========================================
    // Rotações para manter a propriedade de Heap
    // ==========================================

    private NoTreap rotacionarDireita(NoTreap y) {
        NoTreap x = y.getEsquerda();
        NoTreap b = x.getDireita();

        // Realiza a rotação
        x.setDireita(y);
        y.setEsquerda(b);

        return x; // Nova raiz do sub-árvore
    }

    private NoTreap rotacionarEsquerda(NoTreap x) {
        NoTreap y = x.getDireita();
        NoTreap b = y.getEsquerda();

        // Realiza a rotação
        y.setEsquerda(x);
        x.setDireita(b);

        return y; // Nova raiz da sub-árvore
    }

    // ==========================================
    // Inserção em O(log N)
    // ==========================================

    public void inserir(Conta conta) {
        if (conta == null) return;
        this.raiz = inserirRecursivo(this.raiz, conta);
    }

    private NoTreap inserirRecursivo(NoTreap no, Conta conta) {
        if (no == null) {
            quantidadeContas++;
            return new NoTreap(conta, random);
        }

        int idNova = conta.getIdConta();
        int idAtual = no.getConta().getIdConta();

        if (idNova < idAtual) {
            no.setEsquerda(inserirRecursivo(no.getEsquerda(), conta));
            // Restaura o Max-Heap se a prioridade do filho for maior que a do pai
            if (no.getEsquerda().getPrioridade() > no.getPrioridade()) {
                no = rotacionarDireita(no);
            }
        } else if (idNova > idAtual) {
            no.setDireita(inserirRecursivo(no.getDireita(), conta));
            // Restaura o Max-Heap
            if (no.getDireita().getPrioridade() > no.getPrioridade()) {
                no = rotacionarEsquerda(no);
            }
        } else {
            // Se a conta já existe, atualiza os dados
            no.setConta(conta);
        }

        return no;
    }

    // ==========================================
    // Busca em O(log N)
    // ==========================================

    public Conta buscar(int idConta) {
        NoTreap no = buscarRecursivo(this.raiz, idConta);
        return (no != null) ? no.getConta() : null;
    }

    private NoTreap buscarRecursivo(NoTreap no, int idConta) {
        if (no == null || no.getConta().getIdConta() == idConta) {
            return no;
        }

        if (idConta < no.getConta().getIdConta()) {
            return buscarRecursivo(no.getEsquerda(), idConta);
        } else {
            return buscarRecursivo(no.getDireita(), idConta);
        }
    }

    public boolean contem(int idConta) {
        return buscar(idConta) != null;
    }

    /**
     * Busca a conta pelo ID; se não existir, cria e insere uma nova com valores padrão.
     * Operação chave para o fluxo contínuo de transações.
     */
    public Conta obterOuCriar(int idConta, long saldoInicialCentavos, long volumeMedioDiarioCentavos) {
        Conta conta = buscar(idConta);
        if (conta == null) {
            conta = new Conta(idConta, saldoInicialCentavos, volumeMedioDiarioCentavos);
            inserir(conta);
        }
        return conta;
    }

    // ==========================================
    // Remoção em O(log N)
    // ==========================================

    public void remover(int idConta) {
        if (contem(idConta)) {
            this.raiz = removerRecursivo(this.raiz, idConta);
            quantidadeContas--;
        }
    }

    private NoTreap removerRecursivo(NoTreap no, int idConta) {
        if (no == null) return null;

        int idAtual = no.getConta().getIdConta();

        if (idConta < idAtual) {
            no.setEsquerda(removerRecursivo(no.getEsquerda(), idConta));
        } else if (idConta > idAtual) {
            no.setDireita(removerRecursivo(no.getDireita(), idConta));
        } else {
            // Caso 1: Nó folha
            if (no.getEsquerda() == null && no.getDireita() == null) {
                return null;
            }
            // Caso 2: Possui apenas um filho
            else if (no.getEsquerda() == null) {
                return no.getDireita();
            } else if (no.getDireita() == null) {
                return no.getEsquerda();
            }
            // Caso 3: Possui dois filhos -> rotaciona o filho de maior prioridade para cima
            else {
                if (no.getEsquerda().getPrioridade() > no.getDireita().getPrioridade()) {
                    no = rotacionarDireita(no);
                    no.setDireita(removerRecursivo(no.getDireita(), idConta));
                } else {
                    no = rotacionarEsquerda(no);
                    no.setEsquerda(removerRecursivo(no.getEsquerda(), idConta));
                }
            }
        }
        return no;
    }

    // ==========================================
    // Métodos Utilitários e Auditoria
    // ==========================================

    // Retorna todas as contas com score de risco crítico (>= scoreMinimo)
    public List<Conta> buscarContasComRiscoElevado(double scoreMinimo) {
        List<Conta> suspeitas = new ArrayList<>();
        coletarSuspeitasRecursivo(this.raiz, scoreMinimo, suspeitas);
        return suspeitas;
    }

    private void coletarSuspeitasRecursivo(NoTreap no, double scoreMinimo, List<Conta> resultado) {
        if (no == null) return;
        coletarSuspeitasRecursivo(no.getEsquerda(), scoreMinimo, resultado);
        if (no.getConta().getScoreRisco() >= scoreMinimo) {
            resultado.add(no.getConta());
        }
        coletarSuspeitasRecursivo(no.getDireita(), scoreMinimo, resultado);
    }

    // Calcula a altura máxima da árvore para checar o balanceamento nos benchmarks
    public int calcularAltura() {
        return calcularAlturaRecursivo(this.raiz);
    }

    private int calcularAlturaRecursivo(NoTreap no) {
        if (no == null) return 0;
        return 1 + Math.max(calcularAlturaRecursivo(no.getEsquerda()), 
                            calcularAlturaRecursivo(no.getDireita()));
    }

    public int getQuantidadeContas() {
        return quantidadeContas;
    }
}