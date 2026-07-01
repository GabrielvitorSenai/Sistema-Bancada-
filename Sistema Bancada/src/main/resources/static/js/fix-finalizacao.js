// Correção complementar de finalização do pedido.
// A expedição só deve ser gravada no banco/CLP quando a produção terminar de verdade.
// Este arquivo também observa os bytes do CLP de Expedição para concluir automaticamente.

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

    function obterPosicaoExpedicaoSelecionada() {
        const posSession = parseInt(sessionStorage.getItem("posicaoExpedicaoAtual"));
        if (posSession) return posSession;

        const el = document.getElementById("posExpedicao");
        if (el) {
            const posTela = parseInt(el.value) || 0;
            if (posTela) return posTela;
        }

        if (window.posicaoExpedicaoFinalizadaDetectada) {
            return parseInt(window.posicaoExpedicaoFinalizadaDetectada) || 0;
        }

        return 0;
    }

    function obterSnapshotExpedicao() {
        try {
            const salvo = sessionStorage.getItem("expedicaoSnapshotAntesPedido");
            return salvo ? JSON.parse(salvo) : {};
        } catch (e) {
            console.warn("Snapshot da expedição inválido:", e);
            return {};
        }
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

    function limparPedidoAtualFinalizado() {
        sessionStorage.removeItem("pedidoEmCurso");
        sessionStorage.removeItem("pedidoIdAtual");
        sessionStorage.removeItem("posicaoExpedicaoAtual");
        sessionStorage.removeItem("expedicaoSnapshotAntesPedido");
        window.posicaoExpedicaoFinalizadaDetectada = null;
    }

    function corrigirDuplicidadeExpedicao(pedidoId, posicaoExpedicao, ipExpedicao) {
        const snapshot = obterSnapshotExpedicao();

        return fetch("/expedicao/corrigir-duplicidade", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                pedidoId: pedidoId,
                posicaoCorreta: posicaoExpedicao,
                ipClp: ipExpedicao,
                snapshot: snapshot
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
        const delays = [900, 2500, 4500];
        let concluidas = 0;

        delays.forEach(delay => {
            setTimeout(() => {
                corrigirDuplicidadeExpedicao(pedidoId, posicaoExpedicao, ipExpedicao)
                    .finally(() => {
                        concluidas++;
                        if (concluidas === delays.length) {
                            limparPedidoAtualFinalizado();
                            atualizarVisualExpedicaoDepois();
                        }
                    });
            }, delay);
        });
    }

    function finalizarPedidoCorrigido() {
        if (typeof pedidoFinalizado !== "undefined" && pedidoFinalizado) return;

        const pedidoId = obterPedidoIdAtual();
        const ipExpedicao = document.getElementById("hostIpExpedicao")?.value;
        const posicaoExpedicao = obterPosicaoExpedicaoSelecionada();

        if (!pedidoId) {
            console.warn("finalizarPedido: ID do pedido não encontrado para finalizar.");
            return;
        }

        if (!ipExpedicao) {
            console.warn("finalizarPedido: IP do CLP de Expedição não informado.");
            return;
        }

        if (!posicaoExpedicao || posicaoExpedicao < 1 || posicaoExpedicao > 12) {
            console.warn("finalizarPedido: posição de expedição inválida:", posicaoExpedicao);
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
                ipClp: ipExpedicao,
                posicaoExpedicao: posicaoExpedicao
            })
        })
            .then(async response => {
                const texto = await response.text();
                if (!response.ok) {
                    console.error("Erro ao finalizar pedido:", texto);
                    alert(texto || "Erro ao finalizar pedido e gravar expedição no CLP.");
                    if (typeof pedidoFinalizado !== "undefined") pedidoFinalizado = false;
                    return;
                }

                console.log(texto || "Pedido finalizado e expedição gravada no CLP.");

                // A rotina antiga do service ainda pode reagir aos bits do CLP por alguns segundos.
                // Por isso reconciliamos mais de uma vez, mantendo apenas a posição selecionada com o pedido novo.
                agendarReconciliacoesExpedicao(pedidoId, posicaoExpedicao, ipExpedicao);
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

            const pedidoId = obterPedidoIdAtual();
            if (!pedidoId) return;

            const byteArray = bytesHexParaArray(data);
            if (byteArray.length < 46) return;

            const recebidoOpExp = (byteArray[0] & 0b00000001) !== 0;
            const finishOpExp = (byteArray[32] & 0b00000010) !== 0;
            const adicionarExpedicao = (byteArray[42] & 0b00000001) !== 0;
            const ocupadoExp = (byteArray[34] & 0b00000001) !== 0;

            const posicaoGuardada = word(byteArray, 38);
            const opGuardada = word(byteArray, 44);
            const posicaoSelecionada = obterPosicaoExpedicaoSelecionada();
            const valorNaPosicaoSelecionada = posicaoSelecionada >= 1 && posicaoSelecionada <= 12
                ? word(byteArray, 6 + ((posicaoSelecionada - 1) * 2))
                : 0;

            const clpConfirmouPedido =
                opGuardada === pedidoId ||
                valorNaPosicaoSelecionada === pedidoId ||
                (posicaoGuardada >= 1 && posicaoGuardada <= 12 && finishOpExp && !ocupadoExp) ||
                (posicaoGuardada >= 1 && posicaoGuardada <= 12 && adicionarExpedicao);

            const finalizou =
                (recebidoOpExp && finishOpExp) ||
                adicionarExpedicao ||
                clpConfirmouPedido;

            if (finalizou) {
                // A regra do sistema é respeitar a posição selecionada na execução.
                // A posição lida do CLP só é usada como fallback quando não houver posição selecionada válida.
                if (posicaoSelecionada >= 1 && posicaoSelecionada <= 12) {
                    window.posicaoExpedicaoFinalizadaDetectada = posicaoSelecionada;
                } else if (posicaoGuardada >= 1 && posicaoGuardada <= 12) {
                    window.posicaoExpedicaoFinalizadaDetectada = posicaoGuardada;
                }

                console.log("Finalização detectada pelo CLP4", {
                    pedidoId,
                    posicaoSelecionada,
                    posicaoGuardada,
                    opGuardada,
                    valorNaPosicaoSelecionada,
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
    // finalizarPedido passa a ser a versão corrigida e o processamento do CLP4
    // ganha o observador de finalização.
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
