import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GrafoTransacional {
    // Lista de adjacência: idContaOrigem -> Lista de Arestas que saem dela
    private final Map<Integer, List<ArestaTransacao>> adjacencias;
    
    // Contadores de grau para checagens de Fan-in e Fan-out
    private final Map<Integer, Integer> grausEntrada;

    public GrafoTransacional() {
        this.adjacencias = new HashMap<>();
        this.grausEntrada = new HashMap<>();
    }

    // =========================================================================
    // 1. Inserção de Arestas no Grafo
    // =========================================================================

    public void adicionarAresta(Transacao tx) {
        adjacencias.putIfAbsent(tx.getIdContaOrigem(), new ArrayList<>());
        adjacencias.get(tx.getIdContaOrigem()).add(
            new ArestaTransacao(tx.getId(), tx.getIdContaDestino(), tx.getValorCentavos(), tx.getTimestamp())
        );

        // Atualiza grau de entrada da conta destino
        grausEntrada.put(tx.getIdContaDestino(), grausEntrada.getOrDefault(tx.getIdContaDestino(), 0) + 1);
    }

    // =========================================================================
    // 2. Limpeza de Arestas Expiradas (Sincronizado com a Janela Deslizante)
    // =========================================================================

    public void expurgarArestasExpiradas(long timestampAtual, long duracaoJanelaMs) {
        long timestampCorte = timestampAtual - duracaoJanelaMs;

        for (Map.Entry<Integer, List<ArestaTransacao>> entry : adjacencias.entrySet()) {
            List<ArestaTransacao> arestas = entry.getValue();
            Iterator<ArestaTransacao> iterator = arestas.iterator();

            while (iterator.hasNext()) {
                ArestaTransacao aresta = iterator.next();
                if (aresta.getTimestamp() < timestampCorte) {
                    // Decrementa grau de entrada do destino
                    int destino = aresta.getIdContaDestino();
                    int grauAtual = grausEntrada.getOrDefault(destino, 1);
                    if (grauAtual <= 1) {
                        grausEntrada.remove(destino);
                    } else {
                        grausEntrada.put(destino, grauAtual - 1);
                    }
                    iterator.remove();
                }
            }
        }
    }

    // =========================================================================
    // 3. Detecção de Ciclos com Bounded DFS (Profundidade Limitada)
    // =========================================================================

    /**
     * Verifica se ao adicionar a transação (origem -> destino) fecharemos um ciclo.
     * Para isso, busca se já existe um caminho ativo de 'destino' até 'origem'.
     */
    public ResultadoCiclo verificarCiclo(int origem, int destino, long valorCentavos, 
                                        long timestampAtual, int profundidadeMaxima, double toleranciaValor) {
        List<Integer> caminhoAtual = new ArrayList<>();
        Set<Integer> visitados = new HashSet<>();
        
        caminhoAtual.add(destino);

        boolean cicloEncontrado = dfsLimitada(
            destino, 
            origem, 
            valorCentavos, 
            0L, 
            timestampAtual, 
            profundidadeMaxima, 
            toleranciaValor, 
            visitados, 
            caminhoAtual
        );

        if (cicloEncontrado) {
            caminhoAtual.add(destino); // Fecha visualmente o ciclo
            return new ResultadoCiclo(true, caminhoAtual, "Ciclo fechado com causalidade temporal confirmada");
        }

        return new ResultadoCiclo(false, null, "Nenhum ciclo encontrado");
    }

    private boolean dfsLimitada(int noAtual, int alvo, long valorReferencia, long ultimoTimestamp, 
                                long timestampTransacaoAtual, int profundidadeRestante, double toleranciaValor,
                                Set<Integer> visitados, List<Integer> caminho) {
        // Condição de sucesso: alcançou o nó alvo
        if (noAtual == alvo && caminho.size() > 1) {
            return true;
        }

        // Condição de parada: limite de saltos esgotado
        if (profundidadeRestante == 0) {
            return false;
        }

        visitados.add(noAtual);

        List<ArestaTransacao> arestasSaindo = adjacencias.getOrDefault(noAtual, new ArrayList<>());

        for (ArestaTransacao aresta : arestasSaindo) {
            int proximoNo = aresta.getIdContaDestino();

            // Poda 1: Evita revisitar nós dentro do mesmo ramo (exceto se for o alvo final)
            if (visitados.contains(proximoNo) && proximoNo != alvo) {
                continue;
            }

            // Poda 2: Causalidade temporal (t_aresta deve ser posterior à aresta anterior e anterior à transação atual)
            if (aresta.getTimestamp() < ultimoTimestamp || aresta.getTimestamp() > timestampTransacaoAtual) {
                continue;
            }

            // Poda 3: Preservação de valor monetário (taxas de lavagem toleradas, ex: ±20%)
            long limiteMinimo = (long) (valorReferencia * (1.0 - toleranciaValor));
            long limiteMaximo = (long) (valorReferencia * (1.0 + toleranciaValor));
            if (aresta.getValorCentavos() < limiteMinimo || aresta.getValorCentavos() > limiteMaximo) {
                continue;
            }

            caminho.add(proximoNo);

            boolean encontrou = dfsLimitada(
                proximoNo, 
                alvo, 
                aresta.getValorCentavos(), 
                aresta.getTimestamp(), 
                timestampTransacaoAtual, 
                profundidadeRestante - 1, 
                toleranciaValor, 
                visitados, 
                caminho
            );

            if (encontrou) {
                return true;
            }

            caminho.remove(caminho.size() - 1); // Backtracking
        }

        visitados.remove(noAtual);
        return false;
    }

    // =========================================================================
    // 4. Métricas Topológicas (Fan-in e Fan-out)
    // =========================================================================

    public int getGrauSaida(int idConta) {
        return adjacencias.getOrDefault(idConta, new ArrayList<>()).size();
    }

    public int getGrauEntrada(int idConta) {
        return grausEntrada.getOrDefault(idConta, 0);
    }
}