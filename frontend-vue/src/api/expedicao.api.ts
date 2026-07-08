import { api } from './http'

export async function salvarExpedicao(payload: Record<string, number>) {
  const { data } = await api.post<string>('/expedicao/salvar', payload)
  return data
}

export async function buscarPrimeiraPosicaoLivre() {
  const { data } = await api.get<number>('/expedicao/primeira-livre')
  return data
}

export async function enviarExpedicaoParaClp(ipClp: string) {
  const { data } = await api.post<string>('/clp/enviar-expedicao', { ipClp })
  return data
}