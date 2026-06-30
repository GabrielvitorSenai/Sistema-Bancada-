// Correção complementar de finalização do pedido.
// A expedição só deve ser gravada no CLP quando a produção terminar.

(function () {
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

            return producaoFinalizada || todasEstacoesConcluidas;
        } catch (e) {
            return false;
        }
    }

    function obterPedidoIdAtualSeguro() {
        try {
            if (typeof obterPedidoIdAtual === "function") {
                const id = obterPedidoIdAtual();
                if (id) return id;
            }
        } catch (e) {
            console.warn("Falha ao usar obterPedidoIdAtual:", e);
        }

        const el = document.getElementById("pedido-id");
        if (!el) return null;

        const match = el.innerText.match(/#(\d+)/);
        return match ? parseInt(match[1]) : null;
    }

    function obterPosicaoExpedicaoSelecionada() {
        const el = document.getElementById("posExpedicao");
        if (!el) return 0;
        return parseInt(el.value) || 0;
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

    function instalarFinalizacaoCorrigida() {
        if (typeof finalizarPedido !== "function") return false;
        if (finalizarPedido.__fixFinalizacaoInstalado) return true;

        finalizarPedido = function () {
            if (typeof pedidoFinalizado !== "undefined" && pedidoFinalizado) return;
            pedidoFinalizado = true;

            if (typeof pedidoConcluido !== "undefined") pedidoConcluido = true;
            if (typeof pedidoEmCurso !== "undefined") pedidoEmCurso = false;

            if (typeof pararContador === "function") {
                pararContador();
            }

            const btn = document.getElementById("btnExecutarPedidoProducao");
            if (btn) btn.textContent = "Pedido concluido";

            const pedidoId = obterPedidoIdAtualSeguro();
            const ipExpedicao = document.getElementById("hostIpExpedicao")?.value;
            const posicaoExpedicao = obterPosicaoExpedicaoSelecionada();

            if (!pedidoId) {
                console.warn("finalizarPedido: ID do pedido não encontrado para finalizar.");
                return;
            }

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
                        return;
                    }
                    console.log(texto || "Pedido finalizado e expedição gravada no CLP.");
                    atualizarVisualExpedicaoDepois();
                })
                .catch(err => {
                    console.error("Erro ao finalizar pedido:", err);
                    alert("Erro na comunicação ao finalizar pedido.");
                });
        };

        finalizarPedido.__fixFinalizacaoInstalado = true;
        return true;
    }

    function tentarFinalizarPedido() {
        try {
            if (typeof finalizarPedido !== "function") return;
            if (typeof pedidoFinalizado !== "undefined" && pedidoFinalizado) return;

            if (podeFinalizarPorStatus()) {
                console.log("Finalização detectada pelo status consolidado da produção.");
                finalizarPedido();
            }
        } catch (e) {
            console.warn("Não foi possível verificar a finalização do pedido:", e);
        }
    }

    const setupInterval = setInterval(() => {
        if (instalarFinalizacaoCorrigida()) {
            clearInterval(setupInterval);
        }
    }, 300);

    setInterval(tentarFinalizarPedido, 700);
})();
