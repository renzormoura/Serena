import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { ErroApi, mensagemDeErro } from '../../api/cliente'
import { useAnalise, useCatalogo, useDecidir, useExcluirPelaCoordenacao, useSimulacaoCoordenacao } from '../../api/consultas'
import { useNotificar } from '../../componentes/notificar'
import type { Alerta as AlertaTriagem, Analise, Decisao } from '../../api/tipos'
import { PreviaArquivo } from '../../componentes/PreviaArquivo'
import { Alerta, Cartao, classeBotaoPrimario, classeBotaoSecundario, classeEntrada, Entrada, SeloStatus, Selecao } from '../../componentes/ui'
import { numeroOuNada } from '../../componentes/valoresCertificado'
import { data, dataHora, horas, rotuloDecisao } from '../../formatos'

const rotuloConfianca = { ALTA: 'confiança alta', MEDIA: 'confiança média', BAIXA: 'confiança baixa' } as const

/** Teclas de atalho da fila: só valem fora de campos de texto. V valida, A pede ajuste, R reprova. */
const ATALHOS: Record<string, Decisao> = { v: 'DEFERIR', a: 'SOLICITAR_AJUSTE', r: 'INDEFERIR' }

const estiloGravidade: Record<AlertaTriagem['gravidade'], { caixa: string; rotulo: string }> = {
  FORTE: { caixa: 'border-bordo/30 bg-bordo-claro text-bordo-escuro', rotulo: 'Atenção forte' },
  ATENCAO: { caixa: 'border-[#F0E2C4] bg-alerta-fundo text-[#6B4600]', rotulo: 'Conferir' },
  INFORMACAO: { caixa: 'border-linha bg-fundo text-tinta-media', rotulo: 'Informação' },
}

/** Triagem automática: o que conferir antes de validar. Nunca decide sozinha. */
export function ListaDeAlertas({ alertas }: { alertas: AlertaTriagem[] }) {
  if (alertas.length === 0) {
    return (
      <p className="rounded-[10px] bg-ok-fundo px-4 py-3 text-sm text-ok">
        Triagem automática: nenhum alerta. Os dados batem com o arquivo.
      </p>
    )
  }
  return (
    <ul className="flex flex-col gap-2" aria-label="Alertas da triagem automática">
      {alertas.map((a) => (
        <li key={a.tipo} className={`flex flex-col gap-0.5 rounded-[10px] border px-4 py-2.5 text-sm ${estiloGravidade[a.gravidade].caixa}`}>
          <span className="text-[11px] font-semibold tracking-wide uppercase">{estiloGravidade[a.gravidade].rotulo}</span>
          <span>{a.mensagem}</span>
        </li>
      ))}
    </ul>
  )
}

function Dado({ rotulo, children, tom }: { rotulo: string; children: React.ReactNode; tom?: 'ok' | 'alerta' }) {
  return (
    <div className="flex flex-col gap-1">
      <dt className="text-xs text-apagado">{rotulo}</dt>
      <dd className={`text-sm font-medium ${tom === 'ok' ? 'text-ok' : tom === 'alerta' ? 'text-alerta' : ''}`}>{children}</dd>
    </div>
  )
}

function Historico({ historico }: { historico: Analise['historico'] }) {
  if (historico.length === 0) return null
  return (
    <Cartao className="flex flex-col gap-3 p-6">
      <h3 className="text-base font-semibold">Histórico</h3>
      <ol className="flex flex-col gap-3">
        {historico.map((h) => (
          <li key={h.criadoEm} className="flex flex-col gap-0.5 border-l-2 border-bordo-trilho pl-3 text-sm">
            <span>
              <span className="font-semibold">{rotuloDecisao[h.decisao]}</span>
              <span className="text-apagado">
                {' '}
                por {h.avaliador} em {dataHora(h.criadoEm)}
              </span>
            </span>
            {h.itemNovo != null && (
              <span className="text-apagado">
                Item {h.itemAnterior} → {h.itemNovo}
              </span>
            )}
            {h.horasNovas != null && (
              <span className="text-apagado">
                Horas {horas(h.horasAnteriores)} → {horas(h.horasNovas)}
              </span>
            )}
            {h.justificativa && <span>“{h.justificativa}”</span>}
          </li>
        ))}
      </ol>
    </Cartao>
  )
}

/**
 * Análise de um certificado pela coordenação: arquivo, dados conferidos pelo aluno, sugestão do sistema e as
 * decisões (validar, pedir ajuste, reprovar, reabrir). Usado na fila e na página própria do certificado.
 */
export function PainelAnalise({
  id,
  aoDecidir,
  aoExcluir,
}: {
  id: string
  aoDecidir?: (analise: Analise) => void
  /** Depois de excluir: a fila vai para o próximo; a página do certificado volta para a ficha do aluno. */
  aoExcluir?: (alunoId: string) => void
}) {
  const { data: analise, isPending, isError } = useAnalise(id)

  if (isPending) return <Cartao className="p-8 text-apagado">Carregando…</Cartao>
  if (isError || !analise) return <Alerta>Não foi possível abrir este certificado.</Alerta>
  return (
    <Conteudo key={`${analise.certificado.id}-${analise.certificado.versao}`} analise={analise} aoDecidir={aoDecidir} aoExcluir={aoExcluir} />
  )
}

function Conteudo({
  analise,
  aoDecidir,
  aoExcluir,
}: {
  analise: Analise
  aoDecidir?: (analise: Analise) => void
  aoExcluir?: (alunoId: string) => void
}) {
  const { certificado: c, aluno, saldoAluno } = analise
  const decidir = useDecidir(c.id)
  const { data: catalogo } = useCatalogo()
  const [observacao, setObservacao] = useState('')
  const [erroObservacao, setErroObservacao] = useState<string>()
  const [item, setItem] = useState(c.item ? String(c.item.numero) : '')
  const [horasCorrigidas, setHorasCorrigidas] = useState(c.horasCertificado != null ? String(c.horasCertificado).replace('.', ',') : '')
  const campoObservacao = useRef<HTMLTextAreaElement>(null)

  const emAnalise = c.status === 'CONFIRMADO'
  const itemNumero = item ? Number(item) : undefined
  const horasNumero = numeroOuNada(horasCorrigidas)
  const mudouItem = itemNumero != null && itemNumero !== c.item?.numero
  const mudouHoras = horasNumero != null && horasNumero !== c.horasCertificado
  const simulacao = useSimulacaoCoordenacao(c.id, mudouItem || mudouHoras ? { itemNumero, horasCertificado: horasNumero } : {})

  function enviar(decisao: Decisao) {
    const texto = observacao.trim()
    const exige = decisao !== 'DEFERIR' || mudouItem || mudouHoras
    if (exige && !texto) {
      setErroObservacao(
        decisao === 'DEFERIR'
          ? 'Explique ao aluno por que o item ou as horas mudaram.'
          : 'Explique ao aluno o motivo e o que ficou faltando.',
      )
      campoObservacao.current?.focus()
      return
    }
    setErroObservacao(undefined)
    decidir.mutate(
      {
        versao: c.versao,
        decisao,
        justificativa: texto || undefined,
        itemNumero: decisao === 'DEFERIR' && mudouItem ? itemNumero : undefined,
        horasCertificado: decisao === 'DEFERIR' && mudouHoras ? horasNumero : undefined,
      },
      { onSuccess: (nova) => aoDecidir?.(nova) },
    )
  }

  // Atalhos D, A, I (o design da fila). Ignorados enquanto se digita.
  const enviarRef = useRef(enviar)
  useEffect(() => {
    enviarRef.current = enviar
  })
  useEffect(() => {
    if (!emAnalise) return
    function aoTeclar(e: KeyboardEvent) {
      const alvo = e.target as HTMLElement
      if (e.ctrlKey || e.metaKey || e.altKey || alvo.closest('input, textarea, select, [contenteditable]')) return
      const decisao = ATALHOS[e.key.toLowerCase()]
      if (decisao) {
        e.preventDefault()
        enviarRef.current(decisao)
      }
    }
    window.addEventListener('keydown', aoTeclar)
    return () => window.removeEventListener('keydown', aoTeclar)
  }, [emAnalise])

  const erroServidor = decidir.error instanceof ErroApi ? decidir.error : undefined

  return (
    <div className="flex flex-col gap-5">
      <Cartao className="flex flex-col gap-6 p-6 sm:p-7">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex min-w-0 flex-col gap-1">
            <span className="text-[13px] text-apagado">
              <Link to={`/coordenacao/alunos/${aluno.id}`} className="font-medium text-tinta-media hover:text-bordo">
                {aluno.nome}
              </Link>{' '}
              · Matrícula {aluno.matricula} · {aluno.curso}
            </span>
            <h2 className="text-[22px] font-semibold">{c.titulo ?? c.arquivo.nome}</h2>
          </div>
          <SeloStatus status={c.status} />
        </div>

        {(c.status === 'CONFIRMADO' || analise.alertas.length > 0) && <ListaDeAlertas alertas={analise.alertas} />}

        <div className="flex flex-col gap-6 2xl:flex-row">
          <div className="min-w-0 2xl:flex-1">
            <PreviaArquivo url={`/api/v1/coordenacao/certificados/${c.id}/arquivo`} tipo={c.arquivo.tipoMime} nome={c.arquivo.nome} />
          </div>
          <div className="flex min-w-0 flex-col gap-5 2xl:w-[340px] 2xl:shrink-0">
            <dl className="grid grid-cols-2 gap-x-6 gap-y-4">
              <Dado rotulo="Instituição">{c.instituicao ?? '—'}</Dado>
              <Dado rotulo="Período">
                {data(c.dataInicio)}
                {c.dataFim && c.dataFim !== c.dataInicio ? ` – ${data(c.dataFim)}` : ''}
              </Dado>
              <Dado rotulo="Horas no certificado">{horas(c.horasCertificado)}</Dado>
              <Dado
                rotulo="Nome no certificado"
                tom={c.leitura?.nomeConfere === true ? 'ok' : c.leitura?.nomeConfere === false ? 'alerta' : undefined}
              >
                {c.leitura?.nomeConfere === true ? 'Confere' : c.leitura?.nomeConfere === false ? 'Não encontrado' : 'Não verificado'}
              </Dado>
            </dl>

            <div className="flex flex-col gap-3">
              <div className="flex flex-col gap-1 rounded-xl bg-fundo px-4 py-3">
                <span className="text-xs text-apagado">Sugestão do sistema</span>
                <span className="text-sm font-semibold">
                  {c.sugestaoItem
                    ? `${c.sugestaoItem.item.numero} · ${c.sugestaoItem.item.nome} (${rotuloConfianca[c.sugestaoItem.confianca]})`
                    : 'Sem sugestão (lançamento manual ou texto não lido)'}
                </span>
              </div>
              <div className="flex flex-col gap-1 rounded-xl bg-bordo-claro px-4 py-3">
                <span className="text-xs text-bordo-escuro">Escolhido pelo aluno</span>
                <span className="text-sm font-semibold">
                  {c.item ? `${c.item.numero} · ${c.item.nome}` : '—'}
                  {c.horasQueContam != null && ` · vale ${horas(c.horasQueContam)}`}
                </span>
              </div>
              {c.cortes.length > 0 && (
                <ul className="text-[13px] text-alerta">
                  {c.cortes.map((corte) => (
                    <li key={corte}>{corte}</li>
                  ))}
                </ul>
              )}
              {c.observacaoAluno && (
                <div className="flex flex-col gap-1 rounded-xl border border-linha px-4 py-3">
                  <span className="text-xs text-apagado">Observação do aluno</span>
                  <span className="text-sm">“{c.observacaoAluno}”</span>
                </div>
              )}
              <p className="text-[13px] text-apagado">
                O aluno tem {horas(saldoAluno.integralizadas)} validadas e {horas(saldoAluno.pendentes)} em análise.
              </p>
            </div>
          </div>
        </div>

        {emAnalise && (
          <div className="flex flex-col gap-5 border-t border-linha pt-5">
            <div className="flex flex-col gap-3">
              <div className="flex flex-col gap-0.5">
                <span className="text-sm font-medium">Item e carga horária</span>
                <span className="text-[13px] text-apagado">Já vêm como o aluno informou. Corrija só se precisar; a mudança exige explicação.</span>
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
                <div className="flex flex-col gap-2">
                  <label htmlFor="item-corrigido" className="text-xs text-apagado">
                    Item
                  </label>
                  <Selecao id="item-corrigido" value={item} onChange={(e) => setItem(e.target.value)}>
                    {catalogo?.macroatividades.map((m) => (
                      <optgroup key={m.codigo} label={m.nome}>
                        {m.itens.map((i) => (
                          <option key={i.numero} value={i.numero}>
                            {i.numero} · {i.nome} ({horas(i.horasPorOcorrencia)})
                          </option>
                        ))}
                      </optgroup>
                    ))}
                  </Selecao>
                </div>
                <div className="flex flex-col gap-2">
                  <label htmlFor="horas-corrigidas" className="text-xs text-apagado">
                    Horas no certificado
                  </label>
                  <Entrada id="horas-corrigidas" inputMode="decimal" value={horasCorrigidas} onChange={(e) => setHorasCorrigidas(e.target.value)} />
                </div>
                {(mudouItem || mudouHoras) && (
                  <p className="text-[13px] text-alerta sm:col-span-2">
                    Você alterou {mudouItem && mudouHoras ? 'o item e as horas' : mudouItem ? 'o item' : 'as horas'}
                    {simulacao.data && (
                      <>
                        : vai contar <strong>{horas(simulacao.data.horasQueContam)}</strong>
                      </>
                    )}
                    . Explique ao aluno na observação.
                  </p>
                )}
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="observacao" className="text-sm font-medium">
                Mensagem para o aluno{' '}
                <span className="font-normal text-apagado">(obrigatória para reprovar, pedir ajuste ou corrigir: diga o motivo e o que faltou)</span>
              </label>
              <textarea
                id="observacao"
                ref={campoObservacao}
                rows={3}
                maxLength={1000}
                value={observacao}
                onChange={(e) => setObservacao(e.target.value)}
                aria-invalid={erroObservacao ? true : undefined}
                aria-describedby={erroObservacao ? 'observacao-erro' : undefined}
                className={`${classeEntrada} h-auto py-3`}
              />
              {erroObservacao && (
                <span id="observacao-erro" className="text-[13px] text-bordo">
                  {erroObservacao}
                </span>
              )}
            </div>

            {decidir.error && (
              <Alerta>
                {erroServidor?.codigo === 'versao'
                  ? 'Este certificado mudou desde que você abriu (o aluno pode ter editado). Recarregue a página.'
                  : mensagemDeErro(decidir.error)}
              </Alerta>
            )}

            <div className="flex flex-wrap items-center gap-3">
              <button type="button" className={classeBotaoPrimario} disabled={decidir.isPending} onClick={() => enviar('DEFERIR')}>
                Validar
              </button>
              <button type="button" className={classeBotaoSecundario} disabled={decidir.isPending} onClick={() => enviar('SOLICITAR_AJUSTE')}>
                Pedir ajuste
              </button>
              <button
                type="button"
                className={`${classeBotaoSecundario} text-bordo`}
                disabled={decidir.isPending}
                onClick={() => enviar('INDEFERIR')}
              >
                Reprovar
              </button>
              <span className="ml-auto hidden text-xs text-apagado sm:inline">
                Atalhos: <kbd>V</kbd> validar · <kbd>A</kbd> ajuste · <kbd>R</kbd> reprovar
              </span>
            </div>
          </div>
        )}

        {(c.status === 'DEFERIDO' || c.status === 'INDEFERIDO') && (
          <Reabrir
            pendente={decidir.isPending}
            erro={decidir.error}
            aoReabrir={(justificativa) => decidir.mutate({ versao: c.versao, decisao: 'REABRIR', justificativa })}
          />
        )}

        {(c.status === 'AJUSTE_SOLICITADO' || c.status === 'AGUARDANDO_CONFIRMACAO' || c.status === 'PROCESSANDO') && (
          <p className="rounded-[10px] bg-fundo px-4 py-3 text-sm text-apagado">
            {c.status === 'AJUSTE_SOLICITADO'
              ? 'Esperando o aluno corrigir. Quando ele salvar, o certificado volta para a fila.'
              : 'O aluno ainda não conferiu este certificado.'}
          </p>
        )}
      </Cartao>

      <Historico historico={analise.historico} />

      <Excluir certificadoId={c.id} versao={c.versao} titulo={c.titulo ?? c.arquivo.nome} aoExcluir={() => aoExcluir?.(aluno.id)} />
    </div>
  )
}

function Reabrir({ pendente, erro, aoReabrir }: { pendente: boolean; erro: unknown; aoReabrir: (justificativa: string) => void }) {
  const [aberto, setAberto] = useState(false)
  const [motivo, setMotivo] = useState('')
  if (!aberto) {
    return (
      <div className="border-t border-linha pt-5">
        <button type="button" className={classeBotaoSecundario} onClick={() => setAberto(true)}>
          Reabrir para nova análise
        </button>
      </div>
    )
  }
  return (
    <form
      className="flex flex-col gap-3 border-t border-linha pt-5"
      onSubmit={(e) => {
        e.preventDefault()
        if (motivo.trim()) aoReabrir(motivo.trim())
      }}
    >
      <label htmlFor="motivo-reabrir" className="text-sm font-medium">
        Por que reabrir?
      </label>
      <textarea
        id="motivo-reabrir"
        rows={2}
        required
        maxLength={1000}
        value={motivo}
        onChange={(e) => setMotivo(e.target.value)}
        className={`${classeEntrada} h-auto py-3`}
      />
      {erro != null && <Alerta>{mensagemDeErro(erro)}</Alerta>}
      <div className="flex gap-3">
        <button type="submit" className={classeBotaoPrimario} disabled={pendente || !motivo.trim()}>
          Reabrir
        </button>
        <button type="button" className={classeBotaoSecundario} onClick={() => setAberto(false)}>
          Cancelar
        </button>
      </div>
    </form>
  )
}

/**
 * Exclusão pela coordenação (ex.: enviado por engano, documento inválido, duplicado). Pede o motivo, que vai para
 * o aluno. O certificado some da conta e do saldo do aluno; o histórico fica guardado.
 */
function Excluir({ certificadoId, versao, titulo, aoExcluir }: { certificadoId: string; versao: number; titulo: string; aoExcluir: () => void }) {
  const [aberto, setAberto] = useState(false)
  const [motivo, setMotivo] = useState('')
  const excluir = useExcluirPelaCoordenacao(certificadoId)
  const notificar = useNotificar()

  if (!aberto) {
    return (
      <div className="flex justify-end">
        <button type="button" className="text-sm font-semibold text-bordo hover:text-bordo-escuro" onClick={() => setAberto(true)}>
          Excluir certificado do aluno
        </button>
      </div>
    )
  }
  return (
    <Cartao className="flex flex-col gap-3 border-bordo/30 p-6">
      <h3 className="text-base font-semibold text-bordo-escuro">Excluir “{titulo}”?</h3>
      <p className="text-sm text-tinta-media">
        O certificado sai da lista e do saldo do aluno, mesmo se já estiver validado. O aluno recebe o motivo por aviso e e-mail. O
        histórico fica guardado.
      </p>
      <form
        className="flex flex-col gap-3"
        onSubmit={(e) => {
          e.preventDefault()
          if (!motivo.trim()) return
          excluir.mutate(
            { versao, justificativa: motivo.trim() },
            {
              onSuccess: () => {
                notificar('Certificado excluído', 'O aluno foi avisado com o motivo.')
                aoExcluir()
              },
            },
          )
        }}
      >
        <label htmlFor="motivo-exclusao" className="text-sm font-medium">
          Motivo (o aluno vai ler)
        </label>
        <textarea
          id="motivo-exclusao"
          rows={2}
          required
          maxLength={1000}
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
          placeholder="Ex.: certificado enviado em duplicidade."
          className={`${classeEntrada} h-auto py-3`}
        />
        {excluir.error != null && <Alerta>{mensagemDeErro(excluir.error)}</Alerta>}
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={excluir.isPending || !motivo.trim()}
            className="inline-flex h-11 items-center rounded-[10px] bg-bordo px-5 text-[15px] font-semibold text-white hover:bg-bordo-escuro disabled:opacity-60"
          >
            {excluir.isPending ? 'Excluindo…' : 'Sim, excluir'}
          </button>
          <button type="button" className={classeBotaoSecundario} onClick={() => setAberto(false)}>
            Cancelar
          </button>
        </div>
      </form>
    </Cartao>
  )
}
