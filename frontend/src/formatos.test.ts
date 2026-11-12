import { describe, expect, it } from 'vitest'
import { horas, tempoRelativo } from './formatos'

describe('tempoRelativo', () => {
  const agora = new Date(2026, 10, 11, 9, 0)

  it('conta dias de calendário, não 24 h', () => {
    expect(tempoRelativo(new Date(2026, 10, 11, 0, 5).toISOString(), agora)).toBe('hoje')
    expect(tempoRelativo(new Date(2026, 10, 10, 23, 50).toISOString(), agora)).toBe('ontem')
    expect(tempoRelativo(new Date(2026, 10, 5, 12, 0).toISOString(), agora)).toBe('há 6 dias')
  })

  it('sem data, mostra um traço', () => {
    expect(tempoRelativo(null, agora)).toBe('—')
  })
})

describe('horas', () => {
  it('usa vírgula decimal', () => {
    expect(horas(4.5)).toBe('4,5 h')
  })
})
