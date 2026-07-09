import type { StatusBancada } from '@/stores/bancada.store'

export type EstacaoCodigo = 'Est' | 'Pro' | 'Mon' | 'Exp'

const sufixoStatus: Record<StatusBancada, string> = {
  online: 'on',
  pausado: 'pause',
  offline: 'off',
}

export function imagemEstacao(codigo: EstacaoCodigo, status: StatusBancada) {
  return `/assets/bancada/Smart40-${codigo}_${sufixoStatus[status]}.png`
}

export function imagemFluxoEstacao(nome: 'Estoque' | 'Processo' | 'Montagem' | 'Expedicao', etapa: 0 | 1 | 2) {
  return `/assets/bancada/Smart40_${nome}_${etapa}.png`
}

export function imagemBloco(cor: number) {
  return `/assets/bloco/rBlocoCor${cor || 0}.png`
}

export function imagemTampa(cor: number) {
  return `/assets/bloco/rTampa${cor || 0}.png`
}

export function imagemLamina(posicao: number, cor: number) {
  return `/assets/laminas/lamina${posicao}-${cor || 0}.png`
}

export function imagemPadrao(padrao: number, lado: 1 | 2 = 1) {
  return `/assets/padroes/padrao${padrao || 0}-${lado}.png`
}

export const statusIcons = {
  estoque: '/assets/status/estoqueIcone.png',
  laminas: '/assets/status/laminasIcone.png',
  montagem: '/assets/status/montagemIcone.png',
  qualidade: '/assets/status/qualidadeIcone.png',
  expedicao: '/assets/status/expedicaoIcone.png',
}

export const senaiLogo = '/assets/senai.png'
export const backgroundVerde = '/assets/background/background_verde.png'
