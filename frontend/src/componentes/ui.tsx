import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react'
import type { StatusCertificado } from '../api/tipos'
import { estiloStatus, rotuloStatus } from '../formatos'

export const classeBotaoPrimario =
  'inline-flex h-11 items-center justify-center gap-2 rounded-[10px] bg-bordo px-5 text-[15px] font-semibold text-white hover:bg-bordo-escuro disabled:opacity-60'
export const classeBotaoSecundario =
  'inline-flex h-11 items-center justify-center gap-2 rounded-[10px] border border-linha-forte bg-white px-5 text-[15px] font-medium text-tinta hover:bg-fundo disabled:opacity-60'
export const classeEntrada =
  'h-12 w-full min-w-0 rounded-[10px] border border-linha-forte bg-white px-3.5 text-[15px] text-tinta focus:border-bordo aria-invalid:border-bordo'

export function SeloStatus({ status }: { status: StatusCertificado }) {
  return (
    <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold whitespace-nowrap ${estiloStatus[status]}`}>
      {rotuloStatus[status]}
    </span>
  )
}

/** Rótulo + controle + mensagem de erro ligados por id (acessível a leitores de tela). */
export function Campo({
  id,
  rotulo,
  erro,
  dica,
  children,
}: {
  id: string
  rotulo: string
  erro?: string
  dica?: string
  children: ReactNode
}) {
  return (
    <div className="flex min-w-0 flex-col gap-2">
      <label htmlFor={id} className="text-sm font-medium">
        {rotulo}
      </label>
      {children}
      {dica && !erro && (
        <span id={`${id}-dica`} className="text-[13px] text-apagado">
          {dica}
        </span>
      )}
      {erro && (
        <span id={`${id}-erro`} className="text-[13px] text-bordo">
          {erro}
        </span>
      )}
    </div>
  )
}

export function Entrada({ id, erro, ...resto }: InputHTMLAttributes<HTMLInputElement> & { id: string; erro?: string }) {
  return (
    <input
      id={id}
      aria-invalid={erro ? true : undefined}
      aria-describedby={erro ? `${id}-erro` : undefined}
      className={classeEntrada}
      {...resto}
    />
  )
}

export function Selecao({
  id,
  erro,
  children,
  ...resto
}: SelectHTMLAttributes<HTMLSelectElement> & { id: string; erro?: string }) {
  return (
    <select
      id={id}
      aria-invalid={erro ? true : undefined}
      aria-describedby={erro ? `${id}-erro` : undefined}
      className={classeEntrada}
      {...resto}
    >
      {children}
    </select>
  )
}

export function Alerta({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="rounded-[10px] bg-bordo-claro px-4 py-3 text-sm text-bordo-escuro">
      {children}
    </p>
  )
}

export function Cartao({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-2xl border border-linha bg-white ${className}`}>{children}</div>
}
