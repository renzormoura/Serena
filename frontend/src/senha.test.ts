import { describe, expect, it } from 'vitest'
import { senhaAceita } from './senha'

describe('senhaAceita', () => {
  it('exige 8 caracteres, uma letra e um especial', () => {
    expect(senhaAceita('abcdef-1')).toBe(true)
    expect(senhaAceita('ção!1234')).toBe(true)
    expect(senhaAceita('ab-1')).toBe(false)
    expect(senhaAceita('abcdefgh1')).toBe(false)
    expect(senhaAceita('12345678!')).toBe(false)
    expect(senhaAceita('abcd efg1')).toBe(false)
  })
})
