<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { PedidoDTO, TipoPedido } from '@/types/pedido'

const emit = defineEmits<{ salvar: [pedido: PedidoDTO] }>()

const form = reactive<PedidoDTO>({
  numeroPedido: '',
  tipo: 'simples',
  tampa: 1,
  posicaoExpedicao: 0,
  statusOrderProduction: 'pendente',
  blocos: [],
})

function quantidadeBlocos(tipo: TipoPedido) {
  if (tipo === 'simples') return 1
  if (tipo === 'duplo') return 2
  return 3
}

function laminasPadrao() {
  return [
    { cor: 0, padrao: 0 },
    { cor: 0, padrao: 0 },
    { cor: 0, padrao: 0 },
  ]
}

function montarBlocos() {
  form.blocos = Array.from({ length: quantidadeBlocos(form.tipo) }, (_, index) => ({
    andar: index + 1,
    corBloco: 1,
    laminas: laminasPadrao(),
  }))
}

watch(() => form.tipo, montarBlocos, { immediate: true })

function salvar() {
  emit('salvar', JSON.parse(JSON.stringify(form)))
}
</script>

<template>
  <form class="pedido-form" @submit.prevent="salvar">
    <div class="form-grid card">
      <label class="field">
        Número do pedido
        <input v-model="form.numeroPedido" class="form-control" type="text" placeholder="Ex: 1001" />
      </label>

      <label class="field">
        Tipo
        <select v-model="form.tipo" class="form-control">
          <option value="simples">Simples</option>
          <option value="duplo">Duplo</option>
          <option value="triplo">Triplo</option>
        </select>
      </label>

      <label class="field">
        Tampa
        <select v-model.number="form.tampa" class="form-control">
          <option :value="1">Preto</option>
          <option :value="2">Vermelho</option>
          <option :value="3">Azul</option>
        </select>
      </label>

      <label class="field">
        Posição expedição
        <select v-model.number="form.posicaoExpedicao" class="form-control">
          <option :value="0">Automática</option>
          <option v-for="pos in 12" :key="pos" :value="pos">Posição {{ pos }}</option>
        </select>
      </label>
    </div>

    <div class="blocos">
      <article v-for="bloco in form.blocos" :key="bloco.andar" class="bloco card">
        <div class="bloco-topo">
          <h3>Bloco / Andar {{ bloco.andar }}</h3>
          <label class="field pequeno">
            Cor do bloco
            <select v-model.number="bloco.corBloco" class="form-control">
              <option :value="1">Preto</option>
              <option :value="2">Vermelho</option>
              <option :value="3">Azul</option>
            </select>
          </label>
        </div>

        <div class="laminas">
          <div v-for="(lamina, index) in bloco.laminas" :key="index" class="lamina">
            <strong>Lâmina {{ index + 1 }}</strong>
            <label class="field">
              Cor
              <select v-model.number="lamina.cor" class="form-control">
                <option :value="0">Nenhuma</option>
                <option v-for="cor in 6" :key="cor" :value="cor">Cor {{ cor }}</option>
              </select>
            </label>
            <label class="field">
              Padrão
              <select v-model.number="lamina.padrao" class="form-control">
                <option :value="0">Nenhum</option>
                <option :value="1">Casa</option>
                <option :value="2">Navio</option>
                <option :value="3">Estrela</option>
              </select>
            </label>
          </div>
        </div>
      </article>
    </div>

    <button class="btn" type="submit">Salvar pedido</button>
  </form>
</template>

<style scoped>
.pedido-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 16px;
  padding: 20px;
}

.blocos {
  display: grid;
  gap: 16px;
}

.bloco {
  padding: 20px;
}

.bloco-topo {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

h3 {
  margin: 0;
}

.pequeno {
  min-width: 180px;
}

.laminas {
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 14px;
}

.lamina {
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 14px;
  background: #f8fafc;
  display: grid;
  gap: 10px;
}

@media (max-width: 1000px) {
  .form-grid,
  .laminas {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 700px) {
  .form-grid,
  .laminas {
    grid-template-columns: 1fr;
  }

  .bloco-topo {
    flex-direction: column;
  }
}
</style>
