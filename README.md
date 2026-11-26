# 🛡️ Motor Antifraude Pix em Tempo Real (In-Memory)

Motor antifraude bancário preventivo de altíssima velocidade (*In-Memory Computing*), desenvolvido em **Java puro** (sem dependências externas pesadas na camada de decisão), projetado para analisar transferências Pix e TED no milissegundo da submissão — **antes** da efetivação contábil.

Enquanto mecanismos regulatórios pós-transação como o **MED 2.0 (Banco Central)** atuam de forma reativa após a consumação do golpe, este motor opera na borda com **explicabilidade determinística (*Explainable AI*)**, alcançando vazão superior a **250.000 a 1.000.000+ transações/segundo** e latência média na casa de **1 a 5 microssegundos**.

---

## 🏗️ Arquitetura e Estruturas de Dados Fundamentais

Cada transação submetida ao orquestrador (`MotorFraude.java`) atravessa um pipeline de **4 estruturas de dados em memória RAM**:

```text
[ Nova Transação Pix: (Origem, Destino, Valor em Centavos, Timestamp) ]
        │
        ▼
[ 0. Expurgo Temporal ] ───────► Descarta transações e arestas fora da janela ativa
        │
        ▼
[ 1. TreapContas O(log N) ] ───► Árvore BST + Max-Heap: Valida saldo, média diária (μ) e retenção
        │
        ▼
[ 2. JanelaDeslizante O(log K)]► Busca Binária (lowerBound): Detecta Smurfing e Burst (Robôs)
        │
        ▼
[ 3. GrafoTransacional O(b^D) ]► Bounded DFS (D ≤ 4 com 3 Podas) + Graus (Fan-in / Fan-out)
        │
        ├─── Score < 40 ───────────────► ✅ APROVADA  (Efetiva débito/crédito e insere aresta)
        ├─── 40 ≤ Score < 75 ──────────► ⚠️ SUSPEITA  (Efetiva com monitoramento e eleva risco)
        └─── Score ≥ 75 ou Ciclo ──────► 🛑 BLOQUEADA (Aborta débito e pune contas do anel)
        │
        ▼
[ 4. ArvoreMerkle SHA-256 ] ───► Selagem criptográfica de blocos e geração de Merkle Proof O(log K)
```

| Módulo / Classe | Estrutura de Dados / Algoritmo | Complexidade | Responsabilidade no Motor |
| :--- | :--- | :---: | :--- |
| `TreapContas.java` `NoTreap.java` `Conta.java` | **Treap Auto-balanceada** (Árvore Binária de Busca por `idConta` + Max-Heap por `prioridade` aleatória) | $O(\log N)$ | Consulta e atualiza saldos em centavos, volume médio diário ($\mu$), taxa de retenção de saldo ($R$) e score de risco dinâmico ($0\text{ a }100$). |
| `JanelaDeslizante.java` `MetricasJanela.java` | **Buffer Cronológico Ordenado + Busca Binária (`lowerBound`)** | $O(\log K)$ | Isola o intervalo temporal ativo ($[T - \Delta t, T]$), expurga transações expiradas via `subList.clear()` e consolida volume/frequência para detectar *Smurfing* e *Burst*. |
| `GrafoTransacional.java` `ArestaTransacao.java` `ResultadoCiclo.java` | **Grafo Direcionado Dinâmico + Busca em Profundidade Limitada (*Bounded DFS*)** | $O(b^D)$ local ($D \le 4$) | Detecta fechamento de anéis de lavagem ($A \to B \to C \to A$) aplicando **3 Podas** (Profundidade, Causalidade Temporal e Preservação de Valor $\pm 20\%$), além de monitorar *Fan-in* (`In-Degree`) e *Fan-out* (`Out-Degree`) em $O(1)$. |
| `ArvoreMerkle.java` `MerkleProof.java` | **Árvore de Merkle Criptográfica (SHA-256)** | $O(K)$ lote / $O(\log K)$ prova | Garante imutabilidade e não-repúdio do histórico de decisões, permitindo auditoria externa de inclusão com apenas $\approx 20$ hashes para lotes de 1 milhão de transações. |
| `MotorFraude.java` | **Orquestrador Determinístico e Reativo** | $O(\log N + \log K + b^D)$ | Combina as violações das estruturas no Score de Fraude e propaga penalidades de volta para as contas envolvidas na Treap. |

---

## 🚨 Padrões de Fraude Detectados

1. **Validação Contábil de Saldo Insuficiente (`SALDO_INSUFICIENTE`):** Rejeição imediata em $O(\log N)$ na `TreapContas` antes de tocar na janela ou no grafo.
2. **Desvio Abrupto de Perfil (`VALOR_ATIPICO_ACIMA_DA_MEDIA` / `CONTA_ORIGEM_RISCO_ELEVADO`):** Dispara quando o valor da transação excede $3\times$ a média diária histórica da conta ou quando a conta já possui histórico de risco elevado.
3. **Fracionamento / Smurfing (`SMURFING_FRACIONAMENTO_DETECTADO`):** Identifica tentativas de burlar o teto de notificação fiscal do COAF (ex: 5 transferências seguidas de R$ 9.600,00 totalizando R$ 48.000,00 em menos de 10 minutos, superando o limiar de $\ge 4$ transações e $\ge \text{R\$ } 25.000,00$).
4. **Explosão de Velocidade Temporal / Burst (`EXPLOSAO_VELOCIDADE_TEMPORAL`):** Detecta automação criminosa (scripts/bots) ao identificar $\ge 4$ disparos da mesma conta em uma janela curta de 60 segundos.
5. **Anéis de Lavagem de Dinheiro (`ANEL_LAVAGEM_CICLO_DETECTADO`):** Quando uma transação $C \to A$ tenta ocorrer, a *Bounded DFS* verifica se já existe caminho válido $A \to \dots \to C$ com timestamps crescentes ($t_1 \le t_2 \le t_3$) e conservação de capital ($\pm 20\%$). Se confirmado, bloqueia imediatamente a transação e eleva o risco de todas as contas do anel para $\ge 85.0$.
6. **Funil de Concentração em Conta Laranja (`FAN_IN_CONCENTRACAO_DETECTADA`):** Monitora o grau de entrada (`In-Degree >= 4`) na conta destino para flagrar contas arrecadadoras (*Mule Accounts*).
7. **Pulverização em Leque (`FAN_OUT_DISPERSAO_ELEVADA`):** Monitora o grau de saída (`Out-Degree >= 5`) para detectar evasão rápida de fundos.

---

## 🖥️ Bancada Visual Interativa & Modo Apresentação (`ServidorVisual.java`)

O projeto inclui um servidor HTTP nativo (`com.sun.net.httpserver.HttpServer`) na porta `8080` sem necessidade de servidores externos:

- **Dashboard & Simulador Passo a Passo (`http://localhost:8080`):**
  - `0. Pipeline`: Animação passo a passo de transações legítimas vs. fraudulentas.
  - `1. Treap (Contas)`: Inspeção em tempo real dos saldos, scores de risco e taxas de retenção.
  - `2. Janela: Smurfing`: Simulação do ataque de fracionamento com busca binária `lowerBound`.
  - `3. Janela: Velocity`: Simulação de rajada de alta frequência (Burst).
  - `4. Grafo: Bounded DFS`: Visualização da pilha da DFS fechando e bloqueando o anel `20 → 30 → 40 → 20`.
  - `5. Grafo: In-Degree`: Visualização do funil de concentração *Fan-in* na Conta 80.
  - `6. Stress Test ⚡`: Benchmark ao vivo executando 50.000+ transações em memória.
  - `7. Lab de Limites 🔬`: Bancada experimental para variar a profundidade da DFS ($D_{\max}$ de 2 a 8), tamanho da janela ($\Delta t$ de 1 min a 24h), tolerância de valor ($\pm 5\%$ a $\pm 50\%$) e tamanho do anel injetado.
- **Apresentação Acadêmica Integrada (`http://localhost:8080/apresentacao`):**
  - Deck interativo de 8 slides (`apresentacao.html`) com simulador de adulteração SHA-256 (Efeito Avalanche na Árvore de Merkle) e links diretos para cada cenário do simulador.

---

## 🚀 Como Compilar e Executar

Pré-requisito: **Java JDK 11+** instalado.

### 1. Compilar todos os módulos
```bash
javac *.java
```

### 2. Iniciar o Servidor Visual e a Apresentação
```bash
java ServidorVisual
```
- Acesse o **Simulador Ao Vivo**: [http://localhost:8080](http://localhost:8080)
- Acesse os **Slides da Apresentação**: [http://localhost:8080/apresentacao](http://localhost:8080/apresentacao)

### 3. Executar a Suíte de Testes Automatizados (11 Cenários)
```bash
java SuiteTestes
```

### 4. Executar o Laboratório de Exploração de Limites e Trade-offs
```bash
java LaboratorioLimites
```

### 5. Executar o Benchmark de Escalabilidade (Até 500.000 Transações)
```bash
java BenchmarkMotor
```

### 6. Executar a Demonstração de Auditoria com Merkle Proof
```bash
java DemoMerkleProof
```

---

## 📊 Resultados de Benchmark e Exploração de Limites

| Volume de Transações | Tempo Total | Vazão (Throughput) | Latência Média | Latência P99 |
| :---: | :---: | :---: | :---: | :---: |
| `1.000` | `~2.7 ms` | `> 350.000 tx/s` | `~2.5 µs` | `~15.5 µs` |
| `10.000` | `~15.8 ms` | `> 600.000 tx/s` | `~1.5 µs` | `~6.2 µs` |
| `100.000` | `~85.4 ms` | `> 1.100.000 tx/s` | `~0.8 µs` | `~3.5 µs` |
| `500.000` | `~285.8 ms` | `> 1.700.000 tx/s` | `~0.5 µs` | `~1.0 µs` |

### Descobertas do Laboratório de Limites (`LaboratorioLimites.java`)
- **Limite da Bounded DFS ($O(b^D)$):** Com $D_{\max} = 4$, anéis longos de 6 contas (5 saltos reversos) resultam em *Falso Negativo*. Ao ajustar para $D_{\max} = 5$, o motor intercepta 100% dos anéis de 6 contas mantendo latência média próxima de $3\text{ }\mu\text{s}$.
- **Limite da Janela Deslizante ($\Delta t$):** Ataques de *Smurfing lento* (parcelas espaçadas a cada 12 minutos) expiram na janela padrão de 10 minutos, mas são capturados ao ampliar a janela para 1 hora (`60 min`), evidenciando o *trade-off* de memória no buffer ativo.
- **Sensibilidade da Poda de Valor ($\pm\%$):** Tolerâncias estreitas ($\pm 5\%$ a $\pm 10\%$) sofrem *Falsos Negativos* quando laranjas retêm comissões de $12\%$ por salto, enquanto tolerâncias abertas ($\pm 50\%$) reduzem a eficácia da poda e elevam *Falsos Positivos*.
