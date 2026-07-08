import { defineStore } from 'pinia'
import type { Pedido, PedidoDTO } from '@/types/pedido'
import {
  listarPedidos,
  criarPedido,
  editarPedido,
  excluirPedido,
  iniciarPedido,
} from '@/api/pedidos.api'

export const usePedidosStore = defineStore('pedidos', {
  state: () => ({
    pedidos: [] as Pedido[],
    carregando: false,
    erro: null as string | null,
  }),

  getters: {
    pendentes: state =>
      state.pedidos.filter(p => p.statusOrderProduction === 'pendente'),

    emProducao: state =>
      state.pedidos.filter(p => p.statusOrderProduction === 'producao'),

    concluidos: state =>
      state.pedidos.filter(p => p.statusOrderProduction === 'concluido'),
  },

  actions: {
    async carregar() {
      this.carregando = true
      this.erro = null

      try {
        this.pedidos = await listarPedidos()
      } catch (error: any) {
        this.erro = error.message
      } finally {
        this.carregando = false
      }
    },

    async criar(payload: PedidoDTO) {
      const pedido = await criarPedido(payload)
      await this.carregar()
      return pedido
    },

    async editar(id: number, payload: PedidoDTO) {
      await editarPedido(id, payload)
      await this.carregar()
    },

    async remover(id: number) {
      await excluirPedido(id)
      await this.carregar()
    },

    async executar(payload: PedidoDTO) {
      const resultado = await iniciarPedido(payload)
      await this.carregar()
      return resultado
    },
  },
})