<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { PedidoDTO, TipoPedido } from '@/types/pedido'

const emit = defineEmits<{
  salvar: [pedido: PedidoDTO]
}>()

const form = reactive<PedidoDTO>({
  numeroPedido: '',
  tipo: 'simples',
  tampa: 1,
  posicaoExpedicao: 0,
  statusOrderProduction: 'pendente',
  blocos: [],
})

function quantidadeBlocosPorTipo(tipo: TipoPedido) {
  if (tipo === 'simples') return 1
  if (tipo === 'duplo') return 2
  return 3
}

function criarLaminasPadrao() {
  return [
    { cor: 0, padrao: 0 },
    { cor: 0, padrao: 0 },
    { cor: 0, padrao: 0 },
  ]
}

function montarBlocos() {
  const quantidade = quantidadeBlocosPorTipo(form.tipo)

  form.blocos = Array.from({ length: quantidade }, (_, index) => ({
    andar: index + 1,
    corBloco: 1,
    laminas: criarLaminasPadrao(),
  }))
}

watch(
  () => form.tipo,
  () => montarBlocos(),
  { immediate: true },
)

function salvar() {
  emit('salvar', JSON.parse(JSON.stringify(form)))
}
</script>

<template>
  <form class="form" @submit.prevent="salvar">
    <div class="grid">
      <label>
        Número do pedido
        <input v-model="form.numeroPedido" type="text" placeholder="Ex: 1001" />
      </label>

      <label>
        Tipo
        <select v-model="form.tipo">
          <option value="simples">Simples</option>
          <option value="duplo">Duplo</option>
          <option value="triplo">Triplo</option>
        </select>
      </label>

      <label>
        Tampa
        <select v-model.number="form.tampa">
          <option :value="1">Preto</option>
          <option :value="2">Vermelho</option>
          <option :value="3">Azul</option>
        </select>
      </label>

      <label>
        Posição expedição
        <select v-model.number="form.posicaoExpedicao">
          <option :value="0">Automática</option>
          <option v-for="pos in 12" :key="pos" :value="pos">
            Posição {{ pos }}
          </option>
        </select>
      </label>
    </div>

    <div class="blocos">
      <div v-for="bloco in form.blocos" :key="bloco.andar" class="bloco">
        <h3>Bloco / Andar {{ bloco.andar }}</h3>

        <label>
          Cor do bloco
          <select v-model.number="bloco.corBloco">
            <option :value="1">Preto</option>
            <option :value="2">Vermelho</option>
            <option :value="3">Azul</option>
          </select>
        </label>

        <div class="laminas">
          <div
            v-for="(lamina, index) in bloco.laminas"
            :key="index"
            class="lamina"
          >
            <strong>Lâmina {{ index + 1 }}</strong>

            <label>
              Cor
              <select v-model.number="lamina.cor">
                <option :value="0">Nenhuma</option>
                <option :value="1">Cor 1</option>
                <option :value="2">Cor 2</option>
                <option :value="3">Cor 3</option>
                <option :value="4">Cor 4</option>
                <option :value="5">Cor 5</option>
                <option :value="6">Cor 6</option>
              </select>
            </label>

            <label>
              Padrão
              <select v-model.number="lamina.padrao">
                <option :value="0">Nenhum</option>
                <option :value="1">Casa</option>
                <option :value="2">Navio</option>
                <option :value="3">Estrela</option>
              </select>
            </label>
          </div>
        </div>
      </div>
    </div>

    <button type="submit">
      Salvar pedido
    </button>
  </form>
</template>

<style scoped>
.form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-weight: 600;
}

input,
select {
  height: 38px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 0 10px;
}

.blocos {
  display: grid;
  gap: 16px;
}

.bloco {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 4px #0001;
}

.laminas {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 16px;
}

.lamina {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
}

button {
  width: fit-content;
  padding: 12px 20px;
  border: 0;
  border-radius: 8px;
  background: #2563eb;
  color: white;
  font-weight: 700;
  cursor: pointer;
}
</style>