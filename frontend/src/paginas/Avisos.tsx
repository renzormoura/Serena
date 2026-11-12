import { useEffect } from 'react'
import { Link } from 'react-router'
import { useAvisos, useMarcarAvisosLidos } from '../api/consultas'
import { Alerta, Cartao } from '../componentes/ui'
import { dataHora } from '../formatos'

/** Resultado das análises da coordenação (validado, reprovado, ajuste pedido), com a mensagem de quem analisou. */
export function Avisos() {
  const { data: caixa, isPending, isError } = useAvisos()
  const marcar = useMarcarAvisosLidos()
  const naoLidos = caixa?.naoLidos ?? 0

  // Abrir a página conta como ler. Os novos continuam destacados até sair da página.
  useEffect(() => {
    if (naoLidos > 0 && !marcar.isPending && !marcar.isSuccess) marcar.mutate()
  }, [naoLidos, marcar])

  return (
    <div className="flex flex-col gap-7">
      <header className="flex flex-col gap-1.5">
        <h1 className="text-3xl font-semibold">Avisos</h1>
        <p className="text-[15px] text-apagado">O resultado de cada análise da coordenação, com a mensagem de quem analisou.</p>
      </header>
      {isError && <Alerta>Não foi possível carregar os avisos.</Alerta>}
      {isPending && <p className="text-apagado">Carregando…</p>}
      {caixa && caixa.avisos.length === 0 && (
        <Cartao className="p-8 text-sm text-apagado">Nenhum aviso ainda. Quando a coordenação analisar um certificado, ele aparece aqui.</Cartao>
      )}
      {caixa && caixa.avisos.length > 0 && (
        <ul className="flex flex-col gap-3">
          {caixa.avisos.map((a) => {
            const tom = a.titulo.includes('reprovado') ? 'border-l-bordo' : a.titulo.includes('Ajuste') ? 'border-l-alerta' : 'border-l-ok'
            const conteudo = (
              <>
                <span className="flex flex-wrap items-baseline justify-between gap-2">
                  <span className="text-[15px] font-semibold">
                    {a.titulo}
                    {!a.lido && <span className="ml-2 rounded-full bg-bordo px-2 py-0.5 text-[11px] font-semibold text-white">novo</span>}
                  </span>
                  <span className="text-xs text-apagado">{dataHora(a.criadoEm)}</span>
                </span>
                <span className="text-sm text-tinta-media">{a.mensagem}</span>
              </>
            )
            return (
              <li key={a.id}>
                {a.link ? (
                  <Link to={a.link} className={`flex flex-col gap-1.5 rounded-2xl border border-l-4 border-linha bg-white px-5 py-4 hover:bg-fundo ${tom}`}>
                    {conteudo}
                  </Link>
                ) : (
                  <div className={`flex flex-col gap-1.5 rounded-2xl border border-l-4 border-linha bg-white px-5 py-4 ${tom}`}>{conteudo}</div>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
