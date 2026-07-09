<script setup lang="ts">
defineProps<{
  titulo: string
  subtitulo: string
  imagem: string
}>()
</script>

<template>
  <article class="estacao-fluxo card">
    <div class="topo">
      <div>
        <h3>{{ titulo }}</h3>
        <p>{{ subtitulo }}</p>
      </div>
    </div>

    <div class="imagem-box">
      <img :src="imagem" :alt="titulo" />
    </div>
  </article>
</template>

<style scoped>
.estacao-fluxo {
  padding: 18px;
  transition: 0.2s ease;
}

.estacao-fluxo:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-strong);
}

.topo {
  margin-bottom: 14px;
}

h3 {
  margin: 0;
  font-size: 18px;
}

p {
  margin: 4px 0 0;
  color: var(--color-muted);
  font-size: 13px;
}

.imagem-box {
  height: 170px;
  border-radius: 14px;
  background:
    radial-gradient(circle at top, rgba(37, 99, 235, 0.12), transparent 45%),
    linear-gradient(135deg, #f8fafc, #e2e8f0);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.imagem-box img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
</style>
