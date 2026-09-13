import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MerkleProof {
    public enum PosicaoIrmao {
        ESQUERDA, DIREITA
    }

    public static class ElementoProva {
        private final String hashIrmao;
        private final PosicaoIrmao posicao;

        public ElementoProva(String hashIrmao, PosicaoIrmao posicao) {
            this.hashIrmao = hashIrmao;
            this.posicao = posicao;
        }

        public String getHashIrmao() { return hashIrmao; }
        public PosicaoIrmao getPosicao() { return posicao; }

        @Override
        public String toString() {
            return String.format("[%s] %s", posicao, hashIrmao.substring(0, 16) + "...");
        }
    }

    private final long idTransacao;
    private final String hashFolha;
    private final String raizEsperada;
    private final List<ElementoProva> caminho;

    public MerkleProof(long idTransacao, String hashFolha, String raizEsperada) {
        this.idTransacao = idTransacao;
        this.hashFolha = hashFolha;
        this.raizEsperada = raizEsperada;
        this.caminho = new ArrayList<>();
    }

    public void adicionarPasso(String hashIrmao, PosicaoIrmao posicao) {
        caminho.add(new ElementoProva(hashIrmao, posicao));
    }

    public long getIdTransacao() { return idTransacao; }
    public String getHashFolha() { return hashFolha; }
    public String getRaizEsperada() { return raizEsperada; }
    public List<ElementoProva> getCaminho() { return caminho; }

    /**
     * Valida de forma independente se a prova reconstrói exatamente a raiz esperada.
     */
    public boolean verificar() {
        String hashAtual = this.hashFolha;

        for (ElementoProva passo : caminho) {
            if (passo.getPosicao() == PosicaoIrmao.DIREITA) {
                // O irmão está à direita: Hash(Atual + Irmão)
                hashAtual = calcularSha256(hashAtual + passo.getHashIrmao());
            } else {
                // O irmão está à esquerda: Hash(Irmão + Atual)
                hashAtual = calcularSha256(passo.getHashIrmao() + hashAtual);
            }
        }

        return hashAtual.equalsIgnoreCase(this.raizEsperada);
    }

    private static String calcularSha256(String entrada) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(entrada.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao carregar SHA-256", e);
        }
    }
}