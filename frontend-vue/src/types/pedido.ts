export type TipoPedido = 'simples' | 'duplo' | 'triplo'
export type StatusPedido = 'pendente' | 'producao' | 'concluido'

export interface LaminaDTO {
  cor: number
  padrao: number
}

export interface BlocoDTO {
  andar: number
  posicaoEstoque?: number
  corBloco: number
  laminas: LaminaDTO[]
}

export interface PedidoDTO {
  id?: number
  numeroPedido?: string
  tipo: TipoPedido
  tampa: number
  posicaoExpedicao: number
  ipClp?: string
  statusOrderProduction?: StatusPedido | string
  timeStamp?: string
  blocos: BlocoDTO[]
}

export interface Pedido extends PedidoDTO {
  id: number
  orderProduction?: number
}

export function nomeTampa(codigo: number) {
  if (codigo === 1) return 'Preto'
  if (codigo === 2) return 'Vermelho'
  if (codigo === 3) return 'Azul'
  return `Cor ${codigo}`
}
