<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <h1>Sistema Bancada</h1>

      <nav>
        <RouterLink to="/">Dashboard</RouterLink>
        <RouterLink to="/loja">Loja</RouterLink>
        <RouterLink to="/pedidos">Pedidos</RouterLink>
        <RouterLink to="/gestao">Gestão</RouterLink>
      </nav>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  min-height: 100vh;
  background: #f4f6f8;
  color: #1f2937;
}

.sidebar {
  width: 240px;
  background: #111827;
  color: white;
  padding: 24px;
}

.sidebar h1 {
  font-size: 20px;
  margin-bottom: 32px;
}

.sidebar nav {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar a {
  color: #d1d5db;
  text-decoration: none;
  padding: 10px 12px;
  border-radius: 8px;
}

.sidebar a.router-link-active {
  background: #2563eb;
  color: white;
}

.content {
  flex: 1;
  padding: 32px;
}
</style>