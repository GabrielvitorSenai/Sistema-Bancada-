import { defineStore } from 'pinia'
import { api } from '@/api/http'

export type StatusBancada = 'online' | 'pausado' | 'offline'

interface BancadaState {
  ipBase: string
  conectada: boolean
  carregando: boolean
  erro: string | null
}

export const useBancadaStore = defineStore('bancada', {
  state: (): BancadaState => ({
    ipBase: '10.74.241.10',
    conectada: false,
    carregando: false,
    erro: null,
  }),

  getters: {
    status: state => (state.conectada ? 'online' : 'pausado') as StatusBancada,
    ips: state => {
      const partes = state.ipBase.split('.')
      const base = partes.length === 4 ? partes.slice(0, 3).join('.') : '10.74.241'
      return {
        hostIpEstoque: `${base}.10`,
        hostIpProcesso: `${base}.20`,
        hostIpMontagem: `${base}.30`,
        hostIpExpedicao: `${base}.40`,
      }
    },
  },

  actions: {
    async conectar() {
      this.carregando = true
      this.erro = null
      try {
        await api.post('/start-leituras', this.ips)
        this.conectada = true
      } catch (error: any) {
        console.error('Erro ao conectar bancada:', error)
        this.erro = error.message || 'Erro ao conectar bancada.'
      } finally {
        this.carregando = false
      }
    },

    async desconectar() {
      this.carregando = true
      this.erro = null
      try {
        await api.post('/stop-leituras')
        this.conectada = false
      } catch (error: any) {
        console.error('Erro ao desconectar bancada:', error)
        this.erro = error.message || 'Erro ao desconectar bancada.'
      } finally {
        this.carregando = false
      }
    },
  },
})
