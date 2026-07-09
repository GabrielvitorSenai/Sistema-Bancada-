<script setup lang="ts">
defineProps<{ status?: string }>()
</script>

<template>
  <span :class="['status-badge', status || 'pendente']">
    {{ status || 'pendente' }}
  </span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.status-badge.pendente {
  background: #fef3c7;
  color: #92400e;
}

.status-badge.producao,
.status-badge.produção {
  background: #dbeafe;
  color: #1e40af;
}

.status-badge.concluido,
.status-badge.concluído {
  background: #dcfce7;
  color: #166534;
}
</style>
