import { useState } from 'react'
import { mensagemDeErro } from '../../api/cliente'
import { useDeferirEmLote, useFila } from '../../api/consultas'
import type { ItemDaFila, ResultadoLote } from '../../api/tipos'
import { Alerta, Cartao, classeBotaoPrimario, classeBotaoSecundario } from '../../componentes/ui'
import { horas, tempoRelativo } from '../../formatos'
import { PainelAnalise } from './PainelAnalise'

const rotuloAlerta: Record<ItemDaFila['alertas'][number]['tipo'], string> = {
  NOME_DIFERENTE: 'nome diferente',
  ARQUIVO_DE_OUTRA_CONTA: 'arquivo de outra conta',
  CERTIFICADO_REPETIDO: 'repetido',
  HORAS_DIFERENTES: 'horas diferentes',
  ITEM_TROCADO: 'item trocado',
  ARQUIVO_ILEGIVEL: 'ilegível',
  DATA_SUSPEITA: 'data suspeita',
  HORAS_FORA_DO_COMUM: 'horas incomuns',
  LIMITE_ATINGIDO: 'limite atingido',
  NAO_PARECE_CERTIFICADO: 'não parece certificado',
  PDF_EDITADO: 'PDF editado',
}

const corDoSelo = {
  FORTE: 'bg-bordo-claro text-bordo-escuro',
  ATENCAO: 'bg-alerta-fundo text-alerta',
  INFORMACAO: 'bg-[#F3F0F1] text-tinta-media',
} as const

/** "Sem alertas" ignora os de informação (ex.: PDF editado), que não pedem ação. */
function semAlertas(i: ItemDaFila) {
  return i.alertas.every((a) => a.gravidade === 'INFORMACAO')
}

function LinhaDaFila({
  item,
  ativo,
  marcado,
  aoMarcar,
  aoAbrir,
}: {
  item: ItemDaFila
  ativo: boolean
  marcado: boolean
  aoMarcar: () => void
  aoAbrir: () => void
}) {
  const c = item.certificado
  return (
    <li className={`flex gap-3 border-b border-[#F1ECED] px-4 py-3.5 last:border-b-0 ${ativo ? 'bg-bordo-claro' : 'hover:bg-fundo'}`}>
      <input
        type="checkbox"
        aria-label={`Selecionar ${c.titulo ?? c.arquivo.nome} de ${item.aluno.nome}`}
        checked={marcado}
        onChange={aoMarcar}
        className="mt-1 size-4 shrink-0 accent-[var(--color-bordo)]"
      />
      <button type="button" aria-current={ativo ? 'true' : undefined} onClick={aoAbrir} className="flex min-w-0 flex-1 flex-col gap-1 text-left">
        <span className="truncate text-sm font-semibold">{item.aluno.nome}</span>
        <span className="line-clamp-2 text-[13px] text-tinta-media">{c.titulo ?? c.arquivo.nome}</span>
        <span className="text-xs text-apagado">
          Item {c.item?.numero ?? '—'} · {horas(c.horasQueContam)} · enviado {tempoRelativo(item.enviadoParaAnalise)}
        </span>
        {item.alertas.length > 0 && (
          <span className="flex flex-wrap gap-1 pt-0.5">
            {item.alertas.map((a) => (
              <span key={a.tipo} className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${corDoSelo[a.gravidade]}`}>
                {rotuloAlerta[a.tipo]}
              </span>
            ))}
          </span>
        )}
      </button>
    </li>
  )
}

/**
 * Fila de análise (design aprovado "Coord-Fila"): lista à esquerda, certificado à direita. A triagem automática
 * separa o que tem alerta (topo) do que pode ser validado em lote.
 */
export function Fila() {
  const { data: fila, isPending, isError } = useFila()
  const [escolhido, setEscolhido] = useState<string>()
  const [marcados, setMarcados] = useState<Set<string>>(new Set())
  const [resultadoLote, setResultadoLote] = useState<ResultadoLote[]>()
  const lote = useDeferirEmLote()

  const lista = fila ?? []
  const comAlerta = lista.filter((i) => !semAlertas(i))
  const limpos = lista.filter(semAlertas)
  const ordenada = [...comAlerta, ...limpos]
  const atual = ordenada.find((i) => i.certificado.id === escolhido) ?? ordenada[0]

  function aposDecidir(decididoId: string) {
    const indice = ordenada.findIndex((i) => i.certificado.id === decididoId)
    const proximo = ordenada[indice + 1] ?? ordenada[indice - 1]
    setEscolhido(proximo?.certificado.id)
    setMarcados((m) => {
      const novo = new Set(m)
      novo.delete(decididoId)
      return novo
    })
  }

  function alternar(id: string) {
    setMarcados((m) => {
      const novo = new Set(m)
      if (novo.has(id)) novo.delete(id)
      else novo.add(id)
      return novo
    })
  }

  function validarMarcados() {
    const alvos = ordenada.filter((i) => marcados.has(i.certificado.id)).map((i) => ({ id: i.certificado.id, versao: i.certificado.versao }))
    lote.mutate(alvos, {
      onSuccess: (resultados) => {
        setResultadoLote(resultados)
        setMarcados(new Set(resultados.filter((r) => !r.deferido).map((r) => r.id)))
      },
    })
  }

  const validadosNoLote = resultadoLote?.filter((r) => r.deferido).length ?? 0
  const falhasNoLote = resultadoLote?.filter((r) => !r.deferido) ?? []
  const marcadosComAlerta = comAlerta.filter((i) => marcados.has(i.certificado.id)).length

  const secao = (titulo: string, itens: ItemDaFila[], legenda: string) =>
    itens.length > 0 && (
      <>
        <li className="sticky top-0 z-[1] flex items-baseline justify-between gap-2 border-b border-linha bg-fundo px-4 py-2 text-xs font-semibold text-tinta-media">
          {titulo} · {itens.length}
          <span className="font-normal text-apagado">{legenda}</span>
        </li>
        {itens.map((i) => (
          <LinhaDaFila
            key={i.certificado.id}
            item={i}
            ativo={i.certificado.id === atual?.certificado.id}
            marcado={marcados.has(i.certificado.id)}
            aoMarcar={() => alternar(i.certificado.id)}
            aoAbrir={() => setEscolhido(i.certificado.id)}
          />
        ))}
      </>
    )

  return (
    <div className="flex flex-col gap-7">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-3xl font-semibold">Fila de análise</h1>
          <p className="text-[15px] text-apagado">
            {lista.length === 1 ? '1 certificado' : `${lista.length} certificados`} · {comAlerta.length} com alertas da triagem,{' '}
            {limpos.length} sem alertas
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            type="button"
            className={classeBotaoSecundario}
            disabled={limpos.length === 0}
            onClick={() => setMarcados(new Set(limpos.map((i) => i.certificado.id)))}
          >
            Selecionar os sem alertas
          </button>
          <button type="button" className={classeBotaoPrimario} disabled={marcados.size === 0 || lote.isPending} onClick={validarMarcados}>
            {lote.isPending ? 'Validando…' : marcados.size > 0 ? `Validar selecionados (${marcados.size})` : 'Validar selecionados'}
          </button>
        </div>
      </header>

      {marcadosComAlerta > 0 && (
        <p className="rounded-[10px] bg-alerta-fundo px-4 py-3 text-sm text-[#6B4600]">
          {marcadosComAlerta === 1 ? '1 dos selecionados tem' : `${marcadosComAlerta} dos selecionados têm`} alerta da triagem.
          Confira antes de validar em lote.
        </p>
      )}
      {lote.error && <Alerta>{mensagemDeErro(lote.error)}</Alerta>}
      {resultadoLote && (
        <p role="status" className="rounded-[10px] bg-ok-fundo px-4 py-3 text-sm text-ok">
          {validadosNoLote === 1 ? '1 certificado validado.' : `${validadosNoLote} certificados validados.`}
          {falhasNoLote.length > 0 && (
            <span className="block text-alerta">
              {falhasNoLote.length} não puderam ser validados: {falhasNoLote.map((f) => f.erroMensagem).join(' ')}
            </span>
          )}
        </p>
      )}

      {isError && <Alerta>Não foi possível carregar a fila.</Alerta>}
      {isPending && <p className="text-apagado">Carregando…</p>}

      {fila && lista.length === 0 && (
        <Cartao className="flex flex-col gap-2 p-8">
          <span className="text-lg font-semibold">Nenhum certificado esperando análise</span>
          <span className="text-sm text-apagado">Quando um aluno conferir e salvar um certificado, ele aparece aqui.</span>
        </Cartao>
      )}

      {lista.length > 0 && atual && (
        <div className="flex flex-col items-start gap-6 lg:flex-row">
          <section
            aria-label="Certificados na fila"
            className="w-full overflow-hidden rounded-2xl border border-linha bg-white lg:sticky lg:top-6 lg:w-[340px] lg:shrink-0"
          >
            <ul className="max-h-[calc(100vh-180px)] overflow-y-auto">
              {secao('Com alertas', comAlerta, 'conferir com calma')}
              {secao('Sem alertas', limpos, 'podem ir no lote')}
            </ul>
          </section>

          <section aria-label="Análise do certificado" className="w-full min-w-0 flex-1">
            <PainelAnalise
              id={atual.certificado.id}
              aoDecidir={() => aposDecidir(atual.certificado.id)}
              aoExcluir={() => aposDecidir(atual.certificado.id)}
            />
          </section>
        </div>
      )}
    </div>
  )
}
