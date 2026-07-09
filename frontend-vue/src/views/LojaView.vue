<script setup lang="ts">
import { ref } from 'vue'
import PedidoForm from '@/components/pedidos/PedidoForm.vue'
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
    erro.value = error.message || 'Erro ao salvar pedido.'
  }
}
</script>

<template>
  <section>
    <h2 class="page-title">Loja - Criar Pedido</h2>
    <p class="page-subtitle">Monte o pedido com tipo, tampa, blocos e lâminas.</p>

    <p v-if="sucesso" class="alert success">{{ sucesso }}</p>
    <p v-if="erro" class="alert error">{{ erro }}</p>

    <PedidoForm @salvar="salvarPedido" />
  </section>
</template>
