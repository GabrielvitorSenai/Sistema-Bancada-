<script setup lang="ts">
defineProps<{
  titulo: string
  subtitulo: string
  imagem: string
  status: 'online' | 'pausado' | 'offline'
}>()
</script>

<template>
  <article class="bancada-card">
    <div class="card-topo">
      <div>
        <h3>{{ titulo }}</h3>
        <p>{{ subtitulo }}</p>
      </div>

      <span :class="['status', status]">
        {{ status }}
      </span>
    </div>

    <div class="imagem-area">
      <img :src="imagem" :alt="titulo" />
    </div>
  </article>
</template>

<style scoped>
.bancada-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
  transition: 0.2s ease;
}

.bancada-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
}

.card-topo {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.status {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
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

.imagem-area {
  height: 170px;
  border-radius: 14px;
  background:
    radial-gradient(circle at top, rgba(37, 99, 235, 0.12), transparent 45%),
    linear-gradient(135deg, #f8fafc, #e2e8f0);
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.imagem-area img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
</style>