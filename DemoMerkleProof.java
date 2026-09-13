import java.util.ArrayList;
import java.util.List;

public class DemoMerkleProof {
    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("            DEMONSTRACAO DE GERACAO E AUDITORIA DE MERKLE PROOF (SHA-256)                ");
        System.out.println("=========================================================================================\n");

        // 1. Criação de um bloco selado com 8 transações
        List<Transacao> lote = new ArrayList<>();
        lote.add(new Transacao(101L, 10, 20, 150_00L, 1000L));
        lote.add(new Transacao(102L, 15, 30, 9_600_00L, 1015L));
        lote.add(new Transacao(103L, 20, 40, 500_00L, 1030L));
        lote.add(new Transacao(104L, 25, 80, 8_000_00L, 1045L)); // Transação que será auditada
        lote.add(new Transacao(105L, 30, 50, 1_200_00L, 1060L));
        lote.add(new Transacao(106L, 40, 60, 450_00L, 1075L));
        lote.add(new Transacao(107L, 50, 70, 3_000_00L, 1090L));
        lote.add(new Transacao(108L, 60, 80, 250_00L, 1105L));

        ArvoreMerkle arvore = new ArvoreMerkle(lote);

        System.out.println("📦 Bloco Criptografico Selado:");
        System.out.println(" • Total de Transacoes no Bloco: " + lote.size());
        System.out.println(" • Altura da Arvore: " + arvore.getAltura() + " niveis");
        System.out.println(" • Raiz de Merkle Oficial (Root): " + arvore.getRaizMerkle() + "\n");

        // 2. Geração da Prova para a Tx #104
        long idAlvo = 104L;
        System.out.println("🔍 Solicitando Prova de Inclusao para a Transacao #" + idAlvo + "...");
        MerkleProof prova = arvore.gerarProva(idAlvo);

        System.out.println(" • Hash da Folha (Tx #" + idAlvo + "): " + prova.getHashFolha());
        System.out.println(" • Caminho de Auditoria O(log N) (" + prova.getCaminho().size() + " hashes irmaos):");
        for (int i = 0; i < prova.getCaminho().size(); i++) {
            System.out.printf("    Nivel %d -> %s\n", i + 1, prova.getCaminho().get(i));
        }
        System.out.println();

        // 3. Auditoria Independente pelo Cliente/Auditor
        System.out.print("🛡️ Executando verificacao criptografica da prova... ");
        boolean valida = prova.verificar();
        System.out.println(valida ? "✅ APROVADA (Integridade e Inclusao Confirmadas)" : "❌ FALHOU");

        // 4. Teste de Tentativa de Fraude (Adulteração de dados)
        System.out.println("\n🚨 Simulando tentativa de fraude (adulterando valor da Tx #104 de R$ 8.000 para R$ 800)...");
        Transacao txAdulterada = new Transacao(104L, 25, 80, 800_00L, 1045L);
        String hashAdulterado = ArvoreMerkle.calcularHashTransacao(txAdulterada);

        MerkleProof provaFalsa = new MerkleProof(104L, hashAdulterado, arvore.getRaizMerkle());
        for (MerkleProof.ElementoProva p : prova.getCaminho()) {
            provaFalsa.adicionarPasso(p.getHashIrmao(), p.getPosicao());
        }

        System.out.print("🛡️ Executando verificacao da transacao adulterada... ");
        boolean validaFraude = provaFalsa.verificar();
        System.out.println(validaFraude ? "✅ APROVADA" : "❌ REJEITADA COM SUCESSO (Assinatura do bloco incompativel)");

        System.out.println("\n=========================================================================================");
    }
}