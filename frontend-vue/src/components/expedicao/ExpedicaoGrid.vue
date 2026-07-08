<script setup lang="ts">
const model = defineModel<Record<string, number>>({ required: true })
</script>

<template>
  <div class="expedicao-grid">
    <label v-for="pos in 12" :key="pos" :class="['posicao', { ocupada: model[`P:${pos}`] > 0 }]">
      <span>P{{ pos }}</span>
      <input v-model.number="model[`P:${pos}`]" type="number" min="0" />
    </label>
  </div>
</template>

<style scoped>
.expedicao-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(110px, 1fr));
  gap: 12px;
}

.posicao {
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 10px;
  background: #ecfdf5;
  display: grid;
  gap: 8px;
}

.posicao.ocupada {
  background: #fee2e2;
  border-color: #fecaca;
}

span {
  font-weight: 900;
}

input {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px;
}

@media (max-width: 900px) {
  .expedicao-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 560px) {
  .expedicao-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
