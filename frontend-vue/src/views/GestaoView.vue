<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  listarEstoqueDisponivel,
  salvarEstoque,
  buscarDisponibilidadeEstoque,
} from '@/api/estoque.api'
import {
  salvarExpedicao,
  buscarPrimeiraPosicaoLivre,
} from '@/api/expedicao.api'

const mensagem = ref('')
const erro = ref('')

const disponibilidade = ref<Record<number, number>>({})
const estoque = ref<Record<string, number>>({})
const expedicao = ref<Record<string, number>>({})

for (let i = 1; i <= 28; i++) {
  estoque.value[`P:${i}`] = 0
}

for (let i = 1; i <= 12; i++) {
  expedicao.value[`P:${i}`] = 0
}

async function carregar() {
  try {
    disponibilidade.value = await buscarDisponibilidadeEstoque()

    const disponivel = await listarEstoqueDisponivel()

    for (const item of disponivel) {
      estoque.value[`P:${item.posicao}`] = item.cor
    }
  } catch (error: any) {
    erro.value = error.message
  }
}

async function gravarEstoque() {
  mensagem.value = ''
  erro.value = ''

  try {
    mensagem.value = await salvarEstoque(estoque.value)
    await carregar()
  } catch (error: any) {
    erro.value = error.message
  }
}

async function gravarExpedicao() {
  mensagem.value = ''
  erro.value = ''

  try {
    mensagem.value = await salvarExpedicao(expedicao.value)
  } catch (error: any) {
    erro.value = error.message
  }
}

async function verPrimeiraLivre() {
  mensagem.value = ''
  erro.value = ''

  try {
    const posicao = await buscarPrimeiraPosicaoLivre()
    mensagem.value = `Primeira posição livre na expedição: ${posicao}`
  } catch (error: any) {
    erro.value = error.message
  }
}

onMounted(carregar)
</script>

<template>
  <section>
    <h2>Gestão</h2>

    <p v-if="mensagem" class="sucesso">{{ mensagem }}</p>
    <p v-if="erro" class="erro">{{ erro }}</p>

    <div class="box">
      <h3>Disponibilidade de Estoque</h3>

      <p>Preto: {{ disponibilidade[1] || 0 }}</p>
      <p>Vermelho: {{ disponibilidade[2] || 0 }}</p>
      <p>Azul: {{ disponibilidade[3] || 0 }}</p>
    </div>

    <div class="box">
      <h3>Estoque</h3>

      <div class="grid estoque">
        <label v-for="pos in 28" :key="pos">
          P{{ pos }}
          <select v-model.number="estoque[`P:${pos}`]">
            <option :value="0">Vazio</option>
            <option :value="1">Preto</option>
            <option :value="2">Vermelho</option>
            <option :value="3">Azul</option>
          </select>
        </label>
      </div>

      <button @click="gravarEstoque">
        Salvar estoque
      </button>
    </div>

    <div class="box">
      <h3>Expedição</h3>

      <div class="grid expedicao">
        <label v-for="pos in 12" :key="pos">
          P{{ pos }}
          <input
            v-model.number="expedicao[`P:${pos}`]"
            type="number"
            min="0"
          />
        </label>
      </div>

      <button @click="gravarExpedicao">
        Salvar expedição
      </button>

      <button @click="verPrimeiraLivre">
        Ver primeira livre
      </button>
    </div>
  </section>
</template>

<style scoped>
.box {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 1px 4px #0001;
}

.grid {
  display: grid;
  gap: 12px;
  margin-bottom: 16px;
}

.estoque {
  grid-template-columns: repeat(7, 1fr);
}

.expedicao {
  grid-template-columns: repeat(6, 1fr);
}

label {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

input,
select {
  height: 36px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 0 8px;
}

button {
  margin-right: 8px;
  padding: 10px 14px;
  border: 0;
  border-radius: 8px;
  background: #2563eb;
  color: white;
  cursor: pointer;
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