<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
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
      <RouterLink to="/linha">Linha</RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 280px;
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(37, 99, 235, 0.18), transparent 36%),
    var(--color-sidebar);
  color: white;
  padding: 28px 24px;
  position: sticky;
  top: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 38px;
}

.brand-icon {
  width: 50px;
  height: 50px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2563eb, #22c55e);
  font-weight: 900;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.35);
}

h1 {
  margin: 0;
  font-size: 20px;
  line-height: 1.1;
}

span {
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
  font-weight: 800;
}

.menu a:hover {
  background: rgba(255, 255, 255, 0.08);
  color: white;
}

.menu a.router-link-active {
  background: var(--color-primary);
  color: white;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.3);
}

@media (max-width: 900px) {
  .sidebar {
    position: static;
    width: 100%;
    min-height: auto;
  }

  .menu {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
