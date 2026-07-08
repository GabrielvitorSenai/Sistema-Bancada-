<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
</script>

<template>
  <div class="app-shell">
    <AppSidebar />

    <main class="content">
      <AppHeader />
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
}

.content {
  flex: 1;
  padding: 34px;
  overflow-x: hidden;
}

@media (max-width: 900px) {
  .app-shell {
    flex-direction: column;
  }

  .content {
    padding: 20px;
  }
}
</style>
