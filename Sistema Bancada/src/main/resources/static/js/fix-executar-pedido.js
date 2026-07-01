// Correção da execução do pedido pela tela Linha.
// O contador só deve iniciar depois que existir um pedido válido e o backend aceitar iniciar a produção.

(function () {
    async function capturarSnapshotExpedicao() {
        const snapshot = {};

        // Primeiro tenta pegar do banco, que é a fonte mais confiável do que já estava salvo.
        try {
            const response = await fetch("/pedidos-expedicao", { cache: "no-store" });
            if (response.ok) {
                const dados = await response.json();
                for (let i = 1; i <= 12; i++) {
                    snapshot[String(i)] = parseInt(dados[`P${i}`] || 0) || 0;
                }
                sessionStorage.setItem("expedicaoSnapshotAntesPedido", JSON.stringify(snapshot));
                return snapshot;
            }
        } catch (e) {
            console.warn("Não foi possível capturar snapshot da expedição pelo banco; usando tela.", e);
        }

        // Fallback: usa a tela caso o endpoint falhe.
        for (let i = 1; i <= 12; i++) {
            const celula = document.getElementById(`expedicao-${i}`);
            const valor = parseInt(celula?.textContent || "0") || 0;
            snapshot[String(i)] = valor;
        }

        sessionStorage.setItem("expedicaoSnapshotAntesPedido", JSON.stringify(snapshot));
        return snapshot;
    }

    async function executarPedidoCorrigido() {
        const tipo = document.getElementById("tipoPedido")?.value;
        const tampa = document.getElementById("corTampa")?.value;
        const ipClp = document.getElementById("hostIpEstoque")?.value;

        if (!ipClp) {
            alert("Por favor, informe o IP do CLP.");
            return false;
        }

        const pedidoId = obterPedidoIdAtual();
        if (!pedidoId) {
            alert("Nenhum pedido foi carregado para execução. Crie ou selecione um pedido antes de iniciar.");
            return false;
        }

        window.pedidoIdAtual = pedidoId;
        sessionStorage.setItem("pedidoIdAtual", String(pedidoId));

        if (typeof validarEstoqueBlocos === "function") {
            const estoqueOk = await validarEstoqueBlocos();
            if (!estoqueOk) {
                const aviso = document.getElementById("avisoEstoque");
                alert(aviso && aviso.textContent ? aviso.textContent : "Estoque insuficiente no Gestão para a(s) cor(es) escolhida(s).");
                sessionStorage.removeItem("pedidoIdAtual");
                return false;
            }
        }

        const blocosCount = tipo === "simples" ? 1 : tipo === "duplo" ? 2 : 3;
        const blocos = [];

        for (let b = 1; b <= blocosCount; b++) {
            const corBlocoInput = document.getElementById(`block-color-${b}`);
            let corBloco = 3;

            if (corBlocoInput?.value === "preto") corBloco = 1;
            else if (corBlocoInput?.value === "vermelho") corBloco = 2;

            const bloco = {
                andar: b,
                corBloco: corBloco,
                laminas: []
            };

            for (let l = 1; l <= 3; l++) {
                const cor = parseInt(document.getElementById(`l${l}-color-${b}`)?.value) || 0;
                const padrao = parseInt(document.getElementById(`l${l}-pattern-${b}`)?.value) || 0;
                bloco.laminas.push({ numero: l, cor, padrao });
            }

            blocos.push(bloco);
        }

        let posicaoExpedicao = parseInt(document.getElementById("posExpedicao")?.value) || 0;

        // Posição "Automática": resolve aqui a primeira posição livre, para que
        // frontend e backend trabalhem com a MESMA posição durante todo o pedido.
        if (posicaoExpedicao < 1 || posicaoExpedicao > 12) {
            try {
                const res = await fetch("/expedicao/primeira-livre", { cache: "no-store" });
                if (res.ok) {
                    posicaoExpedicao = parseInt(await res.text()) || 0;
                }
            } catch (e) {
                console.error("Erro ao buscar primeira posição livre da expedição:", e);
            }

            if (posicaoExpedicao < 1 || posicaoExpedicao > 12) {
                alert("Não há posição livre na expedição para guardar o pedido.");
                sessionStorage.removeItem("pedidoIdAtual");
                return false;
            }
        }

        sessionStorage.setItem("posicaoExpedicaoAtual", String(posicaoExpedicao));

        // Mantém a foto da expedição apenas como referência local/debug.
        // Não chama mais /expedicao/preparar-posicao, porque o controller foi removido.
        await capturarSnapshotExpedicao();

        const pedidoDTO = {
            id: pedidoId,
            tipo: tipo,
            tampa: tampa,
            ipClp: ipClp,
            posicaoExpedicao: posicaoExpedicao,
            blocos: blocos
        };

        try {
            const response = await fetch("/iniciar-pedido", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(pedidoDTO)
            });

            const msg = await response.text();

            if (!response.ok) {
                alert(msg || "Erro ao executar pedido.");
                sessionStorage.removeItem("pedidoIdAtual");
                sessionStorage.removeItem("posicaoExpedicaoAtual");
                sessionStorage.removeItem("pedidoEmCurso");
                sessionStorage.removeItem("expedicaoSnapshotAntesPedido");
                return false;
            }

            sessionStorage.setItem("pedidoEmCurso", "true");
            alert("Pedido executado com sucesso!");
            return true;
        } catch (error) {
            console.error("Erro na requisição:", error);
            alert("Erro na comunicação com o servidor.");
            sessionStorage.removeItem("pedidoIdAtual");
            sessionStorage.removeItem("posicaoExpedicaoAtual");
            sessionStorage.removeItem("pedidoEmCurso");
            sessionStorage.removeItem("expedicaoSnapshotAntesPedido");
            return false;
        }
    }

    function resetarTelaParaNovoPedido() {
        const btn = document.getElementById("btnExecutarPedidoProducao");
        if (btn) btn.textContent = "Executar Pedido";

        if (typeof statusEstoque !== "undefined") statusEstoque = 0;
        if (typeof statusProcesso !== "undefined") statusProcesso = 0;
        if (typeof statusMontagem !== "undefined") statusMontagem = 0;
        if (typeof statusExpedicao !== "undefined") statusExpedicao = 0;
        if (typeof statusProducao !== "undefined") statusProducao = 0;
        if (typeof pedidoEmCurso !== "undefined") pedidoEmCurso = false;
        if (typeof pedidoFinalizado !== "undefined") pedidoFinalizado = false;
        if (typeof pedidoConcluido !== "undefined") pedidoConcluido = false;

        sessionStorage.removeItem("pedidoEmCurso");
        sessionStorage.removeItem("pedidoIdAtual");
        sessionStorage.removeItem("posicaoExpedicaoAtual");
        sessionStorage.removeItem("expedicaoSnapshotAntesPedido");

        fetch("/smart/reset-status", { method: "POST" })
            .then(response => {
                if (!response.ok) throw new Error("Erro ao resetar status no backend");
                return response.text();
            })
            .then(data => console.log("Backend:", data))
            .catch(error => console.error("Erro na chamada ao backend:", error));

        const leituraAtiva = (typeof modoLeitura !== "undefined" && modoLeitura);
        if (!leituraAtiva) {
            if (typeof carregarValoresEstoque === "function") carregarValoresEstoque();
            if (typeof carregarValoresExpedicao === "function") carregarValoresExpedicao();
        }
    }

    window.executarPedido = executarPedidoCorrigido;

    window.fases = async function (fase) {
        if (fase == 1) {
            const btn = document.getElementById("btnExecutarPedidoProducao");
            if (!btn) return;

            if (btn.textContent === "Executar Pedido") {
                const leituraAtiva = (typeof modoLeitura !== "undefined" && modoLeitura);
                if (leituraAtiva) return;

                const pedidoIniciadoComSucesso = await executarPedidoCorrigido();

                if (!pedidoIniciadoComSucesso) {
                    // Garante que o contador não fique rodando quando não existe pedido válido.
                    if (typeof pararContador === "function") pararContador();
                    btn.textContent = "Executar Pedido";
                    return;
                }

                if (typeof pedidoConcluido !== "undefined") pedidoConcluido = false;
                if (typeof pedidoFinalizado !== "undefined") pedidoFinalizado = false;
                if (typeof pedidoEmCurso !== "undefined") pedidoEmCurso = true;

                if (typeof iniciarContador === "function") iniciarContador();
                btn.textContent = "Pedido em curso";
            } else if (btn.textContent === "Pedido concluido") {
                resetarTelaParaNovoPedido();
            }
        } else if (fase == 2) {
            if (typeof pararContador === "function") pararContador();
        } else if (fase == 3) {
            if (typeof statusMontagem !== "undefined") {
                statusMontagem++;
                if (statusMontagem > 2) statusMontagem = 0;
            }
        } else if (fase == 4) {
            if (typeof statusExpedicao !== "undefined") {
                statusExpedicao++;
                if (statusExpedicao > 2) statusExpedicao = 0;
            }
        }
    };
})();
