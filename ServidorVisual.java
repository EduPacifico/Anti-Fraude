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
import java.util.Random;

public class ServidorVisual {
    private static MotorFraude motor;
    private static final int[] CONTAS_SISTEMA = {10, 15, 20, 25, 30, 40, 50, 60, 70, 80};
    private static final List<Transacao> historicoDecisoes = new ArrayList<>();
    
    // Contadores de passos guiados
    private static int passoSmurfingAtual = 0;
    private static int passoCicloAtual = 0;
    private static int passoFanInAtual = 0;
    private static int passoBurstAtual = 0;
    
    private static final int[] ORIGENS_FAN_IN = {10, 15, 20, 25, 30};
    private static final int[] DESTINOS_BURST = {50, 60, 70, 80, 10, 30};
    
    private static String ultimaMerkleRoot = "Nenhum bloco selado ainda";
    private static int totalBlocosSelados = 0;
    private static long contadorId = 1000L;
    private static long timestampAtual = System.currentTimeMillis();
    private static final Random random = new Random(42);

    public static void main(String[] args) throws IOException {
        reiniciarSistema();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new PaginaPrincipalHandler());
        server.createContext("/api/smurfing/passo", new SmurfingPassoHandler());
        server.createContext("/api/smurfing/reset", new SmurfingResetHandler());
        server.createContext("/api/ciclo/passo", new CicloPassoHandler());
        server.createContext("/api/ciclo/reset", new CicloResetHandler());
        server.createContext("/api/fanin/passo", new FanInPassoHandler());
        server.createContext("/api/fanin/reset", new FanInResetHandler());
        server.createContext("/api/burst/passo", new BurstPassoHandler());
        server.createContext("/api/burst/reset", new BurstResetHandler());
        server.createContext("/api/geral/processar", new GeralProcessarHandler());
        server.createContext("/api/contas/todas", new ListarTodasContasHandler());
        server.createContext("/api/conta/extrato", new ExtratoContaHandler());
        server.setExecutor(null);

        System.out.println("=================================================================");
        System.out.println(" Servidor Visual com Auditoria Detalhada Iniciado!");
        System.out.println(" Acesse no navegador: http://localhost:8080");
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
        ultimaMerkleRoot = "Nenhum bloco selado ainda";
        totalBlocosSelados = 0;

        for (int id : CONTAS_SISTEMA) {
            motor.getTreapContas().obterOuCriar(id, 100_000_00L, 2_000_00L);
        }
    }

    private static void checarSelagemMerkle() {
        if (historicoDecisoes.size() > 0 && historicoDecisoes.size() % 10 == 0) {
            int inicio = historicoDecisoes.size() - 10;
            List<Transacao> lote = historicoDecisoes.subList(inicio, historicoDecisoes.size());
            ArvoreMerkle arvore = new ArvoreMerkle(lote);
            ultimaMerkleRoot = arvore.getRaizMerkle();
            totalBlocosSelados++;
        }
    }

    static class PaginaPrincipalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <title>Sistema Antifraude - Auditoria Operacional</title>
                <style>
                    * { box-sizing: border-box; }
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #070b14; color: #e2e8f0; margin: 0; padding: 20px; }
                    .container { max-width: 1440px; margin: 0 auto; }
                    
                    .nav-tabs { display: flex; gap: 8px; border-bottom: 2px solid #1e293b; margin-bottom: 18px; flex-wrap: wrap; }
                    .tab-btn { background: #0f172a; color: #94a3b8; border: 1px solid #1e293b; border-bottom: none; padding: 11px 16px; border-radius: 8px 8px 0 0; cursor: pointer; font-weight: 600; font-size: 0.88rem; transition: 0.2s; }
                    .tab-btn:hover { background: #1e293b; color: #f8fafc; }
                    .tab-btn.active { background: #0284c7; color: white; border-color: #0284c7; }

                    .tab-content { display: none; }
                    .tab-content.active { display: block; }

                    .card { background: #0f172a; border: 1px solid #1e293b; border-radius: 12px; padding: 18px; box-shadow: 0 8px 24px rgba(0,0,0,0.4); margin-bottom: 16px; }
                    .grid-main { display: grid; grid-template-columns: 1.05fr 0.95fr; gap: 16px; }
                    
                    h2 { margin: 0 0 12px 0; color: #38bdf8; font-size: 1.1rem; }
                    p { margin: 0 0 12px 0; font-size: 0.88rem; color: #94a3b8; line-height: 1.4; }

                    button { background: #0284c7; color: white; border: none; padding: 9px 16px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 0.88rem; transition: 0.15s; margin-right: 8px; margin-bottom: 8px; }
                    button:hover { background: #0369a1; transform: translateY(-1px); }
                    button.warning { background: #d97706; }
                    button.warning:hover { background: #b45309; }
                    button.danger { background: #e11d48; }
                    button.danger:hover { background: #be123c; }
                    button.neutral { background: #334155; }
                    button.neutral:hover { background: #475569; }
                    button.sm { padding: 5px 10px; font-size: 0.78rem; }

                    canvas { background: #020617; border-radius: 8px; border: 1px solid #1e293b; width: 100%; height: 350px; }
                    
                    .step-container { display: flex; flex-direction: column; gap: 12px; }
                    .step-card { background: #020617; border-left: 4px solid #38bdf8; border-radius: 0 8px 8px 0; padding: 14px 16px; border-top: 1px solid #1e293b; border-right: 1px solid #1e293b; border-bottom: 1px solid #1e293b; }
                    .step-card.alert { border-left-color: #f43f5e; background: rgba(225, 29, 72, 0.05); }
                    .step-card.warning { border-left-color: #fbbf24; background: rgba(245, 158, 11, 0.05); }
                    .step-card.ok { border-left-color: #34d399; }
                    .step-title { font-weight: bold; font-size: 0.88rem; margin-bottom: 6px; display: flex; justify-content: space-between; align-items: center; }
                    .step-desc { font-size: 0.82rem; color: #cbd5e1; line-height: 1.5; font-family: Consolas, monospace; }

                    .badge { padding: 4px 8px; border-radius: 4px; font-weight: 700; font-size: 0.72rem; display: inline-block; }
                    .APROVADA, .REGULAR { background: rgba(16, 185, 129, 0.2); color: #34d399; border: 1px solid #059669; }
                    .SUSPEITA, .MODERADO { background: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid #d97706; }
                    .BLOQUEADA, .CRITICO { background: rgba(225, 29, 72, 0.2); color: #f43f5e; border: 1px solid #e11d48; }
                    .LARANJA { background: rgba(168, 85, 247, 0.2); color: #c084fc; border: 1px solid #9333ea; }

                    .table-wrapper { max-height: 320px; overflow-y: auto; border: 1px solid #1e293b; border-radius: 8px; }
                    table { width: 100%; border-collapse: collapse; font-size: 0.84rem; text-align: left; }
                    th { background: #020617; color: #94a3b8; padding: 10px 12px; position: sticky; top: 0; font-size: 0.76rem; text-transform: uppercase; }
                    td { padding: 9px 12px; border-top: 1px solid #1e293b; }

                    .metric-badge { background: #0f172a; border: 1px solid #334155; padding: 6px 12px; border-radius: 6px; font-size: 0.82rem; font-weight: bold; color: #38bdf8; display: inline-block; margin-bottom: 10px; }
                    
                    .extrato-header { background: #020617; border: 1px solid #334155; border-radius: 8px; padding: 14px; margin-bottom: 12px; display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
                    .extrato-stat-title { font-size: 0.72rem; color: #94a3b8; text-transform: uppercase; }
                    .extrato-stat-val { font-size: 1.15rem; font-weight: bold; color: #f8fafc; margin-top: 2px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="nav-tabs">
                        <button class="tab-btn active" onclick="abrirAba('tela-contas', this)">Contas e Extratos (Treap)</button>
                        <button class="tab-btn" onclick="abrirAba('tela-smurfing', this)">Caso 1: Smurfing</button>
                        <button class="tab-btn" onclick="abrirAba('tela-ciclo', this)">Caso 2: Anel de Lavagem</button>
                        <button class="tab-btn" onclick="abrirAba('tela-fanin', this)">Caso 3: Concentracao (Fan-in)</button>
                        <button class="tab-btn" onclick="abrirAba('tela-burst', this)">Caso 4: Explosao de Velocidade</button>
                        <button class="tab-btn" onclick="abrirAba('tela-fluxo', this)">Caso 5: Rede e Merkle Tree</button>
                    </div>

                    <!-- TELA 0: VISÃO GERAL DE CONTAS E EXTRATOS -->
                    <div id="tela-contas" class="tab-content active">
                        <div class="card">
                            <h2>Perfis de Contas na Treap O(log N)</h2>
                            <p>Saldos e scores de risco mantidos em tempo real na arvore. Clique em <strong>Ver Extrato</strong> para auditar o historico contabil detalhado de qualquer conta:</p>
                            <button onclick="carregarTodasContas()">Atualizar Tabela</button>
                            <span class="metric-badge" id="badgeTotalContas">10 Contas Monitoradas na Treap</span>
                        </div>

                        <div class="card">
                            <h2>Tabela Geral de Contas</h2>
                            <div class="table-wrapper">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>ID da Conta</th>
                                            <th>Saldo Atual</th>
                                            <th>Media Diaria</th>
                                            <th>Taxa de Retencao</th>
                                            <th>Score de Risco</th>
                                            <th>Classificacao</th>
                                            <th>Acao</th>
                                        </tr>
                                    </thead>
                                    <tbody id="tabelaContasCorpo">
                                        <tr><td colspan="7" style="text-align:center; color:#94a3b8;">Carregando dados da Treap...</td></tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <div class="card" id="cardExtratoConta">
                            <h2 id="tituloExtrato">Extrato da Conta Selecionada</h2>
                            <div id="conteudoExtrato">
                                <p style="color:#94a3b8;">Selecione uma conta na tabela acima para carregar o extrato bancario.</p>
                            </div>
                        </div>
                    </div>

                    <!-- TELA 1: SMURFING PASSO A PASSO -->
                    <div id="tela-smurfing" class="tab-content">
                        <div class="card">
                            <h2>Cenario: Pulverizacao de R$ 48.000,00 (Conta 15 -> Conta 40)</h2>
                            <p>Envio de 5 transferencias de R$ 9.600,00 para burlar o teto unitario. Avance transacao por transacao:</p>
                            <button class="warning" onclick="proximoPassoSmurfing()">Executar Proxima Transacao (<span id="btnTxtSmurfing">Passo 1 de 5</span>)</button>
                            <button class="neutral" onclick="resetarSmurfing()">Reiniciar Smurfing</button>
                            <span class="metric-badge" id="badgeJanelaSmurfing">Buffer Janela: 0 transferencias | R$ 0,00 acumulados</span>
                        </div>

                        <div class="grid-main">
                            <div class="card">
                                <h2>Topologia de Envio (Conta 15 -> Conta 40)</h2>
                                <canvas id="canvasSmurfing" width="600" height="350"></canvas>
                            </div>
                            <div class="card">
                                <h2>Auditoria Operacional das 4 Checagens</h2>
                                <div id="passosSmurfingLog" class="step-container">
                                    <div style="color:#94a3b8; font-size:0.85rem;">Clique em Executar Proxima Transacao para iniciar o teste.</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- TELA 2: ANEL DE LAVAGEM PASSO A PASSO -->
                    <div id="tela-ciclo" class="tab-content">
                        <div class="card">
                            <h2>Cenario: Anel Fechado de Lavagem (20 -> 30 -> 40 -> 20)</h2>
                            <p>Circulacao de R$ 50.000,00 por intermediarios para ocultar a origem e retornar a conta 20:</p>
                            <button class="danger" onclick="proximoPassoCiclo()">Executar Proxima Perna (<span id="btnTxtCiclo">Perna 1: 20 -> 30</span>)</button>
                            <button class="neutral" onclick="resetarCiclo()">Reiniciar Anel</button>
                            <span class="metric-badge" id="badgeCicloStatus">Estado do Grafo: Vertices sem arestas</span>
                        </div>

                        <div class="grid-main">
                            <div class="card">
                                <h2>Grafo Transacional do Anel</h2>
                                <canvas id="canvasCiclo" width="600" height="350"></canvas>
                            </div>
                            <div class="card">
                                <h2>Rastreamento da Bounded DFS e Decisao</h2>
                                <div id="passosCicloLog" class="step-container">
                                    <div style="color:#94a3b8; font-size:0.85rem;">Clique em Executar Proxima Perna para iniciar a circulacao do capital.</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- TELA 3: CONCENTRAÇÃO (FAN-IN) -->
                    <div id="tela-fanin" class="tab-content">
                        <div class="card">
                            <h2>Cenario: Funil de Concentracao (Contas 10, 15, 20, 25, 30 -> Conta 80)</h2>
                            <p>Multiplas origens depositando para uma unica conta arrecadadora (Conta 80). Acompanhe o calculo de In-Degree no Grafo:</p>
                            <button class="danger" onclick="proximoPassoFanIn()">Executar Proximo Aporte (<span id="btnTxtFanIn">Passo 1 de 5: Conta 10 -> 80</span>)</button>
                            <button class="neutral" onclick="resetarFanIn()">Reiniciar Fan-in</button>
                            <span class="metric-badge" id="badgeFanInStatus">Grau de Entrada (In-Degree) da Conta 80: 0</span>
                        </div>

                        <div class="grid-main">
                            <div class="card">
                                <h2>Topologia em Funil (N -> 1)</h2>
                                <canvas id="canvasFanIn" width="600" height="350"></canvas>
                            </div>
                            <div class="card">
                                <h2>Auditoria de In-Degree e Concentracao</h2>
                                <div id="passosFanInLog" class="step-container">
                                    <div style="color:#94a3b8; font-size:0.85rem;">Clique em Executar Proximo Aporte para iniciar o funil de depositos.</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- TELA 4: EXPLOSÃO DE VELOCIDADE TEMPORAL (BURST) -->
                    <div id="tela-burst" class="tab-content">
                        <div class="card">
                            <h2>Cenario: Rajada Relampago de Disparos (Conta 25 em Alta Frequencia)</h2>
                            <p>A Conta 25 dispara transferencias com intervalos de apenas <strong>1.5 segundos</strong> (delta_t &le; 2s), simulando script automatizado:</p>
                            <button class="danger" onclick="proximoPassoBurst()">Disparar Proxima Transacao Rapida (<span id="btnTxtBurst">Disparo 1 de 5</span>)</button>
                            <button class="neutral" onclick="resetarBurst()">Reiniciar Rajada</button>
                            <span class="metric-badge" id="badgeBurstStatus">Velocidade: 0 disparos / min</span>
                        </div>

                        <div class="grid-main">
                            <div class="card">
                                <h2>Dispersao Radial em Alta Velocidade (Conta 25)</h2>
                                <canvas id="canvasBurst" width="600" height="350"></canvas>
                            </div>
                            <div class="card">
                                <h2>Auditoria de Velocidade Temporal e Janela Deslizante</h2>
                                <div id="passosBurstLog" class="step-container">
                                    <div style="color:#94a3b8; font-size:0.85rem;">Clique em Disparar Proxima Transacao Rapida para iniciar a rajada.</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- TELA 5: REDE GERAL & AUDITORIA MERKLE -->
                    <div id="tela-fluxo" class="tab-content">
                        <div class="card">
                            <h2>Simulacao Geral e Selagem Criptografica</h2>
                            <button onclick="executarGeral('legitima')">Transacao Legitima Aleatoria</button>
                            <button class="neutral" onclick="executarGeral('avancar_tempo')">Avancar Tempo (+15 min / Expurgar)</button>
                            <button class="neutral" onclick="executarGeral('limpar')">Resetar Tudo</button>
                            <span class="metric-badge" id="badgeGeralMerkle">Blocos Merkle Selados: 0</span>
                        </div>

                        <div class="grid-main">
                            <div class="card">
                                <h2>Rede Completa (10 Contas Monitoradas)</h2>
                                <canvas id="canvasGeral" width="600" height="350"></canvas>
                            </div>
                            <div class="card">
                                <h2>Log Historico de Decisoes</h2>
                                <div class="table-wrapper">
                                    <table>
                                        <thead>
                                            <tr>
                                                <th>ID</th>
                                                <th>Origem -> Destino</th>
                                                <th>Valor</th>
                                                <th>Score</th>
                                                <th>Status</th>
                                                <th>Motivo</th>
                                            </tr>
                                        </thead>
                                        <tbody id="tabelaGeralCorpo"></tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <script>
                    let contaAtivaExtrato = 80;

                    function abrirAba(abaId, btn) {
                        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
                        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
                        btn.classList.add('active');
                        document.getElementById(abaId).classList.add('active');
                        
                        if (abaId === 'tela-contas') carregarTodasContas();
                        if (abaId === 'tela-smurfing') desenharCanvasSmurfing(arestasSmurfing);
                        if (abaId === 'tela-ciclo') desenharCanvasCiclo(arestasCiclo);
                        if (abaId === 'tela-fanin') desenharCanvasFanIn(arestasFanIn);
                        if (abaId === 'tela-burst') desenharCanvasBurst(arestasBurst);
                        if (abaId === 'tela-fluxo') desenharCanvasGeral(arestasGeral);
                    }

                    // =========================================================
                    // 0. CONTAS E EXTRATOS
                    // =========================================================
                    async function carregarTodasContas() {
                        const res = await fetch('/api/contas/todas');
                        const contas = await res.json();
                        
                        const tbody = document.getElementById('tabelaContasCorpo');
                        tbody.innerHTML = '';

                        contas.forEach(c => {
                            let badgeClass = 'REGULAR';
                            let badgeLabel = 'NORMAL';

                            if (c.scoreRisco >= 75.0) {
                                badgeClass = 'CRITICO';
                                badgeLabel = 'ALTO RISCO (BLOQUEADA)';
                            } else if (c.retencao < 0.15 && c.scoreRisco >= 20.0) {
                                badgeClass = 'LARANJA';
                                badgeLabel = 'CONTA DE PASSAGEM / LARANJA';
                            } else if (c.scoreRisco >= 35.0) {
                                badgeClass = 'MODERADO';
                                badgeLabel = 'SUSPEITA MODERADA';
                            }

                            const tr = document.createElement('tr');
                            tr.innerHTML = `
                                <td><strong>Conta #${c.id}</strong></td>
                                <td style="color:#34d399; font-weight:600;">R$ ${(c.saldo / 100).toFixed(2)}</td>
                                <td>R$ ${(c.volumeMedio / 100).toFixed(2)}</td>
                                <td>${(c.retencao * 100).toFixed(1)}%</td>
                                <td><strong style="color:${c.scoreRisco > 50 ? '#f43f5e' : (c.scoreRisco > 30 ? '#fbbf24' : '#38bdf8')};">${c.scoreRisco.toFixed(1)}</strong> / 100</td>
                                <td><span class="badge ${badgeClass}">${badgeLabel}</span></td>
                                <td><button class="neutral sm" onclick="carregarExtratoConta(${c.id})">Ver Extrato</button></td>
                            `;
                            tbody.appendChild(tr);
                        });

                        if (contaAtivaExtrato) {
                            carregarExtratoConta(contaAtivaExtrato);
                        }
                    }

                    async function carregarExtratoConta(idConta) {
                        contaAtivaExtrato = idConta;
                        const res = await fetch('/api/conta/extrato?id=' + idConta);
                        const data = await res.json();

                        document.getElementById('tituloExtrato').innerText = 'Extrato Detalhado da Conta #' + idConta;

                        let html = `
                            <div class="extrato-header">
                                <div>
                                    <div class="extrato-stat-title">Saldo Disponivel</div>
                                    <div class="extrato-stat-val" style="color:#34d399;">R$ ${(data.conta.saldo / 100).toFixed(2)}</div>
                                </div>
                                <div>
                                    <div class="extrato-stat-title">Total Enviado (Saidas)</div>
                                    <div class="extrato-stat-val" style="color:#f43f5e;">R$ ${(data.totalEnviado / 100).toFixed(2)}</div>
                                </div>
                                <div>
                                    <div class="extrato-stat-title">Total Recebido (Entradas)</div>
                                    <div class="extrato-stat-val" style="color:#38bdf8;">R$ ${(data.totalRecebido / 100).toFixed(2)}</div>
                                </div>
                                <div>
                                    <div class="extrato-stat-title">Score de Risco Acumulado</div>
                                    <div class="extrato-stat-val" style="color:${data.conta.scoreRisco > 50 ? '#f43f5e' : '#38bdf8'};">${data.conta.scoreRisco.toFixed(1)} / 100</div>
                                </div>
                            </div>

                            <div class="table-wrapper">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>ID Tx</th>
                                            <th>Operacao / Fluxo</th>
                                            <th>Valor</th>
                                            <th>Score Tx</th>
                                            <th>Decisao</th>
                                            <th>Motivo / Diagnostico</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                        `;

                        if (data.transacoes.length === 0) {
                            html += '<tr><td colspan="6" style="text-align:center; color:#94a3b8;">Nenhuma transacao registrada para a Conta #' + idConta + ' ainda.</td></tr>';
                        } else {
                            data.transacoes.slice().reverse().forEach(tx => {
                                const isSaida = (tx.origem === idConta);
                                const tipoFluxo = isSaida 
                                    ? '<span style="color:#f43f5e; font-weight:bold;">[SAIDA] Enviado para Conta #' + tx.destino + '</span>' 
                                    : '<span style="color:#34d399; font-weight:bold;">[ENTRADA] Recebido da Conta #' + tx.origem + '</span>';

                                html += `
                                    <tr>
                                        <td><strong>#${tx.id}</strong></td>
                                        <td>${tipoFluxo}</td>
                                        <td><strong>R$ ${(tx.valor / 100).toFixed(2)}</strong></td>
                                        <td><strong>${tx.score.toFixed(1)}</strong></td>
                                        <td><span class="badge ${tx.status}">${tx.status}</span></td>
                                        <td style="color:#cbd5e1; font-family:monospace; font-size:0.75rem;">${tx.motivo}</td>
                                    </tr>
                                `;
                            });
                        }

                        html += '</tbody></table></div>';
                        document.getElementById('conteudoExtrato').innerHTML = html;
                    }

                    function desenharSeta(ctx, fromX, fromY, toX, toY, cor, espessura) {
                        const headlen = 11;
                        const angle = Math.atan2(toY - fromY, toX - fromX);
                        const raioNo = 22;
                        const inicioX = fromX + raioNo * Math.cos(angle);
                        const inicioY = fromY + raioNo * Math.sin(angle);
                        const fimX = toX - raioNo * Math.cos(angle);
                        const fimY = toY - raioNo * Math.sin(angle);

                        ctx.beginPath();
                        ctx.moveTo(inicioX, inicioY);
                        ctx.lineTo(fimX, fimY);
                        ctx.strokeStyle = cor;
                        ctx.lineWidth = espessura;
                        ctx.stroke();

                        ctx.beginPath();
                        ctx.moveTo(fimX, fimY);
                        ctx.lineTo(fimX - headlen * Math.cos(angle - Math.PI / 6), fimY - headlen * Math.sin(angle - Math.PI / 6));
                        ctx.lineTo(fimX - headlen * Math.cos(angle + Math.PI / 6), fimY - headlen * Math.sin(angle + Math.PI / 6));
                        ctx.fillStyle = cor;
                        ctx.fill();
                    }

                    // =========================================================
                    // 1. SMURFING
                    // =========================================================
                    let arestasSmurfing = [];

                    function desenharCanvasSmurfing(arestas) {
                        const canvas = document.getElementById('canvasSmurfing');
                        if (!canvas) return;
                        const ctx = canvas.getContext('2d');
                        ctx.clearRect(0, 0, canvas.width, canvas.height);

                        const p15 = {x: 140, y: 175};
                        const p40 = {x: 460, y: 175};

                        arestas.forEach(a => {
                            desenharSeta(ctx, p15.x, p15.y, p40.x, p40.y, a.status === 'SUSPEITA' ? '#fbbf24' : '#38bdf8', a.status === 'SUSPEITA' ? 4 : 2);
                        });

                        [ {id: 15, x: p15.x, y: p15.y}, {id: 40, x: p40.x, y: p40.y} ].forEach(n => {
                            ctx.beginPath();
                            ctx.arc(n.x, n.y, 24, 0, 2 * Math.PI);
                            ctx.fillStyle = '#1e293b';
                            ctx.fill();
                            ctx.strokeStyle = (arestas.length >= 5 && n.id === 15) ? '#fbbf24' : '#64748b';
                            ctx.lineWidth = 2.5;
                            ctx.stroke();

                            ctx.fillStyle = '#f8fafc';
                            ctx.font = 'bold 12px sans-serif';
                            ctx.textAlign = 'center';
                            ctx.textBaseline = 'middle';
                            ctx.fillText('C' + n.id, n.x, n.y);
                        });
                    }

                    async function proximoPassoSmurfing() {
                        const res = await fetch('/api/smurfing/passo');
                        const data = await res.json();

                        document.getElementById('btnTxtSmurfing').innerText = data.proximoPassoTexto;
                        document.getElementById('badgeJanelaSmurfing').innerText = `Buffer Janela: ${data.bufferQtd} transferencias | R$ ${(data.bufferValor/100).toFixed(2)} acumulados`;
                        
                        arestasSmurfing = data.arestas;
                        desenharCanvasSmurfing(arestasSmurfing);

                        const tx = data.ultimaTransacao;
                        const isFraude = tx.status === 'SUSPEITA' || tx.status === 'BLOQUEADA';

                        document.getElementById('passosSmurfingLog').innerHTML = `
                            <div style="font-weight:bold; color:#38bdf8; margin-bottom:8px;">
                                [Transacao #${tx.id}] Conta 15 -> Conta 40 (R$ ${(tx.valor/100).toFixed(2)})
                            </div>

                            <div class="step-card ok">
                                <div class="step-title"><span>1. Validacao na Treap O(log N)</span> <span style="color:#34d399;">CONCLUIDO</span></div>
                                <div class="step-desc">
                                    - Saldo Conta 15: R$ ${(data.saldoAnterior/100).toFixed(2)} - R$ ${(tx.valor/100).toFixed(2)} = <span style="color:#34d399;">R$ ${(data.novoSaldo/100).toFixed(2)}</span> (Suficiente)<br>
                                    - Checagem de Media: R$ ${(tx.valor/100).toFixed(2)} > 3 x R$ 2.000,00 (R$ 6.000,00) -> <span style="color:#fbbf24;">+25.0 pontos (Valor Atipico)</span>
                                </div>
                            </div>

                            <div class="step-card ${isFraude ? 'alert' : 'ok'}">
                                <div class="step-title"><span>2. Janela Deslizante (lower_bound O(log K))</span> <span style="color:${isFraude ? '#f43f5e' : '#34d399'};">${isFraude ? 'SMURFING CONFIRMADO' : 'MONITORANDO BUFFER'}</span></div>
                                <div class="step-desc">
                                    - Intervalo Temporal de Busca: [t - 600s, t]<br>
                                    - Transferencias Ativas no Buffer: <strong>${data.bufferQtd} transacoes</strong><br>
                                    - Volume Acumulado: <strong>R$ ${(data.bufferValor/100).toFixed(2)}</strong> (Limite Maximo Tolerado: R$ 25.000,00)<br>
                                    - Diagnostico: ${isFraude ? '<span style="color:#f43f5e; font-weight:bold;">Volume R$ 48.000,00 > R$ 25.000,00 -> Flag de Smurfing disparada (+45.0 pontos)</span>' : '<span style="color:#34d399;">Volume acumulado dentro do teto seguro da janela movel.</span>'}
                                </div>
                            </div>

                            <div class="step-card ok">
                                <div class="step-title"><span>3. Grafo Transacional (Adjacencia)</span> <span style="color:#34d399;">SEM CICLOS</span></div>
                                <div class="step-desc">
                                    - Insercao de Aresta Direcionada: Conta 15 -> Conta 40 [R$ ${(tx.valor/100).toFixed(2)} | t=${tx.id}]<br>
                                    - Grau de Saida (Out-Degree): 1 | Grau de Entrada (In-Degree): ${data.bufferQtd}
                                </div>
                            </div>

                            <div class="step-card ${isFraude ? 'warning' : 'ok'}">
                                <div class="step-title"><span>4. Equacao de Decisao do Motor</span> <span class="badge ${tx.status}">${tx.status}</span></div>
                                <div class="step-desc">
                                    - Equacao do Score: ${isFraude ? '25.0 (Valor Atipico) + 45.0 (Smurfing) = <strong>70.0 / 100</strong>' : '25.0 (Valor Atipico) = <strong>25.0 / 100</strong>'}<br>
                                    - Acao no Perfil da Conta 15: ${isFraude ? '<span style="color:#fbbf24;">Score permanente da Conta 15 elevado para 56.0 na Treap.</span>' : 'Score mantido em nivel regular.'}
                                </div>
                            </div>
                        `;
                    }

                    async function resetarSmurfing() {
                        await fetch('/api/smurfing/reset');
                        arestasSmurfing = [];
                        document.getElementById('btnTxtSmurfing').innerText = 'Passo 1 de 5';
                        document.getElementById('badgeJanelaSmurfing').innerText = 'Buffer Janela: 0 transferencias | R$ 0,00 acumulados';
                        document.getElementById('passosSmurfingLog').innerHTML = '<div style="color:#94a3b8; font-size:0.85rem;">Cenario de smurfing reiniciado.</div>';
                        desenharCanvasSmurfing([]);
                    }

                    // =========================================================
                    // 2. CICLO DE LAVAGEM
                    // =========================================================
                    let arestasCiclo = [];
                    const posCiclo = { 20: {x: 300, y: 80}, 30: {x: 480, y: 260}, 40: {x: 120, y: 260} };

                    function desenharCanvasCiclo(arestas) {
                        const canvas = document.getElementById('canvasCiclo');
                        if (!canvas) return;
                        const ctx = canvas.getContext('2d');
                        ctx.clearRect(0, 0, canvas.width, canvas.height);

                        arestas.forEach(a => {
                            const o = posCiclo[a.origem];
                            const d = posCiclo[a.destino];
                            if (o && d) {
                                desenharSeta(ctx, o.x, o.y, d.x, d.y, a.status === 'BLOQUEADA' ? '#f43f5e' : '#38bdf8', a.status === 'BLOQUEADA' ? 4 : 2.5);
                            }
                        });

                        Object.keys(posCiclo).forEach(id => {
                            const p = posCiclo[id];
                            ctx.beginPath();
                            ctx.arc(p.x, p.y, 22, 0, 2 * Math.PI);
                            ctx.fillStyle = '#1e293b';
                            ctx.fill();
                            ctx.strokeStyle = '#64748b';
                            ctx.lineWidth = 2;
                            ctx.stroke();

                            ctx.fillStyle = '#f8fafc';
                            ctx.font = 'bold 12px sans-serif';
                            ctx.textAlign = 'center';
                            ctx.textBaseline = 'middle';
                            ctx.fillText('C' + id, p.x, p.y);
                        });
                    }

                    async function proximoPassoCiclo() {
                        const res = await fetch('/api/ciclo/passo');
                        const data = await res.json();

                        document.getElementById('btnTxtCiclo').innerText = data.proximoPassoTexto;
                        document.getElementById('badgeCicloStatus').innerText = data.statusGrafo;

                        arestasCiclo = data.arestas;
                        desenharCanvasCiclo(arestasCiclo);

                        const tx = data.ultimaTransacao;
                        const isBloqueio = tx.status === 'BLOQUEADA';

                        document.getElementById('passosCicloLog').innerHTML = `
                            <div style="font-weight:bold; color:#38bdf8; margin-bottom:8px;">
                                [Perna ${data.passoAtual} de 3] Transacao #${tx.id}: Conta ${tx.origem} -> Conta ${tx.destino} (R$ ${(tx.valor/100).toFixed(2)})
                            </div>

                            <div class="step-card ok">
                                <div class="step-title"><span>1. Atualizacao Contabil na Treap</span> <span style="color:#34d399;">CONCLUIDO</span></div>
                                <div class="step-desc">
                                    - Conta Origem (#${tx.origem}): Saldo R$ ${(data.saldoOrigem/100).toFixed(2)} -> Retencao: ${(data.retencaoOrigem*100).toFixed(1)}%<br>
                                    - Conta Destino (#${tx.destino}): Saldo R$ ${(data.saldoDestino/100).toFixed(2)}
                                </div>
                            </div>

                            <div class="step-card ${isBloqueio ? 'alert' : 'ok'}">
                                <div class="step-title"><span>2. Execucao da Bounded DFS (Profundidade Limitada &le; 4)</span> <span style="color:${isBloqueio ? '#f43f5e' : '#34d399'};">${isBloqueio ? 'CICLO FECHADO DETECTADO' : 'CAMINHO ABERTO'}</span></div>
                                <div class="step-desc">
                                    ${data.dfsDiagnostico}
                                </div>
                            </div>

                            <div class="step-card ${isBloqueio ? 'alert' : 'ok'}">
                                <div class="step-title"><span>3. Decisao do Motor e Propagacao de Risco</span> <span class="badge ${tx.status}">${tx.status}</span></div>
                                <div class="step-desc">
                                    - Score Calculado: 25.0 (Valor) + ${isBloqueio ? '85.0 (Anel de Lavagem) = <strong>100.0 / 100</strong>' : '0.0 = <strong>25.0 / 100</strong>'}<br>
                                    - Diagnostico: ${tx.motivo}<br>
                                    - Efeito na Rede: ${isBloqueio ? '<span style="color:#f43f5e; font-weight:bold;">Contas [20, 30, 40] marcadas com score 85.0 (Alto Risco) na Treap!</span>' : 'Perna intermediaria aprovada no pipeline.'}
                                </div>
                            </div>
                        `;
                    }

                    async function resetarCiclo() {
                        await fetch('/api/ciclo/reset');
                        arestasCiclo = [];
                        document.getElementById('btnTxtCiclo').innerText = 'Perna 1: 20 -> 30';
                        document.getElementById('badgeCicloStatus').innerText = 'Estado do Grafo: Vertices sem arestas';
                        document.getElementById('passosCicloLog').innerHTML = '<div style="color:#94a3b8; font-size:0.85rem;">Anel reiniciado.</div>';
                        desenharCanvasCiclo([]);
                    }

                    // =========================================================
                    // 3. CONCENTRAÇÃO (FAN-IN)
                    // =========================================================
                    let arestasFanIn = [];
                    const posFanIn = {
                        10: {x: 100, y: 70}, 15: {x: 100, y: 175}, 20: {x: 100, y: 280},
                        25: {x: 500, y: 100}, 30: {x: 500, y: 250},
                        80: {x: 300, y: 175}
                    };

                    function desenharCanvasFanIn(arestas) {
                        const canvas = document.getElementById('canvasFanIn');
                        if (!canvas) return;
                        const ctx = canvas.getContext('2d');
                        ctx.clearRect(0, 0, canvas.width, canvas.height);

                        arestas.forEach(a => {
                            const o = posFanIn[a.origem];
                            const d = posFanIn[a.destino];
                            if (o && d) {
                                const cor = a.status === 'SUSPEITA' ? '#fbbf24' : (a.status === 'BLOQUEADA' ? '#f43f5e' : '#38bdf8');
                                desenharSeta(ctx, o.x, o.y, d.x, d.y, cor, 2.5);
                            }
                        });

                        Object.keys(posFanIn).forEach(id => {
                            const p = posFanIn[id];
                            const isCentral = (id == '80');

                            ctx.beginPath();
                            ctx.arc(p.x, p.y, isCentral ? 26 : 20, 0, 2 * Math.PI);
                            ctx.fillStyle = isCentral ? '#0f2942' : '#1e293b';
                            ctx.fill();
                            ctx.strokeStyle = isCentral ? (arestas.length >= 4 ? '#fbbf24' : '#38bdf8') : '#64748b';
                            ctx.lineWidth = isCentral ? 3 : 2;
                            ctx.stroke();

                            ctx.fillStyle = '#f8fafc';
                            ctx.font = isCentral ? 'bold 12px sans-serif' : 'bold 11px sans-serif';
                            ctx.textAlign = 'center';
                            ctx.textBaseline = 'middle';
                            ctx.fillText('C' + id + (isCentral ? ' (Alvo)' : ''), p.x, p.y);
                        });
                    }

                    async function proximoPassoFanIn() {
                        const res = await fetch('/api/fanin/passo');
                        const data = await res.json();

                        document.getElementById('btnTxtFanIn').innerText = data.proximoPassoTexto;
                        document.getElementById('badgeFanInStatus').innerText = `Grau de Entrada (In-Degree) da Conta 80: ${data.inDegree}`;

                        arestasFanIn = data.arestas;
                        desenharCanvasFanIn(arestasFanIn);

                        const tx = data.ultimaTransacao;
                        const isAlerta = (data.inDegree >= 4);

                        document.getElementById('passosFanInLog').innerHTML = `
                            <div style="font-weight:bold; color:#38bdf8; margin-bottom:8px;">
                                [Aporte ${data.passoAtual} de 5] Transacao #${tx.id}: Conta ${tx.origem} -> Conta 80 (R$ ${(tx.valor/100).toFixed(2)})
                            </div>

                            <div class="step-card ok">
                                <div class="step-title"><span>1. Consulta e Atualizacao na Treap</span> <span style="color:#34d399;">CONCLUIDO</span></div>
                                <div class="step-desc">
                                    - Saldo da Conta #${tx.origem}: R$ ${(data.saldoOrigem/100).toFixed(2)} (Debito confirmado)<br>
                                    - Saldo da Conta Centralizadora (#80): <span style="color:#34d399; font-weight:bold;">R$ ${(data.saldoDestino/100).toFixed(2)}</span> (Credito acumulado)
                                </div>
                            </div>

                            <div class="step-card ${isAlerta ? 'warning' : 'ok'}">
                                <div class="step-title"><span>2. Calculo Topologico de In-Degree no Grafo</span> <span style="color:${isAlerta ? '#fbbf24' : '#34d399'};">${isAlerta ? 'TOPOLOGIA DE FUNIL (FAN-IN)' : 'FLUXO CONVERGENTE'}</span></div>
                                <div class="step-desc">
                                    - Grau de Entrada da Conta 80: <strong>${data.inDegree} arestas ativas convergentes</strong><br>
                                    - Nos Conectados: { ${data.nosConectados} } -> Conta 80<br>
                                    - Diagnostico: ${isAlerta ? '<span style="color:#fbbf24; font-weight:bold;">In-Degree = ' + data.inDegree + ' &ge; 4 (Limiar de Concentracao atingido -> +45.0 pontos de risco)</span>' : '<span style="color:#34d399;">In-Degree = ' + data.inDegree + ' &lt; 4 (Dentro da tolerancia permitida)</span>'}
                                </div>
                            </div>

                            <div class="step-card ${isAlerta ? 'warning' : 'ok'}">
                                <div class="step-title"><span>3. Decisao do Motor e Classificacao</span> <span class="badge ${tx.status}">${tx.status}</span></div>
                                <div class="step-desc">
                                    - Score Calculado: 25.0 (Valor Atipico) + ${isAlerta ? '45.0 (Fan-in) = <strong>70.0 / 100</strong>' : '0.0 = <strong>25.0 / 100</strong>'}<br>
                                    - Diagnostico: ${tx.motivo}<br>
                                    - Acao: ${isAlerta ? '<span style="color:#fbbf24;">Conta 80 classificada como conta arrecadadora / mula receptora!</span>' : 'Aporte aprovado.'}
                                </div>
                            </div>
                        `;
                    }

                    async function resetarFanIn() {
                        await fetch('/api/fanin/reset');
                        arestasFanIn = [];
                        document.getElementById('btnTxtFanIn').innerText = 'Passo 1 de 5: Conta 10 -> 80';
                        document.getElementById('badgeFanInStatus').innerText = 'Grau de Entrada (In-Degree) da Conta 80: 0';
                        document.getElementById('passosFanInLog').innerHTML = '<div style="color:#94a3b8; font-size:0.85rem;">Cenario de Fan-in reiniciado.</div>';
                        desenharCanvasFanIn([]);
                    }

                    // =========================================================
                    // 4. EXPLOSÃO DE VELOCIDADE (BURST)
                    // =========================================================
                    let arestasBurst = [];
                    const posBurst = {
                        25: {x: 300, y: 175},
                        50: {x: 120, y: 80},
                        60: {x: 480, y: 80},
                        70: {x: 480, y: 270},
                        80: {x: 120, y: 270},
                        10: {x: 300, y: 40}
                    };

                    function desenharCanvasBurst(arestas) {
                        const canvas = document.getElementById('canvasBurst');
                        if (!canvas) return;
                        const ctx = canvas.getContext('2d');
                        ctx.clearRect(0, 0, canvas.width, canvas.height);

                        arestas.forEach(a => {
                            const o = posBurst[a.origem] || posBurst[25];
                            const d = posBurst[a.destino] || posBurst[50];
                            if (o && d) {
                                const cor = a.status === 'SUSPEITA' ? '#fbbf24' : (a.status === 'BLOQUEADA' ? '#f43f5e' : '#38bdf8');
                                desenharSeta(ctx, o.x, o.y, d.x, d.y, cor, 2.8);
                            }
                        });

                        Object.keys(posBurst).forEach(id => {
                            const p = posBurst[id];
                            const isOrigem = (id == '25');

                            ctx.beginPath();
                            ctx.arc(p.x, p.y, isOrigem ? 26 : 20, 0, 2 * Math.PI);
                            ctx.fillStyle = isOrigem ? '#2c1538' : '#1e293b';
                            ctx.fill();
                            ctx.strokeStyle = isOrigem ? (arestas.length >= 4 ? '#f43f5e' : '#a855f7') : '#64748b';
                            ctx.lineWidth = isOrigem ? 3.5 : 2;
                            ctx.stroke();

                            ctx.fillStyle = '#f8fafc';
                            ctx.font = isOrigem ? 'bold 12px sans-serif' : 'bold 11px sans-serif';
                            ctx.textAlign = 'center';
                            ctx.textBaseline = 'middle';
                            ctx.fillText('C' + id + (isOrigem ? ' (Bot)' : ''), p.x, p.y);
                        });
                    }

                    async function proximoPassoBurst() {
                        const res = await fetch('/api/burst/passo');
                        const data = await res.json();

                        document.getElementById('btnTxtBurst').innerText = data.proximoPassoTexto;
                        document.getElementById('badgeBurstStatus').innerText = `Velocidade: ${data.velocidadePorMinuto} txs/min (delta_t=${data.deltaTempo}s)`;

                        arestasBurst = data.arestas;
                        desenharCanvasBurst(arestasBurst);

                        const tx = data.ultimaTransacao;
                        const isAlerta = (data.passoAtual >= 4);

                        document.getElementById('passosBurstLog').innerHTML = `
                            <div style="font-weight:bold; color:#38bdf8; margin-bottom:8px;">
                                [Disparo ${data.passoAtual} de 5] Transacao #${tx.id}: Conta 25 -> Conta ${tx.destino} (R$ ${(tx.valor/100).toFixed(2)})
                            </div>

                            <div class="step-card ok">
                                <div class="step-title"><span>1. Delta Temporal na Treap</span> <span style="color:#34d399;">CONCLUIDO</span></div>
                                <div class="step-desc">
                                    - Intervalo desde a ultima transacao: <strong>delta_t = ${data.deltaTempo} segundos</strong><br>
                                    - Tempo Medio de Reacao Humana: 15 a 60 segundos -> <span style="color:#fbbf24;">Comportamento Nao-Humano Detectado</span>
                                </div>
                            </div>

                            <div class="step-card ${isAlerta ? 'alert' : 'ok'}">
                                <div class="step-title"><span>2. Janela Deslizante (Taxa de Disparos por Minuto)</span> <span style="color:${isAlerta ? '#f43f5e' : '#34d399'};">${isAlerta ? 'EXPLOSAO DE VELOCIDADE CONFIRMADA' : 'CADENCIA REGULAR'}</span></div>
                                <div class="step-desc">
                                    - Cadencia Temporal: <strong>${data.passoAtual} transferencias em ${data.tempoTotalDecorrido} segundos</strong><br>
                                    - Velocidade Equivalente: <strong>${data.velocidadePorMinuto} transacoes / minuto</strong> (Teto Seguro: 10 txs/min)<br>
                                    - Diagnostico: ${isAlerta ? '<span style="color:#f43f5e; font-weight:bold;">Taxa de disparos anomala -> Flag de Burst disparada (+30.0 pontos)</span>' : '<span style="color:#34d399;">Frequencia acumulada dentro da tolerancia inicial.</span>'}
                                </div>
                            </div>

                            <div class="step-card ${isAlerta ? 'warning' : 'ok'}">
                                <div class="step-title"><span>3. Decisao do Motor Antifraude</span> <span class="badge ${tx.status}">${tx.status}</span></div>
                                <div class="step-desc">
                                    - Score Calculado: 25.0 (Valor) + ${isAlerta ? '30.0 (Burst) + 15.0 (Fan-out) = <strong>70.0 / 100</strong>' : '0.0 = <strong>25.0 / 100</strong>'}<br>
                                    - Diagnostico: ${tx.motivo}<br>
                                    - Acao: ${isAlerta ? '<span style="color:#fbbf24;">Conta 25 sinalizada por automacao ilicita/invasao de conta!</span>' : 'Operacao aprovada.'}
                                </div>
                            </div>
                        `;
                    }

                    async function resetarBurst() {
                        await fetch('/api/burst/reset');
                        arestasBurst = [];
                        document.getElementById('btnTxtBurst').innerText = 'Disparo 1 de 5';
                        document.getElementById('badgeBurstStatus').innerText = 'Velocidade: 0 disparos / min';
                        document.getElementById('passosBurstLog').innerHTML = '<div style="color:#94a3b8; font-size:0.85rem;">Cenario de explosao de velocidade reiniciado.</div>';
                        desenharCanvasBurst([]);
                    }

                    // =========================================================
                    // 5. FLUXO GERAL
                    // =========================================================
                    const contasGeral = [10, 15, 20, 25, 30, 40, 50, 60, 70, 80];
                    const posGeral = {};
                    let arestasGeral = [];

                    contasGeral.forEach((id, i) => {
                        const angulo = (i / contasGeral.length) * 2 * Math.PI - Math.PI / 2;
                        posGeral[id] = {
                            x: 300 + 130 * Math.cos(angulo),
                            y: 175 + 130 * Math.sin(angulo)
                        };
                    });

                    function desenharCanvasGeral(arestas) {
                        const canvas = document.getElementById('canvasGeral');
                        if (!canvas) return;
                        const ctx = canvas.getContext('2d');
                        ctx.clearRect(0, 0, canvas.width, canvas.height);

                        arestas.forEach(a => {
                            const o = posGeral[a.origem];
                            const d = posGeral[a.destino];
                            if (o && d) desenharSeta(ctx, o.x, o.y, d.x, d.y, '#38bdf8', 1.8);
                        });

                        contasGeral.forEach(id => {
                            const p = posGeral[id];
                            ctx.beginPath();
                            ctx.arc(p.x, p.y, 18, 0, 2 * Math.PI);
                            ctx.fillStyle = '#1e293b';
                            ctx.fill();
                            ctx.strokeStyle = '#64748b';
                            ctx.lineWidth = 2;
                            ctx.stroke();

                            ctx.fillStyle = '#f8fafc';
                            ctx.font = 'bold 11px sans-serif';
                            ctx.textAlign = 'center';
                            ctx.textBaseline = 'middle';
                            ctx.fillText('C' + id, p.x, p.y);
                        });
                    }

                    async function executarGeral(tipo) {
                        const res = await fetch('/api/geral/processar?tipo=' + tipo);
                        const data = await res.json();

                        document.getElementById('badgeGeralMerkle').innerText = 'Blocos Merkle Selados: ' + data.blocosSelados;
                        arestasGeral = data.arestasAtivas;
                        desenharCanvasGeral(arestasGeral);

                        const tabela = document.getElementById('tabelaGeralCorpo');
                        tabela.innerHTML = '';
                        data.transacoes.slice().reverse().forEach(tx => {
                            const tr = document.createElement('tr');
                            tr.innerHTML = `
                                <td><strong>#${tx.id}</strong></td>
                                <td>Conta ${tx.origem} -> Conta ${tx.destino}</td>
                                <td>R$ ${(tx.valor / 100).toFixed(2)}</td>
                                <td><strong>${tx.score.toFixed(1)}</strong></td>
                                <td><span class="badge ${tx.status}">${tx.status}</span></td>
                                <td style="color:#cbd5e1; font-family:monospace; font-size:0.75rem;">${tx.motivo}</td>
                            `;
                            tabela.appendChild(tr);
                        });
                    }

                    window.onload = () => {
                        carregarTodasContas();
                        desenharCanvasSmurfing([]);
                        desenharCanvasCiclo([]);
                        desenharCanvasFanIn([]);
                        desenharCanvasBurst([]);
                        desenharCanvasGeral([]);
                    };
                </script>
            </body>
            </html>
            """;

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
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
            Conta conta15 = motor.getTreapContas().obterOuCriar(15, 100_000_00L, 2_000_00L);
            long saldoAnterior = conta15.getSaldoCentavos();

            passoSmurfingAtual++;
            timestampAtual += 15_000L;

            Transacao tx = new Transacao(contadorId++, 15, 40, 9_600_00L, timestampAtual);
            motor.processarTransacao(tx);
            historicoDecisoes.add(tx);
            checarSelagemMerkle();

            MetricasJanela metricas = motor.getJanelaDeslizante().avaliarAtividadeConta(15, 10 * 60 * 1000L, timestampAtual);
            String proximoTxt = (passoSmurfingAtual < 5) ? ("Passo " + (passoSmurfingAtual + 1) + " de 5") : "Smurfing Concluido (Resetar)";

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"passoAtual\":").append(passoSmurfingAtual).append(",");
            json.append("\"proximoPassoTexto\":\"").append(proximoTxt).append("\",");
            json.append("\"saldoAnterior\":").append(saldoAnterior).append(",");
            json.append("\"novoSaldo\":").append(conta15.getSaldoCentavos()).append(",");
            json.append("\"bufferQtd\":").append(metricas.getQtdTransacoes()).append(",");
            json.append("\"bufferValor\":").append(metricas.getValorTotalCentavos()).append(",");
            json.append("\"ultimaTransacao\":{")
                .append("\"id\":").append(tx.getId()).append(",")
                .append("\"origem\":").append(tx.getIdContaOrigem()).append(",")
                .append("\"destino\":").append(tx.getIdContaDestino()).append(",")
                .append("\"valor\":").append(tx.getValorCentavos()).append(",")
                .append("\"score\":").append(String.format(Locale.US, "%.1f", tx.getScoreFraude())).append(",")
                .append("\"status\":\"").append(tx.getStatus().name()).append("\",")
                .append("\"motivo\":\"").append(tx.getMotivoFraude().replace("\"", "'")).append("\"")
                .append("},");
            json.append("\"arestas\":[{\"origem\":15,\"destino\":40,\"status\":\"").append(tx.getStatus().name()).append("\"}]");
            json.append("}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class SmurfingResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            passoSmurfingAtual = 0;
            motor.getJanelaDeslizante().expurgarExpiradas(timestampAtual + 100000000L);
            String json = "{\"status\":\"ok\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
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
            passoCicloAtual++;
            Transacao tx;
            String dfsMsg;
            String proximoTxt;

            Conta c20 = motor.getTreapContas().obterOuCriar(20, 100_000_00L, 2_000_00L);
            Conta c30 = motor.getTreapContas().obterOuCriar(30, 10_000_00L, 2_000_00L);
            Conta c40 = motor.getTreapContas().obterOuCriar(40, 10_000_00L, 2_000_00L);

            if (passoCicloAtual == 1) {
                timestampAtual += 30_000L;
                tx = new Transacao(contadorId++, 20, 30, 50_000_00L, timestampAtual);
                motor.processarTransacao(tx);
                historicoDecisoes.add(tx);
                dfsMsg = "- Chamada: <code>verificarCiclo(origem=20, destino=30, valor=50000)</code><br>"
                       + "- Pilha DFS: <code>DFS(noAtual=30, alvo=20, prof=4)</code> -> Lista de Adjacencia de 30 vazia (Grau Saida = 0).<br>"
                       + "- Resultado: <strong>Nenhum caminho pre-existente</strong>. Aresta 20 -> 30 inserida no Grafo.";
                proximoTxt = "Perna 2: 30 -> 40";
            } else if (passoCicloAtual == 2) {
                timestampAtual += 45_000L;
                tx = new Transacao(contadorId++, 30, 40, 49_000_00L, timestampAtual);
                motor.processarTransacao(tx);
                historicoDecisoes.add(tx);
                dfsMsg = "- Chamada: <code>verificarCiclo(origem=30, destino=40, valor=49000)</code><br>"
                       + "- Pilha DFS: <code>DFS(noAtual=40, alvo=30, prof=4)</code> -> Lista de Adjacencia de 40 vazia.<br>"
                       + "- Resultado: <strong>Nenhum caminho pre-existente</strong>. Aresta 30 -> 40 inserida no Grafo.";
                proximoTxt = "Perna 3: 40 -> 20 (Fechar Ciclo)";
            } else {
                timestampAtual += 40_000L;
                tx = new Transacao(contadorId++, 40, 20, 48_000_00L, timestampAtual);
                motor.processarTransacao(tx);
                historicoDecisoes.add(tx);
                dfsMsg = "- Chamada: <code>verificarCiclo(origem=40, destino=20, valor=48000)</code><br>"
                       + "- Rastreamento da Pilha Recursiva:<br>"
                       + "&nbsp;&nbsp;1. <code>DFS(noAtual=20, alvo=40, prof=4)</code> -> Examina aresta 20 -> 30 (R$ 50k | t1). Causalidade t1 &le; t3 OK.<br>"
                       + "&nbsp;&nbsp;2. <code>DFS(noAtual=30, alvo=40, prof=3)</code> -> Examina aresta 30 -> 40 (R$ 49k | t2). Causalidade t1 &le; t2 &le; t3 OK.<br>"
                       + "&nbsp;&nbsp;3. <code>DFS(noAtual=40, alvo=40)</code> -> <strong>NO ALVO ALCANCADO!</strong> Caminho ativo: [20 -> 30 -> 40].<br>"
                       + "- Preservacao de Capital: |R$ 50k - R$ 48k| / R$ 50k = <strong>4% de variacao</strong> (&le; tolerancia de 20%).<br>"
                       + "- <strong>ANEL GEOMETRICO IDENTIFICADO: [20 -> 30 -> 40 -> 20]</strong>. Transacao <strong>BLOQUEADA</strong>.";
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
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class CicloResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            passoCicloAtual = 0;
            String json = "{\"status\":\"ok\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
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
            int idxOrigem = passoFanInAtual % ORIGENS_FAN_IN.length;
            int origem = ORIGENS_FAN_IN[idxOrigem];
            int destino = 80;

            passoFanInAtual++;
            timestampAtual += 20_000L;

            Transacao tx = new Transacao(contadorId++, origem, destino, 8_000_00L, timestampAtual);
            motor.processarTransacao(tx);
            historicoDecisoes.add(tx);
            checarSelagemMerkle();

            Conta cOrigem = motor.getTreapContas().obterOuCriar(origem, 100_000_00L, 2_000_00L);
            Conta c80 = motor.getTreapContas().obterOuCriar(80, 100_000_00L, 2_000_00L);
            int inDegree = motor.getGrafoTransacional().getGrauEntrada(80);

            StringBuilder nos = new StringBuilder();
            for (int i = 0; i < passoFanInAtual && i < ORIGENS_FAN_IN.length; i++) {
                nos.append("Conta ").append(ORIGENS_FAN_IN[i]);
                if (i < passoFanInAtual - 1 && i < ORIGENS_FAN_IN.length - 1) nos.append(", ");
            }

            String proximoTxt = (passoFanInAtual < 5) 
                ? ("Passo " + (passoFanInAtual + 1) + " de 5: Conta " + ORIGENS_FAN_IN[passoFanInAtual % ORIGENS_FAN_IN.length] + " -> 80") 
                : "Fan-in Concluido (Resetar)";

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"passoAtual\":").append(passoFanInAtual).append(",");
            json.append("\"proximoPassoTexto\":\"").append(proximoTxt).append("\",");
            json.append("\"inDegree\":").append(inDegree).append(",");
            json.append("\"nosConectados\":\"").append(nos.toString()).append("\",");
            json.append("\"saldoOrigem\":").append(cOrigem.getSaldoCentavos()).append(",");
            json.append("\"saldoDestino\":").append(c80.getSaldoCentavos()).append(",");
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
            for (int i = 0; i < passoFanInAtual && i < ORIGENS_FAN_IN.length; i++) {
                int o = ORIGENS_FAN_IN[i];
                String st = (i >= 3) ? "SUSPEITA" : "APROVADA";
                json.append("{\"origem\":").append(o).append(",\"destino\":80,\"status\":\"").append(st).append("\"}");
                if (i < passoFanInAtual - 1 && i < ORIGENS_FAN_IN.length - 1) json.append(",");
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class FanInResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            passoFanInAtual = 0;
            String json = "{\"status\":\"ok\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
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
            int idxDestino = passoBurstAtual % DESTINOS_BURST.length;
            int origem = 25;
            int destino = DESTINOS_BURST[idxDestino];

            passoBurstAtual++;
            timestampAtual += 1_500L;

            long valor = 7_000_00L;
            Transacao tx = new Transacao(contadorId++, origem, destino, valor, timestampAtual);
            motor.processarTransacao(tx);
            historicoDecisoes.add(tx);
            checarSelagemMerkle();

            String proximoTxt = (passoBurstAtual < 5) 
                ? ("Disparo " + (passoBurstAtual + 1) + " de 5") 
                : "Rajada Concluida (Resetar)";

            double tempoTotalSegundos = passoBurstAtual * 1.5;
            int velocidadeEst = (int) ((passoBurstAtual / tempoTotalSegundos) * 60.0);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"passoAtual\":").append(passoBurstAtual).append(",");
            json.append("\"proximoPassoTexto\":\"").append(proximoTxt).append("\",");
            json.append("\"deltaTempo\":1.5,");
            json.append("\"tempoTotalDecorrido\":").append(String.format(Locale.US, "%.1f", tempoTotalSegundos)).append(",");
            json.append("\"velocidadePorMinuto\":").append(velocidadeEst).append(",");
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
            for (int i = 0; i < passoBurstAtual && i < DESTINOS_BURST.length; i++) {
                int d = DESTINOS_BURST[i];
                String st = (i >= 3) ? "SUSPEITA" : "APROVADA";
                json.append("{\"origem\":25,\"destino\":").append(d).append(",\"status\":\"").append(st).append("\"}");
                if (i < passoBurstAtual - 1 && i < DESTINOS_BURST.length - 1) json.append(",");
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class BurstResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            passoBurstAtual = 0;
            String json = "{\"status\":\"ok\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // =========================================================
    // DEMAIS HANDLERS (CONTAS, EXTRATOS, GERAL)
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
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ExtratoContaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            int id = 80;
            if (query != null && query.contains("id=")) {
                try {
                    id = Integer.parseInt(query.split("id=")[1].split("&")[0]);
                } catch (Exception e) {
                    id = 80;
                }
            }

            Conta conta = motor.getTreapContas().obterOuCriar(id, 100_000_00L, 2_000_00L);

            List<Transacao> extratoConta = new ArrayList<>();
            long totalEnviado = 0;
            long totalRecebido = 0;

            for (Transacao tx : historicoDecisoes) {
                if (tx.getIdContaOrigem() == id || tx.getIdContaDestino() == id) {
                    extratoConta.add(tx);
                    if (tx.getIdContaOrigem() == id && tx.getStatus() != StatusTransacao.BLOQUEADA) {
                        totalEnviado += tx.getValorCentavos();
                    }
                    if (tx.getIdContaDestino() == id && tx.getStatus() != StatusTransacao.BLOQUEADA) {
                        totalRecebido += tx.getValorCentavos();
                    }
                }
            }

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"conta\":{")
                .append("\"id\":").append(conta.getIdConta()).append(",")
                .append("\"saldo\":").append(conta.getSaldoCentavos()).append(",")
                .append("\"scoreRisco\":").append(String.format(Locale.US, "%.2f", conta.getScoreRisco()))
                .append("},");
            json.append("\"totalEnviado\":").append(totalEnviado).append(",");
            json.append("\"totalRecebido\":").append(totalRecebido).append(",");
            json.append("\"transacoes\":[");

            for (int i = 0; i < extratoConta.size(); i++) {
                Transacao tx = extratoConta.get(i);
                json.append("{")
                    .append("\"id\":").append(tx.getId()).append(",")
                    .append("\"origem\":").append(tx.getIdContaOrigem()).append(",")
                    .append("\"destino\":").append(tx.getIdContaDestino()).append(",")
                    .append("\"valor\":").append(tx.getValorCentavos()).append(",")
                    .append("\"score\":").append(String.format(Locale.US, "%.1f", tx.getScoreFraude())).append(",")
                    .append("\"status\":\"").append(tx.getStatus().name()).append("\",")
                    .append("\"motivo\":\"").append(tx.getMotivoFraude().replace("\"", "'")).append("\"")
                    .append("}");
                if (i < extratoConta.size() - 1) json.append(",");
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class GeralProcessarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String tipo = (query != null && query.contains("tipo=")) ? query.split("tipo=")[1] : "legitima";

            if ("limpar".equals(tipo)) {
                reiniciarSistema();
            } else if ("legitima".equals(tipo)) {
                int origem = CONTAS_SISTEMA[random.nextInt(CONTAS_SISTEMA.length)];
                int destino;
                do {
                    destino = CONTAS_SISTEMA[random.nextInt(CONTAS_SISTEMA.length)];
                } while (destino == origem);

                long valorCentavos = (random.nextInt(900) + 50) * 100L;
                timestampAtual += (random.nextInt(4) + 1) * 1000L;

                Transacao tx = new Transacao(contadorId++, origem, destino, valorCentavos, timestampAtual);
                motor.processarTransacao(tx);
                historicoDecisoes.add(tx);
            } else if ("avancar_tempo".equals(tipo)) {
                timestampAtual += (15 * 60 * 1000L);
                Transacao txAvanco = new Transacao(contadorId++, 10, 80, 100_00L, timestampAtual);
                motor.processarTransacao(txAvanco);
                historicoDecisoes.add(txAvanco);
            }

            checarSelagemMerkle();

            long limiteJanela = timestampAtual - (10 * 60 * 1000L);
            List<Transacao> ativasNaJanela = new ArrayList<>();
            for (Transacao t : historicoDecisoes) {
                if (t.getTimestamp() >= limiteJanela && t.getStatus() != StatusTransacao.BLOQUEADA) {
                    ativasNaJanela.add(t);
                }
            }

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"blocosSelados\":").append(totalBlocosSelados).append(",");
            json.append("\"arestasAtivas\":[");
            for (int i = 0; i < ativasNaJanela.size(); i++) {
                Transacao t = ativasNaJanela.get(i);
                json.append("{")
                    .append("\"origem\":").append(t.getIdContaOrigem()).append(",")
                    .append("\"destino\":").append(t.getIdContaDestino()).append(",")
                    .append("\"status\":\"").append(t.getStatus().name()).append("\"")
                    .append("}");
                if (i < ativasNaJanela.size() - 1) json.append(",");
            }
            json.append("],");

            json.append("\"transacoes\":[");
            for (int i = 0; i < historicoDecisoes.size(); i++) {
                Transacao t = historicoDecisoes.get(i);
                json.append("{")
                    .append("\"id\":").append(t.getId()).append(",")
                    .append("\"origem\":").append(t.getIdContaOrigem()).append(",")
                    .append("\"destino\":").append(t.getIdContaDestino()).append(",")
                    .append("\"valor\":").append(t.getValorCentavos()).append(",")
                    .append("\"score\":").append(String.format(Locale.US, "%.1f", t.getScoreFraude())).append(",")
                    .append("\"status\":\"").append(t.getStatus().name()).append("\",")
                    .append("\"motivo\":\"").append(t.getMotivoFraude().replace("\"", "'")).append("\"")
                    .append("}");
                if (i < historicoDecisoes.size() - 1) json.append(",");
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}