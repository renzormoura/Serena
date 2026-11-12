import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { ErroApi } from '../api/cliente'
import { useEntrar, useUsuarioAtual } from '../api/consultas'
import { Marca } from '../componentes/Marca'

export function Entrar() {
  const { data: usuario } = useUsuarioAtual()
  const entrar = useEntrar()
  const navegar = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')

  if (usuario) return <Navigate to="/" replace />

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    entrar.mutate({ email, senha }, { onSuccess: () => navegar('/') })
  }

  const erro =
    entrar.error instanceof ErroApi && entrar.error.status === 401
      ? 'E-mail, matrícula ou senha incorretos.'
      : entrar.error
        ? 'Não foi possível entrar agora. Tente novamente.'
        : null

  return (
    <div className="flex min-h-screen flex-col lg:flex-row">
      <section className="flex flex-col justify-center gap-12 bg-bordo px-8 py-12 text-white sm:px-16 lg:w-1/2">
        <div className="flex max-w-md flex-col gap-5">
          <p className="text-[13px] tracking-[0.12em] text-[#F1D9DE] uppercase">Engenharias · Universidade de Vassouras</p>
          <h1 className="text-4xl leading-tight font-semibold sm:text-[44px]">
            Suas horas complementares, organizadas sem planilha.
          </h1>
          <p className="text-[17px] leading-relaxed text-[#F6E6E9]">
            Envie o certificado em PDF, confira as horas e a classificação sugeridas, e acompanhe quanto falta para
            integralizar.
          </p>
        </div>
        <p className="text-[13px] text-[#EBCBD1]">Pró-Reitoria de Ciências Tecnológicas e Exatas</p>
      </section>

      <section className="flex flex-1 items-center justify-center bg-white px-6 py-14">
        <form onSubmit={enviar} className="flex w-full max-w-[400px] flex-col gap-7" noValidate>
          <div className="flex justify-center">
            <Marca grande />
          </div>
          <div className="flex flex-col gap-2">
            <h2 className="text-[28px] font-semibold">Entrar</h2>
            <p className="text-[15px] text-apagado">Use o e-mail ou a matrícula do seu cadastro.</p>
          </div>

          <div className="flex flex-col gap-[18px]">
            <div className="flex flex-col gap-2">
              <label htmlFor="email" className="text-sm font-medium">
                E-mail ou matrícula
              </label>
              <input
                id="email"
                type="text"
                autoCapitalize="none"
                spellCheck={false}
                autoComplete="username"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="seu@email.com ou sua matrícula"
                className="h-12 rounded-[10px] border border-linha-forte px-3.5 text-[15px] focus:border-bordo"
              />
            </div>
            <div className="flex flex-col gap-2">
              <label htmlFor="senha" className="text-sm font-medium">
                Senha
              </label>
              <input
                id="senha"
                type="password"
                autoComplete="current-password"
                required
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                className="h-12 rounded-[10px] border border-linha-forte px-3.5 text-[15px] focus:border-bordo"
              />
            </div>
          </div>

          {erro && (
            <p role="alert" className="rounded-[10px] bg-bordo-claro px-4 py-3 text-sm text-bordo-escuro">
              {erro}
            </p>
          )}

          <button
            type="submit"
            disabled={entrar.isPending}
            className="h-[50px] rounded-[10px] bg-bordo text-[15px] font-semibold text-white hover:bg-bordo-escuro disabled:opacity-60"
          >
            {entrar.isPending ? 'Entrando…' : 'Entrar'}
          </button>

          <p className="text-center text-sm text-apagado">
            Primeiro acesso?{' '}
            <Link to="/cadastro" className="font-semibold text-bordo">
              Criar conta
            </Link>
          </p>
        </form>
      </section>
    </div>
  )
}
