<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { usePedidosStore } from '@/stores/pedidos.store'
import type { Pedido } from '@/types/pedido'

const pedidosStore = usePedidosStore()

const ipClp = ref('10.74.241.10')
const mensagem = ref('')
const erro = ref('')

onMounted(() => {
  pedidosStore.carregar()
})

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
    erro.value = error.message
  }
}

async function remover(id: number) {
  mensagem.value = ''
  erro.value = ''

  try {
    await pedidosStore.remover(id)
    mensagem.value = 'Pedido removido com sucesso.'
  } catch (error: any) {
    erro.value = error.message
  }
}
</script>

<template>
  <section>
    <h2>Pedidos</h2>

    <div class="toolbar">
      <label>
        IP CLP Estoque
        <input v-model="ipClp" type="text" />
      </label>

      <button @click="pedidosStore.carregar">
        Atualizar
      </button>
    </div>

    <p v-if="mensagem" class="sucesso">{{ mensagem }}</p>
    <p v-if="erro || pedidosStore.erro" class="erro">
      {{ erro || pedidosStore.erro }}
    </p>

    <p v-if="pedidosStore.carregando">Carregando pedidos...</p>

    <table v-else>
      <thead>
        <tr>
          <th>ID</th>
          <th>Número</th>
          <th>Tipo</th>
          <th>Tampa</th>
          <th>Status</th>
          <th>Blocos</th>
          <th>Ações</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="pedido in pedidosStore.pedidos" :key="pedido.id">
          <td>{{ pedido.id }}</td>
          <td>{{ pedido.numeroPedido || '-' }}</td>
          <td>{{ pedido.tipo }}</td>
          <td>{{ pedido.tampa }}</td>
          <td>
            <span :class="['status', pedido.statusOrderProduction]">
              {{ pedido.statusOrderProduction }}
            </span>
          </td>
          <td>{{ pedido.blocos?.length || 0 }}</td>
          <td class="acoes">
            <button
              v-if="pedido.statusOrderProduction === 'pendente'"
              @click="executar(pedido)"
            >
              Executar
            </button>

            <button
              v-if="pedido.statusOrderProduction === 'pendente'"
              class="perigo"
              @click="remover(pedido.id)"
            >
              Excluir
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 16px;
  align-items: end;
  margin-bottom: 20px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-weight: 600;
}

input {
  height: 38px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 0 10px;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

th,
td {
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
}

th {
  background: #f9fafb;
}

button {
  padding: 8px 12px;
  border: 0;
  border-radius: 8px;
  background: #2563eb;
  color: white;
  cursor: pointer;
}

button.perigo {
  background: #dc2626;
}

.acoes {
  display: flex;
  gap: 8px;
}

.status {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status.pendente {
  background: #fef3c7;
  color: #92400e;
}

.status.producao {
  background: #dbeafe;
  color: #1e40af;
}

.status.concluido {
  background: #dcfce7;
  color: #166534;
}

.sucesso {
  background: #dcfce7;
  color: #166534;
  padding: 12px;
  border-radius: 8px;
}

.erro {
  background: #fee2e2;
  color: #991b1b;
  padding: 12px;
  border-radius: 8px;
}
</style>