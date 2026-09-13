import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ArvoreMerkle {
    private final List<Transacao> transacoes;
    private final List<List<String>> niveis;
    private String raizMerkle;

    public ArvoreMerkle(List<Transacao> transacoes) {
        this.transacoes = new ArrayList<>(transacoes);
        this.niveis = new ArrayList<>();
        construirArvore();
    }

    private void construirArvore() {
        if (transacoes == null || transacoes.isEmpty()) {
            this.raizMerkle = "0".repeat(64);
            return;
        }

        // Nível 0: Hashes individuais das folhas
        List<String> folhas = new ArrayList<>();
        for (Transacao tx : transacoes) {
            folhas.add(calcularHashTransacao(tx));
        }
        niveis.add(folhas);

        // Constrói os níveis superiores aos pares até restar apenas a raiz
        List<String> nivelAtual = folhas;
        while (nivelAtual.size() > 1) {
            List<String> proximoNivel = new ArrayList<>();

            for (int i = 0; i < nivelAtual.size(); i += 2) {
                String esquerda = nivelAtual.get(i);
                String direita = (i + 1 < nivelAtual.size()) ? nivelAtual.get(i + 1) : esquerda; // Duplicação para ímpares
                proximoNivel.add(calcularHash(esquerda + direita));
            }

            niveis.add(proximoNivel);
            nivelAtual = proximoNivel;
        }

        this.raizMerkle = nivelAtual.get(0);
    }

    /**
     * Gera a prova criptográfica de inclusão O(log N) para uma transação.
     */
    public MerkleProof gerarProva(long idTransacao) {
        int indiceFolha = -1;
        for (int i = 0; i < transacoes.size(); i++) {
            if (transacoes.get(i).getId() == idTransacao) {
                indiceFolha = i;
                break;
            }
        }

        if (indiceFolha == -1) {
            return null; // Transação não pertence a este bloco
        }

        String hashFolha = niveis.get(0).get(indiceFolha);
        MerkleProof prova = new MerkleProof(idTransacao, hashFolha, this.raizMerkle);

        int indiceAtual = indiceFolha;
        for (int k = 0; k < niveis.size() - 1; k++) {
            List<String> nivelK = niveis.get(k);
            boolean ehEsquerda = (indiceAtual % 2 == 0);

            if (ehEsquerda) {
                // O irmão é o próximo nó à direita (se existir) ou ele mesmo se for ímpar
                int indiceIrmao = (indiceAtual + 1 < nivelK.size()) ? (indiceAtual + 1) : indiceAtual;
                prova.adicionarPasso(nivelK.get(indiceIrmao), MerkleProof.PosicaoIrmao.DIREITA);
            } else {
                // O irmão é o nó anterior à esquerda
                int indiceIrmao = indiceAtual - 1;
                prova.adicionarPasso(nivelK.get(indiceIrmao), MerkleProof.PosicaoIrmao.ESQUERDA);
            }

            indiceAtual /= 2; // Sobe para o índice correspondente no pai
        }

        return prova;
    }

    public static String calcularHashTransacao(Transacao tx) {
        String payload = String.format("%d:%d:%d:%d:%d:%s",
                tx.getId(),
                tx.getIdContaOrigem(),
                tx.getIdContaDestino(),
                tx.getValorCentavos(),
                tx.getTimestamp(),
                tx.getStatus().name());
        return calcularHash(payload);
    }

    public static String calcularHash(String dados) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dados.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 não suportado", e);
        }
    }

    public String getRaizMerkle() { return raizMerkle; }
    public int getAltura() { return niveis.size(); }
}