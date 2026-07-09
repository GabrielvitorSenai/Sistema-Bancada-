<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { usePedidosStore } from '@/stores/pedidos.store'
import type { Pedido } from '@/types/pedido'
import PedidoTabela from '@/components/pedidos/PedidoTabela.vue'

const pedidosStore = usePedidosStore()
const filtro = ref('todos')
const ipClp = ref('10.74.241.10')
const mensagem = ref('')
const erro = ref('')

const pedidosFiltrados = computed(() => {
  if (filtro.value === 'todos') return pedidosStore.pedidos
  return pedidosStore.pedidos.filter(p => p.statusOrderProduction === filtro.value)
})

onMounted(() => pedidosStore.carregar())

async function executar(pedido: Pedido) {
  mensagem.value = ''
  erro.value = ''
  try {
    const resultado = await pedidosStore.executar({
      ...pedido,
      ipClp: ipClp.value,
      posicaoExpedicao: pedido.posicaoExpedicao || 0,
    })
    mensagem.value = `${resultado.mensagem} OP: ${resultado.numeroOp || '-'}`
  } catch (error: any) {
    erro.value = error.message || 'Erro ao executar pedido.'
  }
}

async function excluir(pedido: Pedido) {
  mensagem.value = ''
  erro.value = ''
  try {
    await pedidosStore.remover(pedido.id)
    mensagem.value = `Pedido #${pedido.id} excluído com sucesso.`
  } catch (error: any) {
    erro.value = error.message || 'Erro ao excluir pedido.'
  }
}

function editar(pedido: Pedido) {
  mensagem.value = `Edição do pedido #${pedido.id} será habilitada na próxima etapa.`
}
</script>

<template>
  <section>
    <h2 class="page-title">Pedidos</h2>
    <p class="page-subtitle">Acompanhe, filtre e execute pedidos pendentes.</p>

    <div class="toolbar card">
      <label class="field">
        Filtro
        <select v-model="filtro" class="form-control">
          <option value="todos">Todos</option>
          <option value="pendente">Pendentes</option>
          <option value="producao">Produção</option>
          <option value="concluido">Concluídos</option>
        </select>
      </label>

      <label class="field">
        IP CLP Estoque
        <input v-model="ipClp" class="form-control" type="text" />
      </label>

      <button class="btn" @click="pedidosStore.carregar">Atualizar</button>
    </div>

    <p v-if="mensagem" class="alert success">{{ mensagem }}</p>
    <p v-if="erro || pedidosStore.erro" class="alert error">{{ erro || pedidosStore.erro }}</p>

    <PedidoTabela
      :pedidos="pedidosFiltrados"
      :carregando="pedidosStore.carregando"
      @executar="executar"
      @excluir="excluir"
      @editar="editar"
    />
  </section>
</template>

<style scoped>
.toolbar {
  padding: 18px;
  display: flex;
  align-items: end;
  gap: 14px;
  margin: 22px 0;
  flex-wrap: wrap;
}

.toolbar .field {
  min-width: 220px;
}
</style>
