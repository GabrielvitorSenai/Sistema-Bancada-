import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8088',
      '/iniciar-pedido': 'http://localhost:8088',
      '/finalizar-pedido-producao': 'http://localhost:8088',
      '/estoque': 'http://localhost:8088',
      '/expedicao': 'http://localhost:8088',
      '/clp': 'http://localhost:8088',
      '/start-leituras': 'http://localhost:8088',
      '/stop-leituras': 'http://localhost:8088',
      '/pedidos-expedicao': 'http://localhost:8088',
    },
  },
})
