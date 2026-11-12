import { useState } from 'react'
import { useCatalogo } from '../api/consultas'
import { Cartao } from '../componentes/ui'

function horas(valor: number) {
  return `${Number(valor).toLocaleString('pt-BR')} h`
}

/** Sem acentos e sem caixa, para a busca achar "eletrica" em "Elétrica". */
function normalizar(texto: string) {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

/**
 * Regulamento compacto (pedido da Fase 5.1): uma linha por item, agrupado por macroatividade, com a descrição
 * recolhida. Os 52 itens cabem em poucas telas, e a busca procura em todos.
 */
export function Regulamento() {
  const { data: catalogo, isPending, isError } = useCatalogo()
  const [busca, setBusca] = useState('')
  const [macro, setMacro] = useState<string>('')

  if (isPending) return <p className="text-apagado">Carregando regulamento…</p>
  if (isError || !catalogo) return <p role="alert">Não foi possível carregar o regulamento.</p>

  const termo = normalizar(busca.trim())
  const grupos = catalogo.macroatividades
    .filter((m) => !macro || m.codigo === macro)
    .map((m) => ({
      ...m,
      itens: m.itens.filter((i) => !termo || normalizar(`${i.numero} ${i.nome} ${i.descricao ?? ''}`).includes(termo)),
    }))
    .filter((m) => m.itens.length > 0)
  const encontrados = grupos.reduce((n, g) => n + g.itens.length, 0)

  return (
    <div className="flex flex-col gap-5">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-semibold">Regulamento</h1>
          <p className="text-sm text-apagado">
            {catalogo.nome} · versão {catalogo.versao} ·{' '}
            {catalogo.horasExigidas ? `${horas(catalogo.horasExigidas)} exigidas` : 'total exigido a definir'} · horas que cada
            certificado vale no item
          </p>
        </div>
        <div className="flex w-full flex-wrap gap-2 sm:w-auto">
          <label htmlFor="macro" className="sr-only">
            Macroatividade
          </label>
          <select
            id="macro"
            value={macro}
            onChange={(e) => setMacro(e.target.value)}
            className="h-10 min-w-0 flex-1 rounded-[10px] border border-linha-forte bg-white px-3 text-sm sm:w-56 sm:flex-none"
          >
            <option value="">Todas as macroatividades</option>
            {catalogo.macroatividades.map((m) => (
              <option key={m.codigo} value={m.codigo}>
                {m.nome}
              </option>
            ))}
          </select>
          <label htmlFor="busca" className="sr-only">
            Buscar item
          </label>
          <input
            id="busca"
            type="search"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            placeholder="Buscar item (ex.: monitoria)"
            className="h-10 min-w-0 flex-1 rounded-[10px] border border-linha-forte bg-white px-3.5 text-sm sm:w-64 sm:flex-none"
          />
        </div>
      </header>

      {(termo || macro) && (
        <p className="text-[13px] text-apagado" aria-live="polite">
          {encontrados === 1 ? '1 item encontrado' : `${encontrados} itens encontrados`}
        </p>
      )}

      <div className="flex flex-col gap-4">
        {grupos.map((m) => (
          <Cartao key={m.codigo} className="overflow-hidden">
            <h2 className="flex items-baseline justify-between gap-3 border-b border-linha bg-fundo px-4 py-2.5 text-sm font-semibold">
              {m.nome}
              <span className="text-xs font-normal text-apagado">
                {m.itens.length} {m.itens.length === 1 ? 'item' : 'itens'}
                {m.limiteHoras != null && ` · limite ${horas(m.limiteHoras)}`}
              </span>
            </h2>
            <ul>
              {m.itens.map((item) => (
                <li key={item.numero} className="border-b border-[#F1ECED] last:border-b-0">
                  <details className="group" open={!!termo}>
                    <summary className="flex cursor-pointer list-none items-center gap-3 px-4 py-2 text-sm hover:bg-fundo [&::-webkit-details-marker]:hidden">
                      <span className="w-6 shrink-0 text-right text-xs text-apagado tabular-nums">{item.numero}</span>
                      <span className="min-w-0 flex-1 truncate group-open:whitespace-normal">{item.nome}</span>
                      <span className="shrink-0 rounded-full bg-bordo-claro px-2 py-0.5 text-xs font-semibold text-bordo tabular-nums">
                        {horas(item.horasPorOcorrencia)}
                      </span>
                      <span aria-hidden="true" className="shrink-0 text-apagado transition-transform group-open:rotate-90">
                        ›
                      </span>
                    </summary>
                    <p className="px-4 pb-3 pl-13 text-[13px] leading-relaxed text-apagado">
                      {item.descricao ?? 'Sem descrição no regulamento.'}
                      {(item.limiteOcorrencias != null || item.limiteHoras != null) && (
                        <span className="block pt-1 text-tinta-media">
                          {item.limiteOcorrencias != null && `Até ${item.limiteOcorrencias} vezes. `}
                          {item.limiteHoras != null && `Até ${horas(item.limiteHoras)} no item.`}
                        </span>
                      )}
                    </p>
                  </details>
                </li>
              ))}
            </ul>
          </Cartao>
        ))}
        {grupos.length === 0 && <p className="text-apagado">Nenhum item encontrado.</p>}
      </div>
    </div>
  )
}
