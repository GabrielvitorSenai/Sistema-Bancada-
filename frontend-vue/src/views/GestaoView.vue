<script setup lang="ts">
import { onMounted, ref } from 'vue'
import EstoqueGrid from '@/components/estoque/EstoqueGrid.vue'
import ExpedicaoGrid from '@/components/expedicao/ExpedicaoGrid.vue'
import {
  buscarDisponibilidadeEstoque,
  listarEstoqueDisponivel,
  salvarEstoque,
} from '@/api/estoque.api'
import { buscarPrimeiraPosicaoLivre, salvarExpedicao } from '@/api/expedicao.api'

const mensagem = ref('')
const erro = ref('')
const disponibilidade = ref<Record<number, number>>({})
const estoque = ref<Record<string, number>>({})
const expedicao = ref<Record<string, number>>({})

for (let i = 1; i <= 28; i++) estoque.value[`P:${i}`] = 0
for (let i = 1; i <= 12; i++) expedicao.value[`P:${i}`] = 0

async function carregarEstoque() {
  disponibilidade.value = await buscarDisponibilidadeEstoque()
  const disponivel = await listarEstoqueDisponivel()
  for (let i = 1; i <= 28; i++) estoque.value[`P:${i}`] = 0
  for (const item of disponivel) estoque.value[`P:${item.posicao}`] = item.cor
}

async function carregar() {
  erro.value = ''
  try {
    await carregarEstoque()
  } catch (error: any) {
    erro.value = error.message || 'Erro ao carregar gestão.'
  }
}

async function gravarEstoque() {
  mensagem.value = ''
  erro.value = ''
  try {
    mensagem.value = await salvarEstoque(estoque.value)
    await carregarEstoque()
  } catch (error: any) {
    erro.value = error.message || 'Erro ao salvar estoque.'
  }
}

async function gravarExpedicao() {
  mensagem.value = ''
  erro.value = ''
  try {
    mensagem.value = await salvarExpedicao(expedicao.value)
  } catch (error: any) {
    erro.value = error.message || 'Erro ao salvar expedição.'
  }
}

async function verPrimeiraLivre() {
  mensagem.value = ''
  erro.value = ''
  try {
    const posicao = await buscarPrimeiraPosicaoLivre()
    mensagem.value = `Primeira posição livre na expedição: ${posicao}`
  } catch (error: any) {
    erro.value = error.message || 'Erro ao buscar posição livre.'
  }
}

onMounted(carregar)
</script>

<template>
  <section>
    <h2 class="page-title">Gestão</h2>
    <p class="page-subtitle">Configure o estoque e a expedição usados pela bancada.</p>

    <p v-if="mensagem" class="alert success">{{ mensagem }}</p>
    <p v-if="erro" class="alert error">{{ erro }}</p>

    <div class="disponibilidade">
      <article class="card item"><strong>{{ disponibilidade[1] || 0 }}</strong><span>Preto</span></article>
      <article class="card item"><strong>{{ disponibilidade[2] || 0 }}</strong><span>Vermelho</span></article>
      <article class="card item"><strong>{{ disponibilidade[3] || 0 }}</strong><span>Azul</span></article>
    </div>

    <article class="card painel">
      <div class="painel-topo">
        <div>
          <h3>Magazine de Estoque</h3>
          <p>28 posições físicas do estoque.</p>
        </div>
        <button class="btn" @click="gravarEstoque">Salvar estoque</button>
      </div>
      <EstoqueGrid v-model="estoque" />
    </article>

    <article class="card painel">
      <div class="painel-topo">
        <div>
          <h3>Magazine de Expedição</h3>
          <p>12 posições de saída com OP ou 0 para vazio.</p>
        </div>
        <div class="acoes">
          <button class="btn secondary" @click="verPrimeiraLivre">Primeira livre</button>
          <button class="btn" @click="gravarExpedicao">Salvar expedição</button>
        </div>
      </div>
      <ExpedicaoGrid v-model="expedicao" />
    </article>
  </section>
</template>

<style scoped>
.disponibilidade {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin: 22px 0;
}

.item {
  padding: 20px;
}

.item strong {
  display: block;
  font-size: 34px;
}

.item span {
  color: var(--color-muted);
  font-weight: 800;
}

.painel {
  padding: 20px;
  margin-bottom: 22px;
}

.painel-topo {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

h3 {
  margin: 0;
  font-size: 22px;
}

p {
  margin: 4px 0 0;
  color: var(--color-muted);
}

.acoes {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 760px) {
  .disponibilidade {
    grid-template-columns: 1fr;
  }

  .painel-topo {
    flex-direction: column;
  }
}
</style>
