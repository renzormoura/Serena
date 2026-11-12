import { useId, useState, type ReactNode } from 'react'

/**
 * Gráficos simples em SVG/HTML, sem biblioteca. Cores de dados validadas com o validador de paleta (dataviz):
 * #a3304a e #d9849a passam em todos os checks; o tom claro tem contraste < 3:1, então todo gráfico tem rótulo
 * direto ou tabela. Marcas finas, topo arredondado de 4 px, 2 px de respiro entre barras, dica no hover e no foco.
 */
export const COR_DADO = '#a3304a'
export const COR_DADO_CLARO = '#d9849a'

interface Dica {
  x: number
  y: number
  titulo: string
  linhas: { cor?: string; rotulo: string; valor: string }[]
}

/** A dica flutuante: valor em destaque, nome da série depois. Texto sempre como texto (nunca HTML). */
function CaixaDica({ dica }: { dica: Dica | null }) {
  if (!dica) return null
  return (
    <div
      role="status"
      className="pointer-events-none absolute z-10 min-w-36 -translate-x-1/2 -translate-y-full rounded-lg border border-linha bg-white px-3 py-2 text-xs shadow-sm"
      style={{ left: dica.x, top: dica.y - 8 }}
    >
      <p className="mb-1 font-medium text-apagado">{dica.titulo}</p>
      {dica.linhas.map((l) => (
        <p key={l.rotulo} className="flex items-center gap-2">
          {l.cor && <span className="inline-block h-0.5 w-3 rounded" style={{ background: l.cor }} />}
          <span className="font-semibold text-tinta">{l.valor}</span>
          <span className="text-apagado">{l.rotulo}</span>
        </p>
      ))}
    </div>
  )
}

/** Colunas verticais de uma série (sem legenda: o título nomeia). Valor escrito sobre cada coluna. */
export function Colunas({
  dados,
  formatar,
  rotuloSerie,
  altura = 200,
}: {
  dados: { rotulo: string; valor: number }[]
  formatar: (v: number) => string
  rotuloSerie: string
  altura?: number
}) {
  const [dica, setDica] = useState<Dica | null>(null)
  const maximo = Math.max(1, ...dados.map((d) => d.valor))
  const largura = 100 / Math.max(1, dados.length)

  return (
    <div className="relative" onPointerLeave={() => setDica(null)}>
      <div className="flex items-end gap-0.5 border-b border-linha-forte" style={{ height: altura }}>
        {dados.map((d) => {
          const h = (d.valor / maximo) * (altura - 24)
          const mostrar = (alvo: HTMLElement) => {
            const caixa = alvo.parentElement!.parentElement!.getBoundingClientRect()
            const r = alvo.getBoundingClientRect()
            setDica({ x: r.left - caixa.left + r.width / 2, y: (alvo.lastElementChild ?? alvo).getBoundingClientRect().top - caixa.top, titulo: d.rotulo, linhas: [{ rotulo: rotuloSerie, valor: formatar(d.valor) }] })
          }
          return (
            <div
              key={d.rotulo}
              tabIndex={0}
              aria-label={`${d.rotulo}: ${formatar(d.valor)}`}
              className="group flex h-full flex-col items-center justify-end gap-1 px-2 outline-none"
              style={{ width: `${largura}%` }}
              onPointerEnter={(e) => mostrar(e.currentTarget)}
              onFocus={(e) => mostrar(e.currentTarget)}
              onBlur={() => setDica(null)}
            >
              <span className="text-xs font-semibold text-tinta tabular-nums">{formatar(d.valor)}</span>
              <div
                className="w-full max-w-16 rounded-t-[4px] transition-opacity group-hover:opacity-80 group-focus-visible:outline-2 group-focus-visible:outline-bordo"
                style={{ height: Math.max(d.valor > 0 ? 2 : 0, h), background: COR_DADO }}
              />
            </div>
          )
        })}
      </div>
      <div className="flex gap-0.5 pt-2">
        {dados.map((d) => (
          <span key={d.rotulo} className="px-1 text-center text-xs text-apagado" style={{ width: `${largura}%` }}>
            {d.rotulo}
          </span>
        ))}
      </div>
      <CaixaDica dica={dica} />
    </div>
  )
}

/** Barras horizontais de uma série, com o valor no fim de cada barra. Bom para nomes longos. */
export function Barras({ dados, formatar }: { dados: { rotulo: string; valor: number }[]; formatar: (v: number) => string }) {
  const maximo = Math.max(1, ...dados.map((d) => d.valor))
  return (
    <ul className="flex flex-col gap-3.5">
      {dados.map((d) => (
        <li key={d.rotulo} className="flex flex-col gap-1.5" title={`${d.rotulo}: ${formatar(d.valor)}`}>
          <div className="flex justify-between gap-3 text-sm">
            <span className="min-w-0 truncate">{d.rotulo}</span>
            <span className="shrink-0 font-semibold tabular-nums">{formatar(d.valor)}</span>
          </div>
          <div className="h-1.5 rounded-full bg-bordo-trilho">
            <div className="h-1.5 rounded-full" style={{ width: `${(d.valor / maximo) * 100}%`, background: COR_DADO }} />
          </div>
        </li>
      ))}
    </ul>
  )
}

export interface Serie {
  chave: string
  rotulo: string
  cor: string
}

/**
 * Colunas empilhadas (ex.: horas deferidas + em análise por semestre). Legenda sempre visível, total escrito
 * sobre cada coluna, dica com todas as séries, e uma tabela com os mesmos números.
 */
export function ColunasEmpilhadas({
  dados,
  series,
  formatar,
  altura = 200,
  rotuloCategoria,
}: {
  dados: ({ rotulo: string } & Record<string, number | string>)[]
  series: Serie[]
  formatar: (v: number) => string
  altura?: number
  rotuloCategoria: string
}) {
  const [dica, setDica] = useState<Dica | null>(null)
  const [tabela, setTabela] = useState(false)
  const idTabela = useId()
  const valor = (d: (typeof dados)[number], s: Serie) => Number(d[s.chave] ?? 0)
  const total = (d: (typeof dados)[number]) => series.reduce((soma, s) => soma + valor(d, s), 0)
  const maximo = Math.max(1, ...dados.map(total))
  const largura = 100 / Math.max(1, dados.length)

  let corpo: ReactNode
  if (tabela) {
    corpo = (
      <table id={idTabela} className="w-full text-left text-sm">
        <thead className="text-xs text-apagado">
          <tr>
            <th scope="col" className="py-2 font-medium">{rotuloCategoria}</th>
            {series.map((s) => (
              <th key={s.chave} scope="col" className="py-2 text-right font-medium">{s.rotulo}</th>
            ))}
            <th scope="col" className="py-2 text-right font-medium">Total</th>
          </tr>
        </thead>
        <tbody>
          {dados.map((d) => (
            <tr key={d.rotulo} className="border-t border-linha">
              <th scope="row" className="py-2 font-medium">{d.rotulo}</th>
              {series.map((s) => (
                <td key={s.chave} className="py-2 text-right tabular-nums">{formatar(valor(d, s))}</td>
              ))}
              <td className="py-2 text-right font-semibold tabular-nums">{formatar(total(d))}</td>
            </tr>
          ))}
        </tbody>
      </table>
    )
  } else {
    corpo = (
      <div className="relative" onPointerLeave={() => setDica(null)}>
        <div className="flex items-end gap-0.5 border-b border-linha-forte" style={{ height: altura }}>
          {dados.map((d) => {
            const mostrar = (alvo: HTMLElement) => {
              const caixa = alvo.parentElement!.parentElement!.getBoundingClientRect()
              const r = alvo.getBoundingClientRect()
              setDica({
                x: r.left - caixa.left + r.width / 2,
                y: (alvo.lastElementChild ?? alvo).getBoundingClientRect().top - caixa.top,
                titulo: d.rotulo,
                linhas: series.map((s) => ({ cor: s.cor, rotulo: s.rotulo, valor: formatar(valor(d, s)) })),
              })
            }
            const visiveis = series.filter((s) => valor(d, s) > 0)
            return (
              <div
                key={d.rotulo}
                tabIndex={0}
                aria-label={`${d.rotulo}: ${series.map((s) => `${s.rotulo} ${formatar(valor(d, s))}`).join(', ')}`}
                className="group flex h-full flex-col items-center justify-end px-2 outline-none"
                style={{ width: `${largura}%` }}
                onPointerEnter={(e) => mostrar(e.currentTarget)}
                onFocus={(e) => mostrar(e.currentTarget)}
                onBlur={() => setDica(null)}
              >
                <span className="mb-1 text-xs font-semibold text-tinta tabular-nums">{formatar(total(d))}</span>
                {/* De cima para baixo: a última série fica no topo; a primeira apoiada na base. */}
                <div className="flex w-full max-w-14 flex-col gap-0.5 group-hover:opacity-85 group-focus-visible:outline-2 group-focus-visible:outline-bordo">
                  {[...visiveis].reverse().map((s, i) => (
                    <div
                      key={s.chave}
                      className={i === 0 ? 'rounded-t-[4px]' : ''}
                      style={{ height: (valor(d, s) / maximo) * (altura - 24), background: s.cor }}
                    />
                  ))}
                </div>
              </div>
            )
          })}
        </div>
        <div className="flex gap-0.5 pt-2">
          {dados.map((d) => (
            <span key={d.rotulo} className="px-1 text-center text-xs text-apagado" style={{ width: `${largura}%` }}>
              {d.rotulo}
            </span>
          ))}
        </div>
        <CaixaDica dica={dica} />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <ul className="flex flex-wrap gap-4 text-xs text-tinta-media" aria-label="Legenda">
          {series.map((s) => (
            <li key={s.chave} className="flex items-center gap-1.5">
              <span className="inline-block size-2.5 rounded-sm" style={{ background: s.cor }} />
              {s.rotulo}
            </li>
          ))}
        </ul>
        <button
          type="button"
          aria-pressed={tabela}
          aria-controls={idTabela}
          className="text-xs font-semibold text-bordo hover:text-bordo-escuro"
          onClick={() => setTabela((t) => !t)}
        >
          {tabela ? 'Ver gráfico' : 'Ver em tabela'}
        </button>
      </div>
      {corpo}
    </div>
  )
}
