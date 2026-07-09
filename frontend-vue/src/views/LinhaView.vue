<script setup lang="ts">
import LinhaBancada from '@/components/dashboard/LinhaBancada.vue'
import EstacaoFluxoCard from '@/components/dashboard/EstacaoFluxoCard.vue'
import { useBancadaStore } from '@/stores/bancada.store'
import { imagemFluxoEstacao } from '@/utils/assets'

const bancadaStore = useBancadaStore()

const estacoes = [
  ['Estoque', 'Magazine de Estoque', 'Estoque'],
  ['Processo', 'Estação de Processo', 'Processo'],
  ['Montagem', 'Estação de Montagem', 'Montagem'],
  ['Expedição', 'Magazine de Expedição', 'Expedicao'],
] as const
</script>

<template>
  <section>
    <h2 class="page-title">Linha</h2>
    <p class="page-subtitle">Primeira versão visual da linha, preparada para leitura dos CLPs.</p>

    <div class="controle card">
      <label class="field">
        IP base
        <input v-model="bancadaStore.ipBase" class="form-control" type="text" />
      </label>
      <button class="btn success" :disabled="bancadaStore.carregando" @click="bancadaStore.conectar">
        Conectar bancada
      </button>
      <button class="btn danger" :disabled="bancadaStore.carregando" @click="bancadaStore.desconectar">
        Desconectar
      </button>
    </div>

    <p v-if="bancadaStore.erro" class="alert error">{{ bancadaStore.erro }}</p>

    <LinhaBancada :status="bancadaStore.status" />

    <div class="section-title">
      <h3>Estações individuais</h3>
      <p>Essas imagens usam os assets de fluxo da bancada, não os overlays de status.</p>
    </div>

    <div class="grid-linha">
      <EstacaoFluxoCard
        v-for="estacao in estacoes"
        :key="estacao[0]"
        :titulo="estacao[0]"
        :subtitulo="estacao[1]"
        :imagem="imagemFluxoEstacao(estacao[2], 0)"
      />
    </div>
  </section>
</template>

<style scoped>
.controle {
  padding: 18px;
  display: flex;
  align-items: end;
  gap: 12px;
  flex-wrap: wrap;
  margin: 22px 0;
}

.controle .field {
  min-width: 240px;
}

.section-title {
  margin: 30px 0 18px;
}

.section-title h3 {
  margin: 0;
  font-size: 24px;
}

.section-title p {
  margin: 4px 0 0;
  color: var(--color-muted);
}

.grid-linha {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 18px;
}

@media (max-width: 1200px) {
  .grid-linha {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 760px) {
  .grid-linha {
    grid-template-columns: 1fr;
  }
}
</style>
