import { api } from './http'
import type { Pedido, PedidoDTO } from '@/types/pedido'

export async function listarPedidos() {
  const { data } = await api.get<Pedido[]>('/api/pedidos')
  return data
}

export async function criarPedido(payload: PedidoDTO) {
  const { data } = await api.post<Pedido>('/api/pedidos', payload)
  return data
}

export async function editarPedido(id: number, payload: PedidoDTO) {
  const { data } = await api.put<string>(`/api/pedidos/${id}`, payload)
  return data
}

export async function excluirPedido(id: number) {
  const { data } = await api.delete<string>(`/api/pedidos/${id}`)
  return data
}

export async function atualizarStatusPedido(id: number, status: string) {
  const { data } = await api.put<string>(`/api/pedidos/${id}/status`, {
    statusOrderProduction: status,
  })
  return data
}

export async function iniciarPedido(payload: PedidoDTO) {
  const response = await api.post<string>('/iniciar-pedido', payload)
  return {
    mensagem: response.data,
    numeroOp: response.headers['x-numero-op'] as string | undefined,
  }
}

export async function finalizarPedido(payload: PedidoDTO) {
  const response = await api.post<string>('/finalizar-pedido-producao', payload)
  return {
    mensagem: response.data,
    numeroOp: response.headers['x-numero-op'] as string | undefined,
  }
}
