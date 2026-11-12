import type { DadosCertificado } from '../api/tipos'

export interface ValoresCertificado {
  titulo: string
  instituicao: string
  dataInicio: string
  dataFim: string
  horasCertificado: string
  itemNumero: string
  /** Recado opcional para quem analisa (até 250 caracteres). */
  observacao: string
}

export const valoresVazios: ValoresCertificado = {
  titulo: '',
  instituicao: '',
  dataInicio: '',
  dataFim: '',
  horasCertificado: '',
  itemNumero: '',
  observacao: '',
}

/** Converte o estado do formulário (strings) para o contrato da API. */
export function paraDados(v: ValoresCertificado): DadosCertificado {
  return {
    titulo: v.titulo.trim(),
    instituicao: v.instituicao.trim() || undefined,
    dataInicio: v.dataInicio || undefined,
    dataFim: v.dataFim || undefined,
    horasCertificado: Number(v.horasCertificado.replace(',', '.')),
    itemNumero: Number(v.itemNumero),
    observacao: v.observacao.trim() || undefined,
  }
}

export function numeroOuNada(texto: string): number | undefined {
  if (texto.trim() === '') return undefined
  const n = Number(texto.replace(',', '.'))
  return Number.isNaN(n) ? undefined : n
}
