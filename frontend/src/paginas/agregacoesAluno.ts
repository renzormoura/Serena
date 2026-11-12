import type { Certificado } from '../api/tipos'

/** "2025-03-10" → "2025.1"; julho em diante é o 2º semestre. */
export function semestreDe(isoData: string): string {
  const [ano, mes] = isoData.slice(0, 10).split('-').map(Number)
  return `${ano}.${mes <= 6 ? 1 : 2}`
}

function proximo(semestre: string): string {
  const [ano, s] = semestre.split('.').map(Number)
  return s === 1 ? `${ano}.2` : `${ano + 1}.1`
}

export interface PontoSemestre {
  rotulo: string
  deferidas: number
  emAnalise: number
}

/**
 * Horas que contam por semestre da atividade (data de início, senão a de término, senão a do envio), separando o
 * já deferido do que está em análise. Semestres sem atividade entre o primeiro e o último aparecem zerados.
 */
export function horasPorSemestre(certificados: Certificado[]): PontoSemestre[] {
  const mapa = new Map<string, PontoSemestre>()
  for (const c of certificados) {
    const deferido = c.status === 'DEFERIDO'
    const emAnalise = c.status === 'CONFIRMADO' || c.status === 'AJUSTE_SOLICITADO'
    if ((!deferido && !emAnalise) || !c.horasQueContam) continue
    const rotulo = semestreDe(c.dataInicio ?? c.dataFim ?? c.criadoEm)
    const ponto = mapa.get(rotulo) ?? { rotulo, deferidas: 0, emAnalise: 0 }
    if (deferido) ponto.deferidas += c.horasQueContam
    else ponto.emAnalise += c.horasQueContam
    mapa.set(rotulo, ponto)
  }
  const ordenados = [...mapa.keys()].sort()
  if (ordenados.length === 0) return []
  const resultado: PontoSemestre[] = []
  for (let s = ordenados[0]; ; s = proximo(s)) {
    resultado.push(mapa.get(s) ?? { rotulo: s, deferidas: 0, emAnalise: 0 })
    if (s === ordenados[ordenados.length - 1]) break
  }
  return resultado
}

export interface UsoDoItem {
  numero: number
  nome: string
  certificados: number
  horas: number
}

/** Horas que contam por item (deferidas + em análise), do maior para o menor. */
export function horasPorItem(certificados: Certificado[]): UsoDoItem[] {
  const mapa = new Map<number, UsoDoItem>()
  for (const c of certificados) {
    if (!c.item || !['DEFERIDO', 'CONFIRMADO', 'AJUSTE_SOLICITADO'].includes(c.status)) continue
    const uso = mapa.get(c.item.numero) ?? { numero: c.item.numero, nome: c.item.nome, certificados: 0, horas: 0 }
    uso.certificados += 1
    uso.horas += c.horasQueContam ?? 0
    mapa.set(c.item.numero, uso)
  }
  return [...mapa.values()].sort((a, b) => b.horas - a.horas || a.numero - b.numero)
}
