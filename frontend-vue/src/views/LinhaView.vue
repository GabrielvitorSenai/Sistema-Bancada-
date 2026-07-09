<script setup lang="ts">
import BancadaCard from '@/components/dashboard/BancadaCard.vue'
import { useBancadaStore } from '@/stores/bancada.store'
import { imagemEstacao } from '@/utils/assets'

const bancadaStore = useBancadaStore()

const estacoes = [
  ['Estoque', 'Magazine de Estoque', 'Est'],
  ['Processo', 'Estação de Processo', 'Pro'],
  ['Montagem', 'Estação de Montagem', 'Mon'],
  ['Expedição', 'Magazine de Expedição', 'Exp'],
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

    <div class="grid-linha">
      <BancadaCard
        v-for="estacao in estacoes"
        :key="estacao[0]"
        :titulo="estacao[0]"
        :subtitulo="estacao[1]"
        :imagem="imagemEstacao(estacao[2], bancadaStore.status)"
        :status="bancadaStore.status"
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
