import { createRouter, createWebHistory } from 'vue-router'

import DashboardView from '@/views/DashboardView.vue'
import LojaView from '@/views/LojaView.vue'
import PedidosView from '@/views/PedidosView.vue'
import GestaoView from '@/views/GestaoView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),

  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView,
    },
    {
      path: '/loja',
      name: 'loja',
      component: LojaView,
    },
    {
      path: '/pedidos',
      name: 'pedidos',
      component: PedidosView,
    },
    {
      path: '/gestao',
      name: 'gestao',
      component: GestaoView,
    },
  ],
})

export default router