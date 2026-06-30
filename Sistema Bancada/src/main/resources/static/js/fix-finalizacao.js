// Correção complementar de finalização do pedido.
// Este arquivo fica separado para não mexer no smart.js original inteiro.
// Ele observa o status consolidado da produção e garante que a finalização rode uma única vez.

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

    // Também reforça a atualização visual da expedição depois que finalizarPedido() gravar no CLP.
    function instalarWrapperFinalizacao() {
        if (typeof finalizarPedido !== "function") return false;
        if (finalizarPedido.__fixFinalizacaoInstalado) return true;

        const finalizarOriginal = finalizarPedido;
        finalizarPedido = function () {
            finalizarOriginal.apply(this, arguments);

            setTimeout(() => {
                try {
                    if (typeof carregarValoresExpedicao === "function") {
                        carregarValoresExpedicao();
                    }
                } catch (e) {
                    console.warn("Não foi possível atualizar a expedição após finalizar:", e);
                }
            }, 800);
        };
        finalizarPedido.__fixFinalizacaoInstalado = true;
        return true;
    }

    const setupInterval = setInterval(() => {
        if (instalarWrapperFinalizacao()) {
            clearInterval(setupInterval);
        }
    }, 300);

    setInterval(tentarFinalizarPedido, 700);
})();
