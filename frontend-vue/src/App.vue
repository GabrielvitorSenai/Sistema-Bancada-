<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon">S4</div>

        <div>
          <h1>Sistema Bancada</h1>
          <span>Smart 4.0</span>
        </div>
      </div>

      <nav class="menu">
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
.app-shell {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 280px;
  background:
    linear-gradient(180deg, rgba(37, 99, 235, 0.18), transparent 35%),
    #0f172a;
  color: white;
  padding: 28px 24px;
  position: sticky;
  top: 0;
  height: 100vh;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 38px;
}

.brand-icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: linear-gradient(135deg, #2563eb, #22c55e);
  display: grid;
  place-items: center;
  font-weight: 900;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.35);
}

.brand h1 {
  margin: 0;
  font-size: 20px;
  line-height: 1.1;
}

.brand span {
  color: #94a3b8;
  font-size: 13px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.menu a {
  color: #cbd5e1;
  text-decoration: none;
  padding: 13px 14px;
  border-radius: 12px;
  font-weight: 700;
}

.menu a:hover {
  background: rgba(255, 255, 255, 0.08);
  color: white;
}

.menu a.router-link-active {
  background: var(--primary);
  color: white;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.30);
}

.content {
  flex: 1;
  padding: 34px;
  overflow-x: hidden;
}
</style>