import { describe, expect, it } from 'vitest'
import type { Certificado } from '../api/tipos'
import { horasPorItem, horasPorSemestre, semestreDe } from './agregacoesAluno'

function cert(parcial: Partial<Certificado>): Certificado {
  return {
    status: 'DEFERIDO',
    dataInicio: null,
    dataFim: null,
    criadoEm: '2026-11-01T12:00:00Z',
    horasQueContam: 10,
    item: { numero: 18, nome: 'Monitoria', horasPorOcorrencia: 50, macroCodigo: 'X', macroNome: 'X' },
    ...parcial,
  } as Certificado
}

describe('semestreDe', () => {
  it('junho é do 1º semestre, julho do 2º', () => {
    expect(semestreDe('2025-06-30')).toBe('2025.1')
    expect(semestreDe('2025-07-01')).toBe('2025.2')
  })
})

describe('horasPorSemestre', () => {
  it('separa deferido de em análise e preenche semestres vazios', () => {
    const pontos = horasPorSemestre([
      cert({ dataInicio: '2024-03-10', horasQueContam: 50 }),
      cert({ dataInicio: '2025-08-01', status: 'CONFIRMADO', horasQueContam: 10 }),
      cert({ dataFim: '2025-09-01', horasQueContam: 5 }),
      cert({ status: 'INDEFERIDO', dataInicio: '2023-01-01' }),
      cert({ status: 'AGUARDANDO_CONFIRMACAO', dataInicio: '2023-01-01' }),
    ])
    expect(pontos.map((p) => p.rotulo)).toEqual(['2024.1', '2024.2', '2025.1', '2025.2'])
    expect(pontos[0]).toEqual({ rotulo: '2024.1', deferidas: 50, emAnalise: 0 })
    expect(pontos[3]).toEqual({ rotulo: '2025.2', deferidas: 5, emAnalise: 10 })
  })

  it('sem data da atividade, usa a do envio', () => {
    expect(horasPorSemestre([cert({})]).map((p) => p.rotulo)).toEqual(['2026.2'])
  })
})

describe('horasPorItem', () => {
  it('soma por item, maior primeiro, ignorando indeferidos', () => {
    const itens = horasPorItem([
      cert({ horasQueContam: 50 }),
      cert({ horasQueContam: 50 }),
      cert({ item: { numero: 21, nome: 'Oficinas', horasPorOcorrencia: 10, macroCodigo: 'X', macroNome: 'X' }, horasQueContam: 10 }),
      cert({ status: 'INDEFERIDO', horasQueContam: 99 }),
    ])
    expect(itens).toEqual([
      { numero: 18, nome: 'Monitoria', certificados: 2, horas: 100 },
      { numero: 21, nome: 'Oficinas', certificados: 1, horas: 10 },
    ])
  })
})
