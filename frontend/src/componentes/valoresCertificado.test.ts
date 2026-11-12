import { describe, expect, it } from 'vitest'
import { data, horas } from '../formatos'
import { numeroOuNada, paraDados, valoresVazios } from './valoresCertificado'

describe('paraDados', () => {
  it('converte o formulário para o contrato da API', () => {
    expect(
      paraDados({
        ...valoresVazios,
        titulo: '  Monitoria  ',
        horasCertificado: '4,5',
        itemNumero: '18',
        dataInicio: '2024-05-22',
      }),
    ).toEqual({
      titulo: 'Monitoria',
      instituicao: undefined,
      dataInicio: '2024-05-22',
      dataFim: undefined,
      horasCertificado: 4.5,
      itemNumero: 18,
    })
  })
})

describe('numeroOuNada', () => {
  it('aceita vírgula decimal e ignora vazio ou texto', () => {
    expect(numeroOuNada('20')).toBe(20)
    expect(numeroOuNada('4,5')).toBe(4.5)
    expect(numeroOuNada('  ')).toBeUndefined()
    expect(numeroOuNada('abc')).toBeUndefined()
  })
})

describe('formatos', () => {
  it('formata horas e datas em pt-BR', () => {
    expect(horas(20)).toBe('20 h')
    expect(horas(4.5)).toBe('4,5 h')
    expect(horas(null)).toBe('—')
    expect(data('2024-05-22')).toBe('22/05/2024')
  })
})
