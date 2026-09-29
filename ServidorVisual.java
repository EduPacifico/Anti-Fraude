import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServidorVisual {
    private static MotorFraude motor;
    private static final int[] CONTAS_SISTEMA = {10, 15, 20, 25, 30, 40, 50, 60, 70, 80};
    private static final List<Transacao> historicoDecisoes = new ArrayList<>();
    
    private static int passoSmurfingAtual = 0;
    private static int passoCicloAtual = 0;
    private static int passoFanInAtual = 0;
    private static int passoBurstAtual = 0;
    private static int passoFluxoAtual = 0; 
    
    private static final int[] ORIGENS_FAN_IN = {10, 15, 20, 25, 30};
    private static final int[] DESTINOS_BURST = {50, 60, 70, 80, 10, 30};
    
    private static String ultimaMerkleRoot = "Nenhum bloco selado ainda";
    private static int totalBlocosSelados = 0;
    private static long contadorId = 1000L;
    private static long timestampAtual = System.currentTimeMillis();

    public static void main(String[] args) throws IOException {
        reiniciarSistema();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/", new PaginaPrincipalHandler());
        server.createContext("/api/fluxo/passo", new FluxoPassoHandler());
        server.createContext("/api/fluxo/reset", new ResetGeralHandler());
        
        server.createContext("/api/smurfing/passo", new SmurfingPassoHandler());
        server.createContext("/api/smurfing/reset", new ResetGeralHandler());
        
        server.createContext("/api/ciclo/passo", new CicloPassoHandler());
        server.createContext("/api/ciclo/reset", new ResetGeralHandler());
        
        server.createContext("/api/fanin/passo", new FanInPassoHandler());
        server.createContext("/api/fanin/reset", new ResetGeralHandler());
        
        server.createContext("/api/burst/passo", new BurstPassoHandler());
        server.createContext("/api/burst/reset", new ResetGeralHandler());
        
        server.createContext("/api/contas/todas", new ListarTodasContasHandler());

        server.setExecutor(null);

        System.out.println("=================================================================");
        System.out.println(" Servidor Visual Iniciado (Proteção Anti-Cache Ativada)!");
        System.out.println(" Acesse no navegador: http://localhost:8080");
        System.out.println(" Lembre-se de dar Ctrl+F5 no navegador!");
        System.out.println("=================================================================");
        server.start();
    }

    private static void reiniciarSistema() {
        long dezMinutosMs = 10 * 60 * 1000L;
        motor = new MotorFraude(dezMinutosMs);
        historicoDecisoes.clear();
        timestampAtual = System.currentTimeMillis();
        contadorId = 1000L;
        passoSmurfingAtual = 0;
        passoCicloAtual = 0;
        passoFanInAtual = 0;
        passoBurstAtual = 0;
        passoFluxoAtual = 0;
        ultimaMerkleRoot = "Nenhum bloco selado ainda";
        totalBlocosSelados = 0;

        for (int id : CONTAS_SISTEMA) {
            motor.getTreapContas().obterOuCriar(id, 100_000_00L, 2_000_00L);
        }
    }

    // Método que estava faltando e causou o erro!
    private static void checarSelagemMerkle() {
        if (historicoDecisoes.size() > 0 && historicoDecisoes.size() % 10 == 0) {
            int inicio = historicoDecisoes.size() - 10;
            List<Transacao> lote = historicoDecisoes.subList(inicio, historicoDecisoes.size());
            ArvoreMerkle arvore = new ArvoreMerkle(lote);
            ultimaMerkleRoot = arvore.getRaizMerkle();
            totalBlocosSelados++;
        }
    }

    // =========================================================================
    // HTML / JS / CSS 
    // =========================================================================
    static class PaginaPrincipalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<!DOCTYPE html>\n" +
            "<html lang=\"pt-BR\" data-theme=\"dark\">\n" +
            "<head>\n" +
            "  <meta charset=\"UTF-8\">\n" +
            "  <title>Motor Antifraude | Bancada de Testes</title>\n" +
            "  <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n" +
            "  <link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n" +
            "  <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css2?family=Archivo:wght@500;600;700&family=IBM+Plex+Mono:wght@400;500;600&family=IBM+Plex+Sans:wght@400;500;600&display=swap\">\n" +
            "  <style>\n" +
            "    :root {\n" +
            "      --ground: #E9EEF3; --surface: #FFFFFF; --surface-2: #F3F7FA; --stage: #FBFCFE;\n" +
            "      --ink: #0F1820; --ink-2: #4B5A69; --ink-3: #7B8B9B; --line: #D3DCE5; --line-soft: #E6ECF2;\n" +
            "      --accent: #2B4FC7; --accent-ink: #FFFFFF; --accent-soft: #E3E9FA;\n" +
            "      --done: #0F7D63; --compare: #C97A16; --swap: #CE3050; --pivot: #8E4090;\n" +
            "      --shadow: 0 1px 2px rgba(15,24,32,.06), 0 8px 24px rgba(15,24,32,.06);\n" +
            "      --radius: 10px; --radius-sm: 6px;\n" +
            "      --font-display: \"Archivo\", sans-serif; --font-body: \"IBM Plex Sans\", sans-serif; --font-mono: \"IBM Plex Mono\", monospace;\n" +
            "    }\n" +
            "    :root[data-theme=\"dark\"] {\n" +
            "      --ground: #0D1218; --surface: #151C24; --surface-2: #1C242D; --stage: #111820;\n" +
            "      --ink: #E6EDF4; --ink-2: #9EACBA; --ink-3: #6A7988; --line: #26313C; --line-soft: #1E2831;\n" +
            "      --accent: #7093FF; --accent-ink: #0D1218; --accent-soft: #1B2540;\n" +
            "      --done: #36C79E; --compare: #EFA544; --swap: #FF6B84; --pivot: #D284D2;\n" +
            "      --shadow: 0 1px 2px rgba(0,0,0,.35), 0 8px 24px rgba(0,0,0,.35);\n" +
            "    }\n" +
            "    * { box-sizing: border-box; }\n" +
            "    body { margin: 0; background: var(--ground); color: var(--ink); font-family: var(--font-body); font-size: 15px; line-height: 1.55; }\n" +
            "    .wrap { max-width: 1440px; margin: 0 auto; padding: 20px 20px 140px; }\n" +
            "    header.top { display: flex; align-items: baseline; justify-content: space-between; border-bottom: 1px solid var(--line); padding-bottom: 14px; margin-bottom: 18px; }\n" +
            "    .wordmark { font-family: var(--font-display); font-size: 22px; font-weight: 700; margin: 0; }\n" +
            "    .wordmark .dot { color: var(--accent); }\n" +
            "    .tagline { color: var(--ink-2); font-size: 13.5px; margin: 0; flex: 1; margin-left: 16px; }\n" +
            "    .theme-btn { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-sm); padding: 5px 11px; font-size: 12.5px; color: var(--ink-2); cursor: pointer; }\n" +
            "    .picker { display: flex; flex-direction: column; gap: 7px; margin-bottom: 16px; }\n" +
            "    .eyebrow { font-family: var(--font-mono); font-size: 10.5px; font-weight: 600; letter-spacing: .14em; text-transform: uppercase; color: var(--ink-3); }\n" +
            "    .chips { display: flex; flex-wrap: wrap; gap: 6px; }\n" +
            "    .chip { background: var(--surface); border: 1px solid var(--line); border-radius: 100px; padding: 5px 13px; font-size: 13px; font-weight: 500; color: var(--ink-2); cursor: pointer; transition: 0.2s; }\n" +
            "    .chip.active { background: var(--accent); border-color: var(--accent); color: var(--accent-ink); }\n" +
            "    .deck { display: grid; grid-template-columns: minmax(0, 1.65fr) minmax(300px, 1fr); gap: 16px; align-items: start; }\n" +
            "    .panel { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); box-shadow: var(--shadow); overflow: hidden; }\n" +
            "    .panel-head { padding: 10px 14px; border-bottom: 1px solid var(--line-soft); display: flex; justify-content: space-between; align-items: center; }\n" +
            "    .panel-title { font-family: var(--font-display); font-weight: 600; font-size: 14px; margin: 0; }\n" +
            "    .stage { background: var(--stage); padding: 0; position: relative; height: 360px; display: flex; align-items: center; justify-content: center; }\n" +
            "    canvas { width: 100%; height: 100%; display: block; }\n" +
            "    .narration { display: flex; gap: 10px; padding: 11px 14px; background: var(--surface-2); border-top: 1px solid var(--line-soft); min-height: 58px; align-items: center; }\n" +
            "    .step-no { font-family: var(--font-mono); font-size: 11px; background: var(--surface); border: 1px solid var(--line); padding: 1px 6px; border-radius: 4px; color: var(--ink-3); }\n" +
            "    .code { padding: 6px 0 10px; overflow-x: auto; }\n" +
            "    .code-line { display: flex; gap: 10px; padding: 1px 14px; font-family: var(--font-mono); font-size: 12.5px; color: var(--ink-2); }\n" +
            "    .code-line.active { background: var(--accent-soft); border-left: 3px solid var(--accent); color: var(--ink); font-weight: 500; padding-left: 11px; }\n" +
            "    .code-line .ln { color: var(--ink-3); min-width: 14px; text-align: right; user-select: none; }\n" +
            "    .metrics { display: grid; grid-template-columns: repeat(3, 1fr); }\n" +
            "    .metric { padding: 10px 14px; border-right: 1px solid var(--line-soft); }\n" +
            "    .metric:last-child { border-right: none; }\n" +
            "    .metric .k { font-family: var(--font-mono); font-size: 10px; text-transform: uppercase; color: var(--ink-3); display: block; }\n" +
            "    .metric .v { font-family: var(--font-mono); font-size: 20px; font-weight: 600; color: var(--ink); }\n" +
            "    .v.danger { color: var(--swap); }\n" +
            "    .v.warning { color: var(--compare); }\n" +
            "    .v.success { color: var(--done); }\n" +
            "    .transport { position: fixed; left: 0; right: 0; bottom: 0; background: var(--surface); border-top: 1px solid var(--line); box-shadow: 0 -4px 20px rgba(0,0,0,.35); z-index: 20; padding: 12px 20px; display: flex; justify-content: center; gap: 14px; }\n" +
            "    .tbtn { background: var(--surface-2); border: 1px solid var(--line); border-radius: var(--radius-sm); width: 40px; height: 36px; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; color: var(--ink-2); transition: 0.2s; }\n" +
            "    .tbtn:hover { border-color: var(--accent); color: var(--accent); }\n" +
            "    .tbtn.play { background: var(--accent); color: var(--accent-ink); border-color: var(--accent); width: 50px; }\n" +
            "    .tbtn svg { width: 16px; height: 16px; fill: currentColor; }\n" +
            "    .btn-small { background: var(--surface-2); border: 1px solid var(--line); border-radius: 4px; padding: 4px 10px; font-size: 11px; cursor:pointer; color: var(--ink-2); }\n" +
            "    .btn-small:hover { border-color: var(--accent); color: var(--accent); }\n" +
            "    table { width: 100%; border-collapse: collapse; font-size: 13px; }\n" +
            "    th { background: var(--surface-2); padding: 8px; text-align: left; color: var(--ink-3); font-family: var(--font-mono); font-size: 11px; text-transform: uppercase; border-bottom: 1px solid var(--line); }\n" +
            "    td { padding: 8px; border-bottom: 1px solid var(--line-soft); color: var(--ink); }\n" +
            "    .table-container { max-height: 360px; overflow-y: auto; }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <div class=\"wrap\">\n" +
            "    <header class=\"top\">\n" +
            "      <h1 class=\"wordmark\">Motor Antifraude<span class=\"dot\">.</span></h1>\n" +
            "      <p class=\"tagline\">Análise de risco em grafos, árvores e janelas temporais passo a passo.</p>\n" +
            "      <button class=\"theme-btn\" id=\"theme-toggle\" type=\"button\">Modo Claro / Escuro</button>\n" +
            "    </header>\n" +
            "\n" +
            "    <div class=\"picker\">\n" +
            "      <span class=\"eyebrow\">Cenários e Algoritmos</span>\n" +
            "      <div class=\"chips\">\n" +
            "        <button class=\"chip active\" onclick=\"mudarAba('fluxo', this)\">0. Pipeline de Transação</button>\n" +
            "        <button class=\"chip\" onclick=\"mudarAba('contas', this)\">1. Treap (Contas/Saldo)</button>\n" +
            "        <button class=\"chip\" onclick=\"mudarAba('smurfing', this)\">2. Janela: Smurfing</button>\n" +
            "        <button class=\"chip\" onclick=\"mudarAba('burst', this)\">3. Janela: Velocity Burst</button>\n" +
            "        <button class=\"chip\" onclick=\"mudarAba('ciclo', this)\">4. Grafo: Bounded DFS (Lavagem)</button>\n" +
            "        <button class=\"chip\" onclick=\"mudarAba('fanin', this)\">5. Grafo: In-Degree (Mulas)</button>\n" +
            "      </div>\n" +
            "    </div>\n" +
            "\n" +
            "    <div class=\"deck\">\n" +
            "      <div class=\"stage-col\">\n" +
            "        <section class=\"panel\">\n" +
            "          <div class=\"panel-head\">\n" +
            "            <h2 class=\"panel-title\" id=\"stage-title\">Dados em Memória</h2>\n" +
            "            <div id=\"controles-fluxo\" style=\"display:flex; gap:8px;\">\n" +
            "               <button class=\"btn-small\" onclick=\"iniciarFluxo('legitima')\">▶ Simular Aprovada</button>\n" +
            "               <button class=\"btn-small\" onclick=\"iniciarFluxo('fraude')\">▶ Simular Negada</button>\n" +
            "            </div>\n" +
            "          </div>\n" +
            "          <div class=\"stage\" id=\"stage-container\">\n" +
            "             <canvas id=\"mainCanvas\"></canvas>\n" +
            "             <div id=\"table-container\" class=\"table-container\" style=\"display:none; width:100%; height:100%;\"></div>\n" +
            "          </div>\n" +
            "          <div class=\"narration\">\n" +
            "            <span class=\"step-no\" id=\"step-counter\">0 / 0</span>\n" +
            "            <div style=\"width:100%;\">\n" +
            "               <p id=\"narrative-text\" style=\"margin:0;\">No pipeline, use os botões no topo da tela para escolher o cenário.</p>\n" +
            "            </div>\n" +
            "          </div>\n" +
            "        </section>\n" +
            "      </div>\n" +
            "\n" +
            "      <div class=\"stage-col\">\n" +
            "        <section class=\"panel\">\n" +
            "          <div class=\"panel-head\"><h2 class=\"panel-title\">Pseudocódigo (MotorFraude.java)</h2></div>\n" +
            "          <div class=\"code\" id=\"pseudocode-box\">\n" +
            "          </div>\n" +
            "        </section>\n" +
            "\n" +
            "        <section class=\"panel\">\n" +
            "          <div class=\"panel-head\"><h2 class=\"panel-title\">Métricas da Transação</h2></div>\n" +
            "          <div class=\"metrics\">\n" +
            "            <div class=\"metric\"><span class=\"k\">Score Fraude</span><span class=\"v\" id=\"m-score\">0.0</span></div>\n" +
            "            <div class=\"metric\"><span class=\"k\">Decisão</span><span class=\"v\" id=\"m-status\">-</span></div>\n" +
            "            <div class=\"metric\"><span class=\"k\">Vértices / Buffer</span><span class=\"v\" id=\"m-extra\">-</span></div>\n" +
            "          </div>\n" +
            "        </section>\n" +
            "      </div>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "\n" +
            "  <!-- BARRA DE TRANSPORTE -->\n" +
            "  <div class=\"transport\" id=\"barra-transporte\">\n" +
            "      <button class=\"tbtn\" onclick=\"resetCurrent()\" title=\"Resetar Cenário\">\n" +
            "        <svg viewBox=\"0 0 24 24\"><path d=\"M6 5h2.5v14H6zM20 5v14L9.5 12z\"/></svg>\n" +
            "      </button>\n" +
            "      <button class=\"tbtn play\" id=\"btn-play\" onclick=\"togglePlay()\" title=\"Reproduzir Lote (Automático)\">\n" +
            "        <svg viewBox=\"0 0 24 24\" id=\"icon-play\"><path d=\"M7 4v16l13-8z\"/></svg>\n" +
            "      </button>\n" +
            "      <button class=\"tbtn\" onclick=\"nextStep()\" title=\"Avançar 1 Passo\">\n" +
            "        <svg viewBox=\"0 0 24 24\"><path d=\"M6 5v14l11-7z\"/></svg>\n" +
            "      </button>\n" +
            "  </div>\n" +
            "\n" +
            "  <script>\n" +
            "    const root = document.documentElement;\n" +
            "    document.getElementById('theme-toggle').onclick = () => {\n" +
            "      root.setAttribute('data-theme', root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');\n" +
            "      desenharGrafoAtual();\n" +
            "    };\n" +
            "\n" +
            "    let abaAtual = 'fluxo';\n" +
            "    let isPlaying = false;\n" +
            "    let playInterval = null;\n" +
            "    let estadoArestas = [];\n" +
            "    let estadoFluxo = {}; \n" +
            "    let tipoFluxo = 'legitima'; \n" +
            "    let isProcessing = false; \n" +
            "\n" +
            "    const codigos = {\n" +
            "      'fluxo': [\n" +
            "         \"1. tx = nova Transacao(origem, destino, valor)\",\n" +
            "         \"2. TreapContas.verificarSaldo(tx.origem, tx.valor)\",\n" +
            "         \"3. JanelaDeslizante.avaliarRisco(tx)\",\n" +
            "         \"4. GrafoTransacional.avaliarRisco(tx)\",\n" +
            "         \"5. motor.processarTransacao(tx);\"\n" +
            "      ],\n" +
            "      'contas': [\n" +
            "         \"Treap.obterOuCriar(idConta):\",\n" +
            "         \"  se raiz == nulo: insere novo NoTreap\",\n" +
            "         \"  senão: busca binária (O(log N))\",\n" +
            "         \"  mantém balanceamento por Max-Heap\",\n" +
            "         \"  retorna Perfil da Conta (Saldo, Média)\"\n" +
            "      ],\n" +
            "      'smurfing': [\n" +
            "         \"limite = tempoAtual - 10_minutos\",\n" +
            "         \"idx = JanelaDeslizante.lowerBound(limite)\",\n" +
            "         \"para i = idx até buffer.size():\",\n" +
            "         \"  acumula valores da origem\",\n" +
            "         \"se qtd >= 4 E total >= 25k:\",\n" +
            "         \"  score += 45 (Flag SMURFING)\"\n" +
            "      ],\n" +
            "      'ciclo': [\n" +
            "         \"BoundedDFS(origem, destino, depth=0):\",\n" +
            "         \"  se origem == destino: ACHOU CICLO\",\n" +
            "         \"  se depth >= 4: retorna FALSO\",\n" +
            "         \"  para vizinho em Grafo.get(origem):\",\n" +
            "         \"    se tempo_vizinho > tempo_atual: continua\",\n" +
            "         \"    se variacao > 20%: continua\",\n" +
            "         \"    BoundedDFS(vizinho, destino, depth+1)\"\n" +
            "      ],\n" +
            "      'fanin': [\n" +
            "         \"Para avaliar Fan-In (Mulas/Laranjas):\",\n" +
            "         \"  grauEntrada = Grafo.getEntradas(destino)\",\n" +
            "         \"  se grauEntrada >= 4:\",\n" +
            "         \"    alerta = VERDADEIRO\",\n" +
            "         \"    score += 45 (Flag FAN-IN)\"\n" +
            "      ],\n" +
            "      'burst': [\n" +
            "         \"limiteCurto = tempoAtual - 60_segundos\",\n" +
            "         \"contaDisparos = 0\",\n" +
            "         \"para tx no buffer recente:\",\n" +
            "         \"  se tx.origem == origem: contaDisparos++\",\n" +
            "         \"se contaDisparos >= 4:\",\n" +
            "         \"  score += 30 (Flag VELOCIDADE)\"\n" +
            "      ]\n" +
            "    };\n" +
            "\n" +
            "    function pause() {\n" +
            "       isPlaying = false;\n" +
            "       if (playInterval) clearInterval(playInterval);\n" +
            "       playInterval = null;\n" +
            "       const icon = document.getElementById('icon-play');\n" +
            "       if (icon) icon.innerHTML = '<path d=\"M7 4v16l13-8z\"/>';\n" +
            "    }\n" +
            "\n" +
            "    async function mudarAba(aba, btn) {\n" +
            "      try {\n" +
            "          pause(); \n" +
            "          document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));\n" +
            "          if(btn) btn.classList.add('active');\n" +
            "          abaAtual = aba;\n" +
            "          \n" +
            "          const divControles = document.getElementById('controles-fluxo');\n" +
            "          const divBarra = document.getElementById('barra-transporte');\n" +
            "          if(divControles) divControles.style.display = (aba === 'fluxo') ? 'flex' : 'none';\n" +
            "          if(divBarra) divBarra.style.display = (aba === 'fluxo') ? 'none' : 'flex';\n" +
            "\n" +
            "          const mCanvas = document.getElementById('mainCanvas');\n" +
            "          const mTable = document.getElementById('table-container');\n" +
            "          \n" +
            "          if(aba === 'contas') {\n" +
            "             if(mCanvas) mCanvas.style.display = 'none';\n" +
            "             if(mTable) mTable.style.display = 'block';\n" +
            "             carregarTabela(aba);\n" +
            "          } else {\n" +
            "             if(mCanvas) mCanvas.style.display = 'block';\n" +
            "             if(mTable) mTable.style.display = 'none';\n" +
            "          }\n" +
            "          await resetCurrent();\n" +
            "      } catch(e) { console.error('Erro ao mudar aba:', e); }\n" +
            "    }\n" +
            "\n" +
            "    async function iniciarFluxo(tipo) {\n" +
            "        try {\n" +
            "            if (isProcessing) return;\n" +
            "            pause(); \n" +
            "            tipoFluxo = tipo;\n" +
            "            await resetCurrent();\n" +
            "            if (abaAtual !== 'fluxo') return; \n" +
            "            \n" +
            "            isPlaying = true;\n" +
            "            document.getElementById('icon-play').innerHTML = '<path d=\"M6 4h4v16H6zM14 4h4v16h-4z\"/>';\n" +
            "            await nextStep(); \n" +
            "            playInterval = setInterval(nextStep, 2000);\n" +
            "        } catch(e) { console.error(e); }\n" +
            "    }\n" +
            "\n" +
            "    async function togglePlay() {\n" +
            "       try {\n" +
            "           if (isPlaying) {\n" +
            "              pause();\n" +
            "           } else {\n" +
            "              pause(); \n" +
            "              isPlaying = true;\n" +
            "              document.getElementById('icon-play').innerHTML = '<path d=\"M6 4h4v16H6zM14 4h4v16h-4z\"/>';\n" +
            "              await nextStep();\n" +
            "              playInterval = setInterval(nextStep, 2000);\n" +
            "           }\n" +
            "       } catch(e) { console.error(e); }\n" +
            "    }\n" +
            "\n" +
            "    async function nextStep() {\n" +
            "      if (abaAtual === 'contas') return;\n" +
            "      if (isProcessing) return; \n" +
            "      isProcessing = true;\n" +
            "      \n" +
            "      try {\n" +
            "          let data;\n" +
            "          if (abaAtual === 'fluxo') {\n" +
            "              const res = await fetch(`/api/fluxo/passo?tipo=${tipoFluxo}`);\n" +
            "              if (!res.ok) throw new Error('Falha API Fluxo');\n" +
            "              data = await res.json();\n" +
            "              estadoFluxo = data;\n" +
            "              desenharGrafoAtual();\n" +
            "              \n" +
            "              let colorClass = '';\n" +
            "              if(data.statusAtual === 'APROVADA') colorClass = 'success';\n" +
            "              if(data.statusAtual === 'SUSPEITA') colorClass = 'warning';\n" +
            "              if(data.statusAtual === 'BLOQUEADA') colorClass = 'danger';\n" +
            "              \n" +
            "              document.getElementById('m-score').innerText = data.scoreAtual.toFixed(1);\n" +
            "              document.getElementById('m-score').className = `v ${colorClass}`;\n" +
            "              document.getElementById('m-status').innerText = data.statusAtual;\n" +
            "              document.getElementById('m-status').className = `v ${colorClass}`;\n" +
            "              document.getElementById('m-extra').innerText = data.infoExtra;\n" +
            "              document.getElementById('step-counter').innerText = `Passo ${data.passoAtual} / 5`;\n" +
            "              document.getElementById('narrative-text').innerHTML = `<code>${tipoFluxo === 'legitima'?'Transação Normal':'Fraude (Ciclo)'}</code>: <br><span style=\"color:var(--ink-2); font-size:13px;\">${data.mensagem}</span>`;\n" +
            "              atualizarPseudocodigo('fluxo', data.linhaCodigo);\n" +
            "              if(data.proximoPassoTexto.includes('Concluido')) pause(); \n" +
            "          } else {\n" +
            "              const res = await fetch(`/api/${abaAtual}/passo`);\n" +
            "              if (!res.ok) throw new Error('Falha API Grafo');\n" +
            "              data = await res.json();\n" +
            "              estadoArestas = data.arestas || [];\n" +
            "              desenharGrafoAtual();\n" +
            "              \n" +
            "              if (data.ultimaTransacao) {\n" +
            "                  const tx = data.ultimaTransacao;\n" +
            "                  let classCss = 'success';\n" +
            "                  if(tx.status === 'SUSPEITA') classCss = 'warning';\n" +
            "                  if(tx.status === 'BLOQUEADA') classCss = 'danger';\n" +
            "                  \n" +
            "                  document.getElementById('m-score').innerText = tx.score.toFixed(1);\n" +
            "                  document.getElementById('m-score').className = `v ${classCss}`;\n" +
            "                  document.getElementById('m-status').innerText = tx.status;\n" +
            "                  document.getElementById('m-status').className = `v ${classCss}`;\n" +
            "                  document.getElementById('step-counter').innerText = `Passo ${data.passoAtual} / 5`;\n" +
            "                  document.getElementById('narrative-text').innerHTML = `<code>Tx #${tx.id}</code>: C${tx.origem} -> C${tx.destino} (R$ ${(tx.valor/100).toFixed(2)})<br><span style=\"color:var(--ink-2); font-size:13px;\">${tx.motivo}</span>`;\n" +
            "                  \n" +
            "                  if(abaAtual === 'smurfing') { document.getElementById('m-extra').innerText = `${data.bufferQtd} txs na Janela`; atualizarPseudocodigo(abaAtual, tx.status==='SUSPEITA'?4:2); }\n" +
            "                  if(abaAtual === 'ciclo') { document.getElementById('m-extra').innerText = `${data.passoAtual} vértices (DFS)`; atualizarPseudocodigo(abaAtual, tx.status==='BLOQUEADA'?1:5); }\n" +
            "                  if(abaAtual === 'fanin') { document.getElementById('m-extra').innerText = `In-Degree: ${data.inDegree}`; atualizarPseudocodigo(abaAtual, 2); }\n" +
            "                  if(abaAtual === 'burst') { document.getElementById('m-extra').innerText = `${data.velocidadePorMinuto} tx/min`; atualizarPseudocodigo(abaAtual, 4); }\n" +
            "              }\n" +
            "              if(data.proximoPassoTexto && data.proximoPassoTexto.includes('Concluido')) pause(); \n" +
            "          }\n" +
            "      } catch (e) {\n" +
            "          console.error(\"Erro no processamento da API:\", e);\n" +
            "          pause();\n" +
            "      } finally {\n" +
            "          isProcessing = false;\n" +
            "      }\n" +
            "    }\n" +
            "\n" +
            "    async function resetCurrent() {\n" +
            "      if (abaAtual === 'contas') return;\n" +
            "      try {\n" +
            "          await fetch(`/api/${abaAtual}/reset`);\n" +
            "      } catch (e) { console.error(\"Erro ao resetar:\", e); }\n" +
            "      \n" +
            "      estadoArestas = [];\n" +
            "      estadoFluxo = {};\n" +
            "      \n" +
            "      const setEl = (id, txt, cls) => { const e = document.getElementById(id); if(e){ e.innerText = txt; if(cls !== undefined) e.className = cls; } };\n" +
            "      setEl('m-score', '0.0', 'v');\n" +
            "      setEl('m-status', '-', 'v');\n" +
            "      setEl('m-extra', '-');\n" +
            "      setEl('step-counter', '0 / 0');\n" +
            "      setEl('narrative-text', 'Aperte Play para analisar os dados em memória.');\n" +
            "      \n" +
            "      atualizarPseudocodigo(abaAtual, -1);\n" +
            "      desenharGrafoAtual();\n" +
            "    }\n" +
            "\n" +
            "    function getCSSVar(name) { return getComputedStyle(document.documentElement).getPropertyValue(name).trim(); }\n" +
            "    \n" +
            "    function desenharSeta(ctx, fromX, fromY, toX, toY, cor) {\n" +
            "        const headlen = 12; const angle = Math.atan2(toY - fromY, toX - fromX); const raioNo = 25;\n" +
            "        const inicioX = fromX + raioNo * Math.cos(angle); const inicioY = fromY + raioNo * Math.sin(angle);\n" +
            "        const fimX = toX - raioNo * Math.cos(angle); const fimY = toY - raioNo * Math.sin(angle);\n" +
            "        ctx.beginPath(); ctx.moveTo(inicioX, inicioY); ctx.lineTo(fimX, fimY); ctx.strokeStyle = cor; ctx.lineWidth = 3; ctx.stroke();\n" +
            "        ctx.beginPath(); ctx.moveTo(fimX, fimY); ctx.lineTo(fimX - headlen * Math.cos(angle - Math.PI / 6), fimY - headlen * Math.sin(angle - Math.PI / 6)); ctx.lineTo(fimX - headlen * Math.cos(angle + Math.PI / 6), fimY - headlen * Math.sin(angle + Math.PI / 6)); ctx.fillStyle = cor; ctx.fill();\n" +
            "    }\n" +
            "\n" +
            "    function desenharGrafoAtual() {\n" +
            "        if(abaAtual === 'contas') return;\n" +
            "        const canvas = document.getElementById('mainCanvas');\n" +
            "        if (!canvas) return;\n" +
            "        const ctx = canvas.getContext('2d');\n" +
            "        const cw = canvas.clientWidth || 800; const ch = canvas.clientHeight || 360;\n" +
            "        canvas.width = cw; canvas.height = ch;\n" +
            "        ctx.clearRect(0, 0, cw, ch);\n" +
            "\n" +
            "        if (abaAtual === 'fluxo') {\n" +
            "            document.getElementById('stage-title').innerText = 'Ciclo de Vida da Transação';\n" +
            "            const cAtivo = getCSSVar('--accent');\n" +
            "            const cInativo = getCSSVar('--surface');\n" +
            "            const nodes = [\n" +
            "                { id: 'origem', x: cw * 0.1, y: ch / 2, tipo: 'circle', label: 'Origem' },\n" +
            "                { id: 'treap', x: cw * 0.3, y: ch / 2, tipo: 'rect', label: 'Treap (Saldo)' },\n" +
            "                { id: 'janela', x: cw * 0.5, y: ch / 2, tipo: 'rect', label: 'Janela' },\n" +
            "                { id: 'grafo', x: cw * 0.7, y: ch / 2, tipo: 'rect', label: 'Grafo' },\n" +
            "                { id: 'destino', x: cw * 0.9, y: ch / 2, tipo: 'circle', label: 'Destino' }\n" +
            "            ];\n" +
            "\n" +
            "            for (let i = 0; i < nodes.length - 1; i++) {\n" +
            "                let xOrig = nodes[i].x + (nodes[i].tipo === 'rect' ? 45 : 25);\n" +
            "                let xDest = nodes[i+1].x - (nodes[i+1].tipo === 'rect' ? 45 : 25);\n" +
            "                desenharSeta(ctx, xOrig, nodes[i].y, xDest, nodes[i+1].y, getCSSVar('--line-soft'));\n" +
            "            }\n" +
            "\n" +
            "            const ativoIdx = (estadoFluxo && estadoFluxo.nodeAtivo !== undefined) ? estadoFluxo.nodeAtivo : -1;\n" +
            "            nodes.forEach((n, i) => {\n" +
            "                ctx.beginPath();\n" +
            "                if (i === ativoIdx) {\n" +
            "                     if (i === 4) ctx.fillStyle = (estadoFluxo.statusAtual === 'BLOQUEADA') ? getCSSVar('--swap') : getCSSVar('--done');\n" +
            "                     else if (i === 3 && estadoFluxo.statusAtual === 'SUSPEITA') ctx.fillStyle = getCSSVar('--compare');\n" +
            "                     else ctx.fillStyle = cAtivo;\n" +
            "                } else { ctx.fillStyle = cInativo; }\n" +
            "\n" +
            "                if (n.tipo === 'circle') ctx.arc(n.x, n.y, 28, 0, 2 * Math.PI);\n" +
            "                else ctx.rect(n.x - 45, n.y - 25, 90, 50);\n" +
            "                \n" +
            "                ctx.fill(); ctx.strokeStyle = getCSSVar('--line'); ctx.lineWidth = 2; ctx.stroke();\n" +
            "                ctx.fillStyle = (i === ativoIdx) ? getCSSVar('--accent-ink') : getCSSVar('--ink');\n" +
            "                ctx.font = 'bold 11px \"IBM Plex Mono\"'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';\n" +
            "                ctx.fillText(n.label, n.x, n.y);\n" +
            "            });\n" +
            "            return; \n" +
            "        }\n" +
            "\n" +
            "        let pos = {};\n" +
            "        if (abaAtual === 'smurfing') {\n" +
            "            document.getElementById('stage-title').innerText = 'Grafo: 15 -> 40 (Pulverização)';\n" +
            "            pos = { 15: {x: cw*0.2, y: ch/2}, 40: {x: cw*0.8, y: ch/2} };\n" +
            "        } else if (abaAtual === 'ciclo') {\n" +
            "            document.getElementById('stage-title').innerText = 'Grafo: Anel 20 -> 30 -> 40 -> 20';\n" +
            "            pos = { 20: {x: cw/2, y: ch*0.2}, 30: {x: cw*0.8, y: ch*0.7}, 40: {x: cw*0.2, y: ch*0.7} };\n" +
            "        } else if (abaAtual === 'fanin') {\n" +
            "            document.getElementById('stage-title').innerText = 'Grafo: Fan-In na Conta 80';\n" +
            "            pos = { 10: {x: cw*0.1, y: ch*0.1}, 15: {x: cw*0.1, y: ch*0.3}, 20: {x: cw*0.1, y: ch*0.5}, 25: {x: cw*0.1, y: ch*0.7}, 30: {x: cw*0.1, y: ch*0.9}, 80: {x: cw*0.8, y: ch/2} };\n" +
            "        } else if (abaAtual === 'burst') {\n" +
            "            document.getElementById('stage-title').innerText = 'Grafo: Explosão Radial da Conta 25';\n" +
            "            pos = { 25: {x: cw/2, y: ch/2}, 50: {x: cw*0.2, y: ch*0.2}, 60: {x: cw*0.8, y: ch*0.2}, 70: {x: cw*0.8, y: ch*0.8}, 80: {x: cw*0.2, y: ch*0.8}, 10: {x: cw/2, y: ch*0.1} };\n" +
            "        }\n" +
            "        \n" +
            "        const cText = getCSSVar('--ink'); const cSurf = getCSSVar('--surface-2');\n" +
            "        const cDone = getCSSVar('--done'); const cWarn = getCSSVar('--compare'); const cAlert = getCSSVar('--swap');\n" +
            "\n" +
            "        if (estadoArestas && estadoArestas.length > 0) {\n" +
            "            estadoArestas.forEach(a => {\n" +
            "                if (pos[a.origem] && pos[a.destino]) {\n" +
            "                    let corSeta = cDone;\n" +
            "                    if(a.status === 'SUSPEITA') corSeta = cWarn;\n" +
            "                    if(a.status === 'BLOQUEADA') corSeta = cAlert;\n" +
            "                    desenharSeta(ctx, pos[a.origem].x, pos[a.origem].y, pos[a.destino].x, pos[a.destino].y, corSeta);\n" +
            "                }\n" +
            "            });\n" +
            "        }\n" +
            "\n" +
            "        Object.keys(pos).forEach(id => {\n" +
            "            const p = pos[id];\n" +
            "            ctx.beginPath(); ctx.arc(p.x, p.y, 25, 0, 2 * Math.PI); ctx.fillStyle = cSurf; ctx.fill(); ctx.strokeStyle = getCSSVar('--line'); ctx.lineWidth = 2; ctx.stroke();\n" +
            "            ctx.fillStyle = cText; ctx.font = 'bold 13px \"IBM Plex Mono\"'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillText('C' + id, p.x, p.y);\n" +
            "        });\n" +
            "    }\n" +
            "\n" +
            "    async function carregarTabela(tipo) {\n" +
            "      const title = document.getElementById('stage-title');\n" +
            "      const tbl = document.getElementById('table-container');\n" +
            "      if (tipo === 'contas') {\n" +
            "        title.innerText = 'Treap: Tabela de Contas (O(log N))';\n" +
            "        try {\n" +
            "            const res = await fetch('/api/contas/todas'); \n" +
            "            if(!res.ok) throw new Error('Falha HTTP');\n" +
            "            const data = await res.json();\n" +
            "            let h = `<table><tr><th>ID Conta</th><th>Saldo (R$)</th><th>Score</th></tr>`;\n" +
            "            data.forEach(c => { h += `<tr><td><strong>#${c.id}</strong></td><td>${(c.saldo/100).toFixed(2)}</td><td style=\"color:${c.scoreRisco>50?'var(--swap)':'var(--done)'}\">${c.scoreRisco.toFixed(1)}</td></tr>`; });\n" +
            "            tbl.innerHTML = h + `</table>`;\n" +
            "        } catch(e) { tbl.innerHTML = 'Erro ao carregar os dados. Verifique o servidor.'; console.error(e); }\n" +
            "      }\n" +
            "    }\n" +
            "\n" +
            "    function atualizarPseudocodigo(aba, linhaDestaque) {\n" +
            "      const box = document.getElementById('pseudocode-box');\n" +
            "      let h = '';\n" +
            "      if(codigos[aba]) {\n" +
            "          codigos[aba].forEach((linha, i) => {\n" +
            "            const active = (i === linhaDestaque) ? 'active' : '';\n" +
            "            h += `<div class=\"code-line ${active}\"><span class=\"ln\">${i+1}</span><span>${linha}</span></div>`;\n" +
            "          });\n" +
            "      }\n" +
            "      box.innerHTML = h;\n" +
            "    }\n" +
            "\n" +
            "    window.onload = () => { mudarAba('fluxo', document.querySelector('.chip')); };\n" +
            "  </script>\n" +
            "</body>\n" +
            "</html>";

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().set("Pragma", "no-cache");
            exchange.getResponseHeaders().set("Expires", "0");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // =========================================================================
    // API FLUXO COMPLETO DA TRANSAÇÃO (OPÇÕES: LEGÍTIMA OU FRAUDE)
    // =========================================================================
    static class FluxoPassoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            boolean isFraude = query != null && query.contains("tipo=fraude");
            
            if (passoFluxoAtual < 5) passoFluxoAtual++;
            
            int nodeAtivo = -1;
            int linhaCodigo = 0;
            double scoreAtual = 0.0;
            String statusAtual = "PROCESSANDO";
            String mensagem = "";
            String infoExtra = "-";
            String proximoPassoTexto = "Passo " + (passoFluxoAtual) + " de 5";
            
            Conta c10 = motor.getTreapContas().obterOuCriar(10, 100_000_00L, 2_000_00L);
            
            if (passoFluxoAtual == 1) {
                nodeAtivo = 0;
                linhaCodigo = 0;
                mensagem = isFraude ? "Iniciada tentativa de Lavagem (C10 -> C20) de R$ 48.000,00." : "Transação comum iniciada (C10 -> C20) de R$ 500,00.";
            } else if (passoFluxoAtual == 2) {
                nodeAtivo = 1;
                linhaCodigo = 1;
                mensagem = "Consulta rápida na Treap (Tempo O(log N)). Saldo de R$ " + (c10.getSaldoCentavos()/100.0) + " é suficiente. Conta isolada com Lock temporário.";
                infoExtra = "Saldo Validado";
            } else if (passoFluxoAtual == 3) {
                nodeAtivo = 2;
                linhaCodigo = 2;
                mensagem = "Varredura na Janela Deslizante (últimos 10 min). Nenhuma atividade anormal de frequência ou pulverização detectada.";
                infoExtra = "Buffer Limpo";
            } else if (passoFluxoAtual == 4) {
                nodeAtivo = 3;
                linhaCodigo = 3;
                if (isFraude) {
                    scoreAtual = 100.0;
                    statusAtual = "SUSPEITA";
                    mensagem = "Motor acionou a Busca DFS no Grafo e identificou conexão com histórico criminoso recente. A transação fecha um anel de lavagem de dinheiro!";
                    infoExtra = "Ciclo Detectado";
                } else {
                    mensagem = "Busca no Grafo Transacional não encontrou anéis de lavagem ou padrão de contas laranjas (Fan-in). Vizinhança limpa.";
                    infoExtra = "Vizinhança Limpa";
                }
            } else {
                nodeAtivo = 4;
                linhaCodigo = 4;
                proximoPassoTexto = "Concluido (Resetar)";
                
                if (historicoDecisoes.size() == 0 || historicoDecisoes.get(historicoDecisoes.size() -1).getIdContaOrigem() != 10) {
                    if (isFraude) {
                        timestampAtual += 1000L;
                        motor.processarTransacao(new Transacao(contadorId++, 20, 30, 480_000_00L, timestampAtual));
                        motor.processarTransacao(new Transacao(contadorId++, 30, 40, 480_000_00L, timestampAtual + 1000));
                        motor.processarTransacao(new Transacao(contadorId++, 40, 10, 480_000_00L, timestampAtual + 2000));
                        
                        Transacao txFraude = new Transacao(contadorId++, 10, 20, 480_000_00L, timestampAtual + 3000);
                        motor.processarTransacao(txFraude);
                        historicoDecisoes.add(txFraude);
                    } else {
                        timestampAtual += 1000L;
                        Transacao txLegitima = new Transacao(contadorId++, 10, 20, 500_00L, timestampAtual);
                        motor.processarTransacao(txLegitima);
                        historicoDecisoes.add(txLegitima);
                    }
                    checarSelagemMerkle();
                }

                Transacao tx = historicoDecisoes.get(historicoDecisoes.size() - 1);
                scoreAtual = tx.getScoreFraude();
                statusAtual = tx.getStatus().name();
                if (isFraude) {
                    mensagem = "Transação <b>BLOQUEADA</b> pelo Motor de Fraude. Transferência desfeita e alerta enviado para a base. (Status Real: " + statusAtual + ")";
                    infoExtra = "Retido R$ 48 mil";
                } else {
                    mensagem = "Score Seguro. Transação <b>APROVADA</b> pelo Motor de Fraude. Saldo debitado e Bloco enviado para registro Merkle. (Status Real: " + statusAtual + ")";
                    infoExtra = "Transferido R$ 500";
                }
            }

            StringBuilder json = new StringBuilder();
            json.append("{")
                .append("\"passoAtual\":").append(passoFluxoAtual).append(",")
                .append("\"proximoPassoTexto\":\"").append(proximoPassoTexto).append("\",")
                .append("\"nodeAtivo\":").append(nodeAtivo).append(",")
                .append("\"linhaCodigo\":").append(linhaCodigo).append(",")
                .append("\"scoreAtual\":").append(scoreAtual).append(",")
                .append("\"statusAtual\":\"").append(statusAtual).append("\",")
                .append("\"infoExtra\":\"").append(infoExtra).append("\",")
                .append("\"mensagem\":\"").append(mensagem).append("\"")
                .append("}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // =========================================================================
    // API GENÉRICA DE RESET (USADA POR TODAS AS ABAS)
    // =========================================================================
    static class ResetGeralHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            reiniciarSistema();
            String json = "{\"status\":\"ok\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // =========================================================================
    // HANDLERS CASO 1: SMURFING COM AUDITORIA NUMÉRICA
    // =========================================================================
    static class SmurfingPassoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (passoSmurfingAtual < 5) passoSmurfingAtual++;
            
            Conta conta15 = motor.getTreapContas().obterOuCriar(15, 100_000_00L, 2_000_00L);
            long saldoAnterior = conta15.getSaldoCentavos();
            timestampAtual += 15_000L;

            if (historicoDecisoes.size() < passoSmurfingAtual) {
                Transacao tx = new Transacao(contadorId++, 15, 40, 9_600_00L, timestampAtual);
                motor.processarTransacao(tx);
                historicoDecisoes.add(tx);
                checarSelagemMerkle();
            }

            MetricasJanela metricas = motor.getJanelaDeslizante().avaliarAtividadeConta(15, 10 * 60 * 1000L, timestampAtual);
            String proximoTxt = (passoSmurfingAtual < 5) ? ("Passo " + (passoSmurfingAtual) + " de 5") : "Smurfing Concluido (Resetar)";
            Transacao txExibir = historicoDecisoes.get(historicoDecisoes.size() - 1);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"passoAtual\":").append(passoSmurfingAtual).append(",");
            json.append("\"proximoPassoTexto\":\"").append(proximoTxt).append("\",");
            json.append("\"saldoAnterior\":").append(saldoAnterior).append(",");
            json.append("\"novoSaldo\":").append(conta15.getSaldoCentavos()).append(",");
            json.append("\"bufferQtd\":").append(metricas.getQtdTransacoes()).append(",");
            json.append("\"bufferValor\":").append(metricas.getValorTotalCentavos()).append(",");
            json.append("\"ultimaTransacao\":{")
                .append("\"id\":").append(txExibir.getId()).append(",")
                .append("\"origem\":").append(txExibir.getIdContaOrigem()).append(",")
                .append("\"destino\":").append(txExibir.getIdContaDestino()).append(",")
                .append("\"valor\":").append(txExibir.getValorCentavos()).append(",")
                .append("\"score\":").append(String.format(Locale.US, "%.1f", txExibir.getScoreFraude())).append(",")
                .append("\"status\":\"").append(txExibir.getStatus().name()).append("\",")
                .append("\"motivo\":\"").append(txExibir.getMotivoFraude().replace("\"", "'")).append("\"")
                .append("},");
            json.append("\"arestas\":[{\"origem\":15,\"destino\":40,\"status\":\"").append(txExibir.getStatus().name()).append("\"}]");
            json.append("}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // =========================================================================
    // HANDLERS CASO 2: CICLO DE LAVAGEM COM PILHA DA DFS
    // =========================================================================
    static class CicloPassoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (passoCicloAtual < 3) passoCicloAtual++;
            Transacao tx = null;
            String dfsMsg = "";
            String proximoTxt = "";

            Conta c20 = motor.getTreapContas().obterOuCriar(20, 100_000_00L, 2_000_00L);
            Conta c30 = motor.getTreapContas().obterOuCriar(30, 10_000_00L, 2_000_00L);
            Conta c40 = motor.getTreapContas().obterOuCriar(40, 10_000_00L, 2_000_00L);

            if (passoCicloAtual == 1) {
                if(historicoDecisoes.size() < 1) {
                    timestampAtual += 30_000L;
                    tx = new Transacao(contadorId++, 20, 30, 50_000_00L, timestampAtual);
                    motor.processarTransacao(tx);
                    historicoDecisoes.add(tx);
                } else { tx = historicoDecisoes.get(0); }
                dfsMsg = "- Pilha DFS: Caminho Aberto.";
                proximoTxt = "Perna 2: 30 -> 40";
            } else if (passoCicloAtual == 2) {
                if(historicoDecisoes.size() < 2) {
                    timestampAtual += 45_000L;
                    tx = new Transacao(contadorId++, 30, 40, 49_000_00L, timestampAtual);
                    motor.processarTransacao(tx);
                    historicoDecisoes.add(tx);
                } else { tx = historicoDecisoes.get(1); }
                dfsMsg = "- Pilha DFS: Caminho Aberto.";
                proximoTxt = "Perna 3: 40 -> 20 (Fechar Ciclo)";
            } else {
                if(historicoDecisoes.size() < 3) {
                    timestampAtual += 40_000L;
                    tx = new Transacao(contadorId++, 40, 20, 48_000_00L, timestampAtual);
                    motor.processarTransacao(tx);
                    historicoDecisoes.add(tx);
                } else { tx = historicoDecisoes.get(2); }
                dfsMsg = "- CICLO DETECTADO: [20 -> 30 -> 40 -> 20]";
                proximoTxt = "Ciclo Concluido (Resetar)";
            }

            checarSelagemMerkle();

            long saldoOrig = (tx.getIdContaOrigem() == 20) ? c20.getSaldoCentavos() : ((tx.getIdContaOrigem() == 30) ? c30.getSaldoCentavos() : c40.getSaldoCentavos());
            long saldoDest = (tx.getIdContaDestino() == 20) ? c20.getSaldoCentavos() : ((tx.getIdContaDestino() == 30) ? c30.getSaldoCentavos() : c40.getSaldoCentavos());
            double retencaoOrig = (tx.getIdContaOrigem() == 20) ? c20.calcularTaxaRetencao() : ((tx.getIdContaOrigem() == 30) ? c30.calcularTaxaRetencao() : c40.calcularTaxaRetencao());

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"passoAtual\":").append(passoCicloAtual).append(",");
            json.append("\"proximoPassoTexto\":\"").append(proximoTxt).append("\",");
            json.append("\"statusGrafo\":\"Grafo com ").append(passoCicloAtual).append(" perna(s) ativa(s)\",");
            json.append("\"saldoOrigem\":").append(saldoOrig).append(",");
            json.append("\"saldoDestino\":").append(saldoDest).append(",");
            json.append("\"retencaoOrigem\":").append(String.format(Locale.US, "%.2f", retencaoOrig)).append(",");
            json.append("\"dfsDiagnostico\":\"").append(dfsMsg.replace("\"", "'")).append("\",");
            json.append("\"ultimaTransacao\":{")
                .append("\"id\":").append(tx.getId()).append(",")
                .append("\"origem\":").append(tx.getIdContaOrigem()).append(",")
                .append("\"destino\":").append(tx.getIdContaDestino()).append(",")
                .append("\"valor\":").append(tx.getValorCentavos()).append(",")
                .append("\"score\":").append(String.format(Locale.US, "%.1f", tx.getScoreFraude())).append(",")
                .append("\"status\":\"").append(tx.getStatus().name()).append("\",")
                .append("\"motivo\":\"").append(tx.getMotivoFraude().replace("\"", "'")).append("\"")
                .append("},");

            json.append("\"arestas\":[");
            if (passoCicloAtual >= 1) json.append("{\"origem\":20,\"destino\":30,\"status\":\"APROVADA\"}");
            if (passoCicloAtual >= 2) json.append(",{\"origem\":30,\"destino\":40,\"status\":\"APROVADA\"}");
            if (passoCicloAtual >= 3) json.append(",{\"origem\":40,\"destino\":20,\"status\":\"BLOQUEADA\"}");
            json.append("]}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // =========================================================
    // HANDLERS CASO 3: FAN-IN
    // =========================================================
    static class FanInPassoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (passoFanInAtual < 5) passoFanInAtual++;
            
            int idxOrigem = (passoFanInAtual - 1) % ORIGENS_FAN_IN.length;
            int origem = ORIGENS_FAN_IN[idxOrigem];
            int destino = 80;
            timestampAtual += 20_000L;

            if(historicoDecisoes.size() < passoFanInAtual) {
                Transacao tx = new Transacao(contadorId++, origem, destino, 8_000_00L, timestampAtual);
                motor.processarTransacao(tx);
                historicoDecisoes.add(tx);
                checarSelagemMerkle();
            }

            Conta cOrigem = motor.getTreapContas().obterOuCriar(origem, 100_000_00L, 2_000_00L);
            Conta c80 = motor.getTreapContas().obterOuCriar(80, 100_000_00L, 2_000_00L);
            int inDegree = motor.getGrafoTransacional().getGrauEntrada(80);

            StringBuilder nos = new StringBuilder();
            for (int i = 0; i < passoFanInAtual && i < ORIGENS_FAN_IN.length; i++) {
                nos.append("Conta ").append(ORIGENS_FAN_IN[i]);
                if (i < passoFanInAtual - 1 && i < ORIGENS_FAN_IN.length - 1) nos.append(", ");
            }

            String proximoTxt = (passoFanInAtual < 5) ? ("Passo " + (passoFanInAtual) + " de 5") : "Fan-in Concluido (Resetar)";
            Transacao txExibir = historicoDecisoes.get(historicoDecisoes.size() - 1);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"passoAtual\":").append(passoFanInAtual).append(",");
            json.append("\"proximoPassoTexto\":\"").append(proximoTxt).append("\",");
            json.append("\"inDegree\":").append(inDegree).append(",");
            json.append("\"nosConectados\":\"").append(nos.toString()).append("\",");
            json.append("\"saldoOrigem\":").append(cOrigem.getSaldoCentavos()).append(",");
            json.append("\"saldoDestino\":").append(c80.getSaldoCentavos()).append(",");
            json.append("\"ultimaTransacao\":{")
                .append("\"id\":").append(txExibir.getId()).append(",")
                .append("\"origem\":").append(txExibir.getIdContaOrigem()).append(",")
                .append("\"destino\":").append(txExibir.getIdContaDestino()).append(",")
                .append("\"valor\":").append(txExibir.getValorCentavos()).append(",")
                .append("\"score\":").append(String.format(Locale.US, "%.1f", txExibir.getScoreFraude())).append(",")
                .append("\"status\":\"").append(txExibir.getStatus().name()).append("\",")
                .append("\"motivo\":\"").append(txExibir.getMotivoFraude().replace("\"", "'")).append("\"")
                .append("},");

            json.append("\"arestas\":[");
            for (int i = 0; i < passoFanInAtual && i < ORIGENS_FAN_IN.length; i++) {
                int o = ORIGENS_FAN_IN[i];
                String st = (i >= 3) ? "SUSPEITA" : "APROVADA";
                json.append("{\"origem\":").append(o).append(",\"destino\":80,\"status\":\"").append(st).append("\"}");
                if (i < passoFanInAtual - 1 && i < ORIGENS_FAN_IN.length - 1) json.append(",");
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // =========================================================
    // HANDLERS CASO 4: BURST
    // =========================================================
    static class BurstPassoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (passoBurstAtual < 5) passoBurstAtual++;
            
            int idxDestino = (passoBurstAtual - 1) % DESTINOS_BURST.length;
            int origem = 25;
            int destino = DESTINOS_BURST[idxDestino];
            timestampAtual += 1_500L;

            if(historicoDecisoes.size() < passoBurstAtual) {
                Transacao tx = new Transacao(contadorId++, origem, destino, 7_000_00L, timestampAtual);
                motor.processarTransacao(tx);
                historicoDecisoes.add(tx);
                checarSelagemMerkle();
            }

            String proximoTxt = (passoBurstAtual < 5) ? ("Disparo " + (passoBurstAtual) + " de 5") : "Rajada Concluida (Resetar)";

            double tempoTotalSegundos = passoBurstAtual * 1.5;
            int velocidadeEst = (int) ((passoBurstAtual / tempoTotalSegundos) * 60.0);
            Transacao txExibir = historicoDecisoes.get(historicoDecisoes.size() - 1);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"passoAtual\":").append(passoBurstAtual).append(",");
            json.append("\"proximoPassoTexto\":\"").append(proximoTxt).append("\",");
            json.append("\"deltaTempo\":1.5,");
            json.append("\"tempoTotalDecorrido\":").append(String.format(Locale.US, "%.1f", tempoTotalSegundos)).append(",");
            json.append("\"velocidadePorMinuto\":").append(velocidadeEst).append(",");
            json.append("\"ultimaTransacao\":{")
                .append("\"id\":").append(txExibir.getId()).append(",")
                .append("\"origem\":").append(txExibir.getIdContaOrigem()).append(",")
                .append("\"destino\":").append(txExibir.getIdContaDestino()).append(",")
                .append("\"valor\":").append(txExibir.getValorCentavos()).append(",")
                .append("\"score\":").append(String.format(Locale.US, "%.1f", txExibir.getScoreFraude())).append(",")
                .append("\"status\":\"").append(txExibir.getStatus().name()).append("\",")
                .append("\"motivo\":\"").append(txExibir.getMotivoFraude().replace("\"", "'")).append("\"")
                .append("},");

            json.append("\"arestas\":[");
            for (int i = 0; i < passoBurstAtual && i < DESTINOS_BURST.length; i++) {
                int d = DESTINOS_BURST[i];
                String st = (i >= 3) ? "SUSPEITA" : "APROVADA";
                json.append("{\"origem\":25,\"destino\":").append(d).append(",\"status\":\"").append(st).append("\"}");
                if (i < passoBurstAtual - 1 && i < DESTINOS_BURST.length - 1) json.append(",");
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // =========================================================
    // DEMAIS HANDLERS (CONTAS, GERAL)
    // =========================================================
    static class ListarTodasContasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < CONTAS_SISTEMA.length; i++) {
                int id = CONTAS_SISTEMA[i];
                Conta conta = motor.getTreapContas().obterOuCriar(id, 100_000_00L, 2_000_00L);
                json.append("{")
                    .append("\"id\":").append(conta.getIdConta()).append(",")
                    .append("\"saldo\":").append(conta.getSaldoCentavos()).append(",")
                    .append("\"volumeMedio\":").append(conta.getVolumeMedioDiarioCentavos()).append(",")
                    .append("\"scoreRisco\":").append(String.format(Locale.US, "%.2f", conta.getScoreRisco())).append(",")
                    .append("\"retencao\":").append(String.format(Locale.US, "%.2f", conta.calcularTaxaRetencao()))
                    .append("}");
                if (i < CONTAS_SISTEMA.length - 1) json.append(",");
            }
            json.append("]");
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}