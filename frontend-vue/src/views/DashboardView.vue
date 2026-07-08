<script setup lang="ts">
import { onMounted } from 'vue'
import { usePedidosStore } from '@/stores/pedidos.store'

const pedidosStore = usePedidosStore()

onMounted(() => {
  pedidosStore.carregar()
})
</script>

<template>
  <section>
    <h2>Dashboard</h2>

    <div class="cards">
      <div class="card">
        <strong>{{ pedidosStore.pedidos.length }}</strong>
        <span>Total de pedidos</span>
      </div>

      <div class="card">
        <strong>{{ pedidosStore.pendentes.length }}</strong>
        <span>Pendentes</span>
      </div>

      <div class="card">
        <strong>{{ pedidosStore.emProducao.length }}</strong>
        <span>Em produção</span>
      </div>

      <div class="card">
        <strong>{{ pedidosStore.concluidos.length }}</strong>
        <span>Concluídos</span>
      </div>
    </div>

    <p v-if="pedidosStore.erro" class="erro">
      {{ pedidosStore.erro }}
    </p>
  </section>
</template>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 4px #0001;
}

.card strong {
  display: block;
  font-size: 32px;
}

.card span {
  color: #6b7280;
}

.erro {
  margin-top: 20px;
  color: #dc2626;
}
</style>