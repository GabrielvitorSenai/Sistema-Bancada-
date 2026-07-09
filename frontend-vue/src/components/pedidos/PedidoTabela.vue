<script setup lang="ts">
import type { Pedido } from '@/types/pedido'
import { nomeTampa } from '@/types/pedido'
import PedidoStatusBadge from './PedidoStatusBadge.vue'

defineProps<{
  pedidos: Pedido[]
  carregando?: boolean
}>()

const emit = defineEmits<{
  executar: [pedido: Pedido]
  excluir: [pedido: Pedido]
  editar: [pedido: Pedido]
}>()
</script>

<template>
  <div class="table-wrapper card">
    <table>
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
        <tr v-if="carregando">
          <td colspan="7">Carregando pedidos...</td>
        </tr>
        <tr v-else-if="pedidos.length === 0">
          <td colspan="7">Nenhum pedido encontrado.</td>
        </tr>
        <template v-else>
          <tr v-for="pedido in pedidos" :key="pedido.id">
            <td>#{{ pedido.id }}</td>
            <td>{{ pedido.numeroPedido || '-' }}</td>
            <td>{{ pedido.tipo }}</td>
            <td>{{ nomeTampa(pedido.tampa) }}</td>
            <td><PedidoStatusBadge :status="pedido.statusOrderProduction" /></td>
            <td>{{ pedido.blocos?.length || 0 }}</td>
            <td class="acoes">
              <button
                class="btn success"
                :disabled="pedido.statusOrderProduction !== 'pendente'"
                @click="emit('executar', pedido)"
              >
                Executar
              </button>
              <button
                class="btn secondary"
                :disabled="pedido.statusOrderProduction !== 'pendente'"
                @click="emit('editar', pedido)"
              >
                Editar
              </button>
              <button
                class="btn danger"
                :disabled="pedido.statusOrderProduction !== 'pendente'"
                @click="emit('excluir', pedido)"
              >
                Excluir
              </button>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-wrapper {
  overflow-x: auto;
}

.acoes {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn {
  padding: 8px 10px;
  font-size: 13px;
}
</style>
