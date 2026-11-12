import { afterEach, describe, expect, it, vi } from 'vitest'
import { ErroApi, lerCookie, requisitar } from './cliente'

afterEach(() => {
  vi.unstubAllGlobals()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
})

function respostaJson(status: number, corpo?: unknown) {
  return new Response(corpo === undefined ? null : JSON.stringify(corpo), {
    status,
    headers: { 'content-type': 'application/json' },
  })
}

describe('lerCookie', () => {
  it('encontra o valor pelo nome exato', () => {
    expect(lerCookie('XSRF-TOKEN', 'outro=1; XSRF-TOKEN=abc%3D; X=2')).toBe('abc%3D')
    expect(lerCookie('XSRF', 'XSRF-TOKEN=abc')).toBeUndefined()
  })
})

describe('requisitar', () => {
  it('envia o token CSRF em métodos que alteram estado', async () => {
    document.cookie = 'XSRF-TOKEN=tok%3D'
    const fetchFalso = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchFalso)

    await requisitar('/api/v1/auth/logout', { method: 'post' })

    const [, init] = fetchFalso.mock.calls[0]
    expect(init.method).toBe('POST')
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('tok=')
  })

  it('não envia CSRF em GET', async () => {
    document.cookie = 'XSRF-TOKEN=tok'
    const fetchFalso = vi.fn().mockResolvedValue(respostaJson(200, { ok: true }))
    vi.stubGlobal('fetch', fetchFalso)

    await expect(requisitar('/api/v1/me')).resolves.toEqual({ ok: true })
    expect(new Headers(fetchFalso.mock.calls[0][1].headers).has('X-XSRF-TOKEN')).toBe(false)
  })

  it('transforma respostas de erro em ErroApi com o status', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation(async () => respostaJson(401, { title: 'Unauthorized' })))

    await expect(requisitar('/api/v1/me')).rejects.toMatchObject({ status: 401, detalhe: { title: 'Unauthorized' } })
    await expect(requisitar('/api/v1/me')).rejects.toBeInstanceOf(ErroApi)
  })
})
