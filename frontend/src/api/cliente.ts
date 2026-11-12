/** Erro HTTP com o status e, quando houver, o Problem Details (RFC 9457) do backend. */
export class ErroApi extends Error {
  readonly status: number
  readonly detalhe?: ProblemDetails

  constructor(status: number, mensagem: string, detalhe?: ProblemDetails) {
    super(mensagem)
    this.name = 'ErroApi'
    this.status = status
    this.detalhe = detalhe
  }

  /** Código estável do backend (ex.: "arquivo-duplicado"), quando houver. */
  get codigo(): string | undefined {
    return this.detalhe?.codigo
  }

  /** Mensagens por campo em erros de validação (400). */
  get campos(): Record<string, string> {
    return this.detalhe?.campos ?? {}
  }
}

export interface ProblemDetails {
  title?: string
  detail?: string
  status?: number
  codigo?: string
  campos?: Record<string, string>
}

/** Texto para mostrar ao usuário a partir de qualquer erro. */
export function mensagemDeErro(erro: unknown, padrao = 'Algo deu errado. Tente novamente.'): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 413) return 'O arquivo passa de 10 MB.'
    if (erro.detalhe?.detail) return erro.detalhe.detail
    if (erro.status === 401) return 'Sua sessão expirou. Entre de novo.'
    if (erro.status === 403) return 'Você não tem permissão para isso.'
  }
  return padrao
}

const METODOS_SEGUROS = new Set(['GET', 'HEAD', 'OPTIONS'])

export function lerCookie(nome: string, cookies = document.cookie): string | undefined {
  return cookies
    .split('; ')
    .find((par) => par.startsWith(`${nome}=`))
    ?.slice(nome.length + 1)
}

/**
 * fetch com cookie de sessão e CSRF no padrão do Spring Security para SPA:
 * o servidor grava XSRF-TOKEN e espera o mesmo valor no cabeçalho X-XSRF-TOKEN.
 */
export async function requisitar<T>(caminho: string, init: RequestInit = {}): Promise<T> {
  const metodo = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (!METODOS_SEGUROS.has(metodo)) {
    const token = lerCookie('XSRF-TOKEN')
    if (token) headers.set('X-XSRF-TOKEN', decodeURIComponent(token))
  }

  const resposta = await fetch(caminho, { ...init, method: metodo, headers, credentials: 'same-origin' })

  if (!resposta.ok) {
    const tipo = resposta.headers.get('content-type') ?? ''
    const corpo = tipo.includes('json') ? ((await resposta.json()) as ProblemDetails) : undefined
    throw new ErroApi(resposta.status, `${metodo} ${caminho} falhou com ${resposta.status}`, corpo)
  }
  if (resposta.status === 204) return undefined as T
  return (await resposta.json()) as T
}

/** JSON no corpo, com o Content-Type certo. */
export function json(corpo: unknown, metodo = 'POST'): RequestInit {
  return { method: metodo, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(corpo) }
}
