import { api } from './http'

export interface EstoqueDisponivelItem {
  posicao: number
  cor: number
}

export async function listarEstoqueDisponivel() {
  const { data } = await api.get<EstoqueDisponivelItem[]>('/api/estoque/disponivel')
  return data
}

export async function buscarDisponibilidadeEstoque() {
  const { data } = await api.get<Record<number, number>>('/estoque/disponibilidade')
  return data
}

export async function salvarEstoque(payload: Record<string, number>) {
  const { data } = await api.post<string>('/estoque/salvar', payload)
  return data
}

export async function buscarPrimeiraPosicaoPorCor(cor: number) {
  const { data } = await api.get<number>(`/estoque/primeira-posicao/${cor}`)
  return data
}

export async function enviarEstoqueParaClp(ipClp: string) {
  const { data } = await api.post<string>('/clp/enviar-estoque', { ipClp })
  return data
}
