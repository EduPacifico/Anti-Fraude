public class TesteTreap {
    public static void main(String[] args) {
        TreapContas treap = new TreapContas(12345L);

        System.out.println("=== 1. Inserindo Contas na Treap ===");
        // Inserção em ordem sequencial (em uma BST comum a altura seria 10)
        for (int id = 1; id <= 10; id++) {
            Conta conta = new Conta(id, 5000_00L, 1000_00L);
            treap.inserir(conta);
        }

        System.out.println("Total de contas cadastradas: " + treap.getQuantidadeContas());
        System.out.println("Altura da Treap (balanceada): " + treap.calcularAltura());

        System.out.println("\n=== 2. Buscando e Atualizando Score de Risco em O(log N) ===");
        Conta contaAlvo = treap.buscar(7);
        if (contaAlvo != null) {
            System.out.println("Conta encontrada: ID " + contaAlvo.getIdConta() + " | Saldo: R$ " + (contaAlvo.getSaldoCentavos() / 100.0));
            
            // Simula detecção de atividade suspeita
            contaAlvo.setScoreRisco(85.5);
            System.out.println("Score atualizado para: " + contaAlvo.getScoreRisco());
        }

        // Atualiza outra conta
        treap.obterOuCriar(3, 1000_00L, 200_00L).setScoreRisco(92.0);

        System.out.println("\n=== 3. Listando Contas com Risco Elevado (>= 80.0) ===");
        for (Conta suspeita : treap.buscarContasComRiscoElevado(80.0)) {
            System.out.println("🚨 ALERTA: Conta ID " + suspeita.getIdConta() + " com Score de Risco = " + suspeita.getScoreRisco());
        }

        System.out.println("\n=== 4. Testando Remoção em O(log N) ===");
        treap.remover(7);
        System.out.println("Conta ID 7 ainda existe? " + (treap.contem(7) ? "SIM" : "NÃO"));
        System.out.println("Total de contas após remoção: " + treap.getQuantidadeContas());
    }
}