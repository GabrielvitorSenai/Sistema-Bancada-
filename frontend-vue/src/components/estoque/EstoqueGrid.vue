<script setup lang="ts">
const model = defineModel<Record<string, number>>({ required: true })

function classeCor(valor: number) {
  return `cor-${valor || 0}`
}
</script>

<template>
  <div class="estoque-grid">
    <label v-for="pos in 28" :key="pos" :class="['posicao', classeCor(model[`P:${pos}`])]">
      <span>P{{ pos }}</span>
      <select v-model.number="model[`P:${pos}`]">
        <option :value="0">Vazio</option>
        <option :value="1">Preto</option>
        <option :value="2">Vermelho</option>
        <option :value="3">Azul</option>
      </select>
    </label>
  </div>
</template>

<style scoped>
.estoque-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(100px, 1fr));
  gap: 12px;
}

.posicao {
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 10px;
  background: #f8fafc;
  display: grid;
  gap: 8px;
}

.posicao span {
  font-weight: 900;
}

select {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px;
}

.cor-1 {
  border-color: #111827;
}

.cor-2 {
  border-color: #dc2626;
}

.cor-3 {
  border-color: #2563eb;
}

@media (max-width: 1000px) {
  .estoque-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 640px) {
  .estoque-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
