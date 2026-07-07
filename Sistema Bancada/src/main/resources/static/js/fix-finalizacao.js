// Correção complementar de finalização do pedido.
// A expedição só deve ser gravada no banco quando a produção terminar de verdade.
// A tabela de posições do CLP não é usada como fonte de verdade, porque ela pode ser zerada
// ou sobrescrita pelo próprio programa do CLP durante o scan.

(function () {
    function bytesHexParaArray(data) {
        if (!data || typeof data !== "string") return [];
        return data.trim().split(/\s+/).map(hex => parseInt(hex, 16));
    }

    function word(byteArray, offset) {
        return ((byteArray[offset] || 0) << 8) | (byteArray[offset + 1] || 0);
    }

    function pedidoEstaEmCursoNaTela() {
        const btn = document.getElementById("btnExecutarPedidoProducao");
        const textoBotao = btn ? btn.textContent.trim().toLowerCase() : "";

        return textoBotao === "pedido em curso" ||
            (typeof pedidoEmCurso !== "undefined" && !!pedidoEmCurso) ||
            sessionStorage.getItem("pedidoEmCurso") === "true";
    }

    function podeFinalizarPorStatus() {
        try {
            const producaoFinalizada = (typeof statusProducao !== "undefined" && Number(statusProducao) === 1);
            const todasEstacoesConcluidas =
                typeof statusEstoque !== "undefined" &&
                typeof statusProcesso !== "undefined" &&
                typeof statusMontagem !== "undefined" &&
                typeof statusExpedicao !== "undefined" &&
                Number(statusEstoque) === 2 &&
                Number(statusProcesso) === 2 &&
                Number(statusMontagem) === 2 &&
                Number(statusExpedicao) === 2;

            return pedidoEstaEmCursoNaTela() && (producaoFinalizada || todasEstacoesConcluidas);
        } catch (e) {
            return false;
        }
    }

    function obterPedidoIdSeguro() {
        try {
            if (typeof obterPedidoIdAtual === "function") {
                const id = obterPedidoIdAtual();
                if (id) return id;
            }
        } catch (e) {
            console.warn("Não foi possível obter ID pelo helper:", e);
        }

        const idSession = parseInt(sessionStorage.getItem("pedidoIdAtual"));
        return idSession || 0;
    }

    // Número de OP realmente gravado no CLP ao iniciar o pedido (número informado
    // pelo operador ou o id automático — ver X-Numero-Op em /iniciar-pedido). É
    // ESTE número que aparece nos bytes do CLP4, não necessariamente o id do banco,
    // então a detecção de finalização precisa comparar com ele.
    function obterNumeroOpSeguro() {
        const numeroOp = parseInt(sessionStorage.getItem("opNumeroAtual"));
        if (numeroOp) return numeroOp;

        // Compatibilidade com pedidos iniciados antes desta correção (sem o número
        // de OP salvo na sessão): cai de volta para o id do pedido.
        return obterPedidoIdSeguro();
    }

    function obterPosicaoExpedicaoSelecionada() {
        const posSession = parseInt(sessionStorage.getItem("posicaoExpedicaoAtual"));
        if (posSession >= 1 && posSession <= 12) return posSession;

        const el = document.getElementById("posExpedicao");
        if (el) {
            const posTela = parseInt(el.value) || 0;
            if (posTela >= 1 && posTela <= 12) return posTela;
        }

        // Importante: não usa mais posição lida do CLP como fallback.
        // O CLP pode manter posição antiga, principalmente a posição 1, e isso fazia
        // a OP nova ser salva na posição errada quando a requisição era repetida.
        return 0;
    }

    function atualizarVisualExpedicaoDepois() {
        setTimeout(() => {
            try {
                if (typeof carregarValoresExpedicao === "function") {
                    carregarValoresExpedicao();
                }
            } catch (e) {
                console.warn("Não foi possível atualizar a expedição após finalizar:", e);
            }
        }, 800);
    }

    function limparPedidoAtualFinalizado(pedidoId) {
        const idAtual = parseInt(sessionStorage.getItem("pedidoIdAtual"));
        if (idAtual && pedidoId && idAtual !== pedidoId) return;

        sessionStorage.removeItem("pedidoEmCurso");
        sessionStorage.removeItem("pedidoIdAtual");
        sessionStorage.removeItem("posicaoExpedicaoAtual");
        sessionStorage.removeItem("expedicaoSnapshotAntesPedido");
        sessionStorage.removeItem("opNumeroAtual");
        window.posicaoExpedicaoFinalizadaDetectada = null;
    }

    function finalizarLimpezaLocal(pedidoId) {
        // Não chama mais /expedicao/corrigir-duplicidade, porque o controller foi removido.
        // A finalização oficial fica concentrada no /finalizar-pedido-producao.
        limparPedidoAtualFinalizado(pedidoId);
        atualizarVisualExpedicaoDepois();
    }

    function finalizarPedidoCorrigido() {
        if (typeof pedidoFinalizado !== "undefined" && pedidoFinalizado) return;

        const pedidoId = obterPedidoIdSeguro();
        const ipExpedicao = document.getElementById("hostIpExpedicao")?.value;
        const posicaoExpedicao = obterPosicaoExpedicaoSelecionada();

        if (!pedidoId) {
            console.warn("finalizarPedido: ID do pedido não encontrado para finalizar.");
            return;
        }

        if (!posicaoExpedicao || posicaoExpedicao < 1 || posicaoExpedicao > 12) {
            console.warn("finalizarPedido: posição de expedição inválida. Finalização não será feita por fallback do CLP.");
            return;
        }

        if (typeof pedidoFinalizado !== "undefined") pedidoFinalizado = true;
        if (typeof pedidoConcluido !== "undefined") pedidoConcluido = true;
        if (typeof pedidoEmCurso !== "undefined") pedidoEmCurso = false;

        if (typeof pararContador === "function") {
            pararContador();
        }

        const btn = document.getElementById("btnExecutarPedidoProducao");
        if (btn) btn.textContent = "Pedido concluido";

        fetch("/finalizar-pedido-producao", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                id: pedidoId,
                statusOrderProduction: "concluido",
                ipClp: ipExpedicao || "",
                posicaoExpedicao: posicaoExpedicao
            })
        })
            .then(async response => {
                const texto = await response.text();
                if (!response.ok) {
                    console.error("Erro ao finalizar pedido:", texto);
                    alert(texto || "Erro ao finalizar pedido.");
                    if (typeof pedidoFinalizado !== "undefined") pedidoFinalizado = false;
                    return;
                }

                console.log(texto || "Pedido finalizado e expedição gravada no banco.");
                finalizarLimpezaLocal(pedidoId);
            })
            .catch(err => {
                console.error("Erro ao finalizar pedido:", err);
                alert("Erro na comunicação ao finalizar pedido.");
                if (typeof pedidoFinalizado !== "undefined") pedidoFinalizado = false;
            });
    }

    function detectarFinalizacaoPeloClp4(data) {
        try {
            if (!pedidoEstaEmCursoNaTela()) return;
            if (typeof pedidoFinalizado !== "undefined" && pedidoFinalizado) return;

            const numeroOp = obterNumeroOpSeguro();
            if (!numeroOp) return;

            const posicaoSelecionada = obterPosicaoExpedicaoSelecionada();
            if (posicaoSelecionada < 1 || posicaoSelecionada > 12) return;

            const byteArray = bytesHexParaArray(data);
            if (byteArray.length < 46) return;

            const recebidoOpExp = (byteArray[0] & 0b00000001) !== 0;
            const finishOpExp = (byteArray[32] & 0b00000010) !== 0;
            const adicionarExpedicao = (byteArray[42] & 0b00000001) !== 0;
            const ocupadoExp = (byteArray[34] & 0b00000001) !== 0;

            const numeroOpExp = word(byteArray, 30);
            const posicaoGuardada = word(byteArray, 38);
            const opGuardada = word(byteArray, 44);

            // A tabela de posições do CLP não entra mais na decisão, pois ela é instável.
            // Finaliza somente quando o CLP confirma a OP em curso por campos de OP/flags.
            const finalizou =
                opGuardada === numeroOp ||
                (numeroOpExp === numeroOp && ((recebidoOpExp && finishOpExp) || adicionarExpedicao));

            if (finalizou) {
                window.posicaoExpedicaoFinalizadaDetectada = posicaoSelecionada;

                console.log("Finalização detectada pelo CLP4", {
                    numeroOp,
                    numeroOpExp,
                    posicaoSelecionada,
                    posicaoGuardada,
                    opGuardada,
                    recebidoOpExp,
                    finishOpExp,
                    adicionarExpedicao,
                    ocupadoExp
                });

                finalizarPedidoCorrigido();
            }
        } catch (e) {
            console.warn("Falha ao analisar finalização pelo CLP4:", e);
        }
    }

    function tentarFinalizarPedido() {
        try {
            if (typeof pedidoFinalizado !== "undefined" && pedidoFinalizado) return;

            if (podeFinalizarPorStatus()) {
                console.log("Finalização detectada pelo status consolidado da produção.");
                finalizarPedidoCorrigido();
            }
        } catch (e) {
            console.warn("Não foi possível verificar a finalização do pedido:", e);
        }
    }

    window.finalizarPedido = finalizarPedidoCorrigido;

    // Encerra o ciclo na tela (para o contador, atualiza o botão e limpa o
    // estado local) quando a conclusão JÁ foi gravada — usado quando o backend
    // finaliza sozinho e o navegador não chegou a detectar pelo CLP. Não chama
    // /finalizar-pedido-producao de novo, pois o banco já está finalizado.
    function aplicarFinalizacaoLocal(pedidoId) {
        if (typeof pedidoFinalizado !== "undefined" && pedidoFinalizado) return;

        if (typeof pedidoFinalizado !== "undefined") pedidoFinalizado = true;
        if (typeof pedidoConcluido !== "undefined") pedidoConcluido = true;
        if (typeof pedidoEmCurso !== "undefined") pedidoEmCurso = false;

        if (typeof pararContador === "function") pararContador();

        const btn = document.getElementById("btnExecutarPedidoProducao");
        if (btn) btn.textContent = "Pedido concluido";

        finalizarLimpezaLocal(pedidoId);
    }

    // Sincroniza a tela com o banco: se o pedido em curso já consta como
    // "concluido" (o backend finaliza sozinho ao detectar o fim), encerra o
    // ciclo na tela mesmo que o sinal do CLP não tenha sido captado ao vivo.
    async function verificarFinalizacaoNoBanco() {
        try {
            if (typeof pedidoFinalizado !== "undefined" && pedidoFinalizado) return;
            if (sessionStorage.getItem("pedidoEmCurso") !== "true") return;

            const pedidoId = obterPedidoIdSeguro();
            if (!pedidoId) return;

            const res = await fetch(`/listar-pedido/${pedidoId}`, { cache: "no-store" });
            if (!res.ok) return;

            const pedido = await res.json();
            const status = (pedido.statusOrderProduction || "").toString().toLowerCase();
            if (status === "concluido" || status === "concluído") {
                console.log("Finalização confirmada pelo banco para o pedido", pedidoId);
                aplicarFinalizacaoLocal(pedidoId);
            }
        } catch (e) {
            // Silencioso: é apenas uma verificação de apoio.
        }
    }

    const processarOriginal = window.processarDadosClp;
    window.processarDadosClp = function (clp, data) {
        processarOriginal.apply(this, arguments);
        if (clp === "clp4") {
            detectarFinalizacaoPeloClp4(data);
        }
    };

    setInterval(tentarFinalizarPedido, 700);
    setInterval(verificarFinalizacaoNoBanco, 2500);
})();
