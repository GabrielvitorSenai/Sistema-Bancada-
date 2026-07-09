<template>
  <header class="app-header">
    <div>
      <strong>Painel Operacional</strong>
      <span>Frontend Vue desacoplado do Spring Boot</span>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

strong {
  display: block;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-primary);
}

span {
  color: var(--color-muted);
}
</style>
