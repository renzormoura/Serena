import { useCallback, useState, type ReactNode } from 'react'
import { ContextoNotificar } from './notificar'

interface Notificacao {
  id: number
  titulo: string
  texto?: string
}

export function ProvedorDeNotificacoes({ children }: { children: ReactNode }) {
  const [lista, setLista] = useState<Notificacao[]>([])
  const notificar = useCallback((titulo: string, texto?: string) => {
    const id = Date.now() + Math.random()
    setLista((atual) => [...atual, { id, titulo, texto }])
    window.setTimeout(() => setLista((atual) => atual.filter((n) => n.id !== id)), 6000)
  }, [])

  return (
    <ContextoNotificar.Provider value={notificar}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed inset-x-4 bottom-4 z-50 flex flex-col items-end gap-2 sm:left-auto">
        {lista.map((n) => (
          <div
            key={n.id}
            role="status"
            className="pointer-events-auto flex w-full max-w-sm gap-3 rounded-xl border border-[#CFE3D7] bg-white px-4 py-3 shadow-lg"
          >
            <span aria-hidden="true" className="mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full bg-ok-fundo text-xs font-bold text-ok">
              ✓
            </span>
            <span className="flex flex-col gap-0.5">
              <span className="text-sm font-semibold text-tinta">{n.titulo}</span>
              {n.texto && <span className="text-[13px] text-apagado">{n.texto}</span>}
            </span>
          </div>
        ))}
      </div>
    </ContextoNotificar.Provider>
  )
}
