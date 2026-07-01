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
        // Se um novo pedido já foi iniciado enquanto as reconciliações rodavam,
        // não apaga os dados dele.
        const idAtual = parseInt(sessionStorage.getItem("pedidoIdAtual"));
        if (idAtual && pedidoId && idAtual !== pedidoId) return;

        sessionStorage.removeItem("pedidoEmCurso");
        sessionStorage.removeItem("pedidoIdAtual");
        sessionStorage.removeItem("posicaoExpedicaoAtual");
        sessionStorage.removeItem("expedicaoSnapshotAntesPedido");
        window.posicaoExpedicaoFinalizadaDetectada = null;
    }

    function corrigirDuplicidadeExpedicao(pedidoId, posicaoExpedicao, ipExpedicao) {
        return fetch("/expedicao/corrigir-duplicidade", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                pedidoId: pedidoId,
                posicaoCorreta: posicaoExpedicao,
                ipClp: ipExpedicao
            })
        })
            .then(async response => {
                const texto = await response.text();
                if (!response.ok) {
                    console.error("Erro ao reconciliar expedição:", texto);
                    return false;
                }
                console.log(texto || "Expedição reconciliada.");
                return true;
            })
            .catch(err => {
                console.error("Erro na reconciliação da expedição:", err);
                return false;
            });
    }

    function agendarReconciliacoesExpedicao(pedidoId, posicaoExpedicao, ipExpedicao) {
        const delays = [900, 2500, 4500, 8000, 15000, 30000];
        let concluidas = 0;

        delays.forEach(delay => {
            setTimeout(() => {
                corrigirDuplicidadeExpedicao(pedidoId, posicaoExpedicao, ipExpedicao)
                    .finally(() => {
                        concluidas++;
                        if (concluidas === delays.length) {
                            limparPedidoAtualFinalizado(pedidoId);
                            atualizarVisualExpedicaoDepois();
                        }
                    });
            }, delay);
        });
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
                agendarReconciliacoesExpedicao(pedidoId, posicaoExpedicao, ipExpedicao || "");
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

            const pedidoId = obterPedidoIdSeguro();
            if (!pedidoId) return;

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
                opGuardada === pedidoId ||
                (numeroOpExp === pedidoId && ((recebidoOpExp && finishOpExp) || adicionarExpedicao));

            if (finalizou) {
                window.posicaoExpedicaoFinalizadaDetectada = posicaoSelecionada;

                console.log("Finalização detectada pelo CLP4", {
                    pedidoId,
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

    // Este arquivo carrega depois de smart.js, então a instalação é direta:
    window.finalizarPedido = finalizarPedidoCorrigido;

    const processarOriginal = window.processarDadosClp;
    window.processarDadosClp = function (clp, data) {
        processarOriginal.apply(this, arguments);
        if (clp === "clp4") {
            detectarFinalizacaoPeloClp4(data);
        }
    };

    setInterval(tentarFinalizarPedido, 700);
})();
