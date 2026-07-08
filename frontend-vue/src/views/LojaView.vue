<script setup lang="ts">
import { ref } from 'vue'
import PedidoForm from '@/components/PedidoForm.vue'
import { usePedidosStore } from '@/stores/pedidos.store'
import type { PedidoDTO } from '@/types/pedido'

const pedidosStore = usePedidosStore()

const sucesso = ref('')
const erro = ref('')

async function salvarPedido(payload: PedidoDTO) {
  sucesso.value = ''
  erro.value = ''

  try {
    const pedido = await pedidosStore.criar(payload)
    sucesso.value = `Pedido #${pedido.id} criado com sucesso.`
  } catch (error: any) {
    erro.value = error.message
  }
}
</script>

<template>
  <section>
    <h2>Loja - Criar Pedido</h2>

    <p v-if="sucesso" class="sucesso">{{ sucesso }}</p>
    <p v-if="erro" class="erro">{{ erro }}</p>

    <PedidoForm @salvar="salvarPedido" />
  </section>
</template>

<style scoped>
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