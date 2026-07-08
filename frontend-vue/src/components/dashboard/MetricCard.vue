<script setup lang="ts">
defineProps<{
  titulo: string
  valor: number | string
  descricao?: string
}>()
</script>

<template>
  <article class="metric-card card">
    <span>{{ titulo }}</span>
    <strong>{{ valor }}</strong>
    <small v-if="descricao">{{ descricao }}</small>
  </article>
</template>

<style scoped>
.metric-card {
  padding: 22px;
}

span {
  display: block;
  color: var(--color-muted);
  font-weight: 800;
  margin-bottom: 10px;
}

strong {
  display: block;
  font-size: 36px;
  color: var(--color-text);
  line-height: 1;
}

small {
  display: block;
  margin-top: 10px;
  color: var(--color-muted);
}
</style>
