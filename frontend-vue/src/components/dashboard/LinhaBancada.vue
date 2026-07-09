<script setup lang="ts">
import type { StatusBancada } from '@/stores/bancada.store'
import { imagemEstacao } from '@/utils/assets'

const props = defineProps<{
  status: StatusBancada
}>()

const overlays = [
  { codigo: 'Est', nome: 'Estoque' },
  { codigo: 'Pro', nome: 'Processo' },
  { codigo: 'Mon', nome: 'Montagem' },
  { codigo: 'Exp', nome: 'Expedição' },
] as const
</script>

<template>
  <article class="linha-completa card">
    <div class="topo">
      <div>
        <h3>Bancada Smart 4.0</h3>
        <p>Imagem completa da bancada com indicadores de status sobrepostos.</p>
      </div>
      <span :class="['status', props.status]">{{ props.status }}</span>
    </div>

    <div class="canvas">
      <img class="base" src="/assets/bancada/Smart40.png" alt="Bancada Smart 4.0" />
      <img
        v-for="overlay in overlays"
        :key="overlay.codigo"
        class="overlay"
        :src="imagemEstacao(overlay.codigo, props.status)"
        :alt="overlay.nome"
      />
    </div>
  </article>
</template>

<style scoped>
.linha-completa {
  padding: 20px;
  margin: 22px 0;
}

.topo {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

h3 {
  margin: 0;
  font-size: 22px;
}

p {
  margin: 4px 0 0;
  color: var(--color-muted);
}

.status {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.status.online {
  background: #dcfce7;
  color: #166534;
}

.status.pausado {
  background: #fef3c7;
  color: #92400e;
}

.status.offline {
  background: #fee2e2;
  color: #991b1b;
}

.canvas {
  position: relative;
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  aspect-ratio: 703 / 355;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.base,
.overlay {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.base {
  z-index: 1;
}

.overlay {
  z-index: 2;
  pointer-events: none;
}
</style>
