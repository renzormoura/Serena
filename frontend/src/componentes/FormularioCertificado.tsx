import { useState, type FormEvent, type ReactNode } from 'react'
import { ErroApi } from '../api/cliente'
import { useCatalogo, useSimulacao } from '../api/consultas'
import type { DadosCertificado, SugestaoItem } from '../api/tipos'
import { numeroOuNada, paraDados, type ValoresCertificado } from './valoresCertificado'
import { hojeLocal, horas } from '../formatos'
import { Alerta, Campo, classeBotaoPrimario, classeEntrada, Entrada, Selecao } from './ui'

const LIMITE_OBSERVACAO = 250

const rotuloConfianca: Record<SugestaoItem['confianca'], string> = {
  ALTA: 'confiança alta',
  MEDIA: 'confiança média',
  BAIXA: 'confiança baixa — escolha você',
}

/** Sugestão de item com as alternativas como botões: um clique troca o item escolhido. */
function PainelSugestao({
  sugestao,
  escolhido,
  aoEscolher,
}: {
  sugestao: SugestaoItem
  escolhido: string
  aoEscolher: (numero: number) => void
}) {
  const opcoes = [sugestao.item, ...sugestao.alternativas]
  return (
    <fieldset className="flex flex-col gap-2.5 rounded-xl bg-fundo p-4">
      <legend className="sr-only">Itens sugeridos</legend>
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <span className="text-sm font-semibold">Sugestão do Sereno</span>
        <span className={`text-xs font-medium ${sugestao.confianca === 'BAIXA' ? 'text-alerta' : 'text-apagado'}`}>
          {rotuloConfianca[sugestao.confianca]}
        </span>
      </div>
      <div className="flex flex-col gap-2">
        {opcoes.map((item, indice) => {
          const ativo = escolhido === String(item.numero)
          return (
            <button
              key={item.numero}
              type="button"
              aria-pressed={ativo}
              onClick={() => aoEscolher(item.numero)}
              className={`flex items-center justify-between gap-3 rounded-[10px] px-3.5 py-2.5 text-left ${
                ativo ? 'border-[1.5px] border-bordo bg-bordo-claro' : 'border border-linha-forte bg-white hover:border-bordo'
              }`}
            >
              <span className="flex min-w-0 flex-col">
                <span className="text-sm font-semibold">
                  {item.numero} · {item.nome}
                </span>
                <span className="text-xs text-apagado">
                  {item.macroNome} · vale {horas(item.horasPorOcorrencia)}
                </span>
              </span>
              <span className="shrink-0 text-[11px] font-semibold text-apagado">{indice === 0 ? 'sugerido' : 'alternativa'}</span>
            </button>
          )
        })}
      </div>
      <p className="text-xs text-apagado">{sugestao.motivo}</p>
    </fieldset>
  )
}

/**
 * Dados do certificado + item do regulamento + "quanto vai contar" ao vivo, e a pergunta de confirmação do
 * design aprovado. Usado no envio e na edição.
 */
export function FormularioCertificado({
  inicial,
  certificadoId,
  erro,
  enviando,
  textoBotao,
  extraAntes,
  sugestao,
  aoEnviar,
}: {
  inicial: ValoresCertificado
  certificadoId?: string
  erro: unknown
  enviando: boolean
  textoBotao: string
  extraAntes?: ReactNode
  /** Item sugerido pela leitura automática, com alternativas. */
  sugestao?: SugestaoItem | null
  aoEnviar: (dados: DadosCertificado) => void
}) {
  const { data: catalogo } = useCatalogo()
  const [v, setV] = useState(inicial)
  const [hoje] = useState(hojeLocal)

  const simulacao = useSimulacao({
    itemNumero: numeroOuNada(v.itemNumero),
    horasCertificado: numeroOuNada(v.horasCertificado),
    data: v.dataInicio || v.dataFim || undefined,
    ignorarCertificado: certificadoId,
  })

  const campos = erro instanceof ErroApi ? erro.campos : {}
  const campoDoCodigo: Record<string, keyof ValoresCertificado> = {
    'item-invalido': 'itemNumero',
    'data-futura': 'dataInicio',
    'datas-invertidas': 'dataFim',
  }
  const codigo = erro instanceof ErroApi ? erro.codigo : undefined
  const detalhe = erro instanceof ErroApi ? erro.detalhe?.detail : undefined
  const erroDoCampo = (campo: keyof ValoresCertificado) =>
    campos[campo] ?? (codigo && campoDoCodigo[codigo] === campo ? detalhe : undefined)
  const erroGeral =
    erro && Object.keys(campos).length === 0 && !(codigo && campoDoCodigo[codigo])
      ? (detalhe ?? 'Não foi possível salvar. Tente novamente.')
      : null

  function alterar(campo: keyof ValoresCertificado) {
    return (e: { target: { value: string } }) => setV((atual) => ({ ...atual, [campo]: e.target.value }))
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    aoEnviar(paraDados(v))
  }

  const resultado = simulacao.data

  return (
    <form onSubmit={enviar} noValidate className="flex flex-col gap-6">
      {extraAntes}

      <div className="flex flex-col gap-5 rounded-2xl border border-linha bg-white p-6 sm:p-7">
        <Campo id="titulo" rotulo="Atividade" erro={erroDoCampo('titulo')}>
          <Entrada id="titulo" erro={erroDoCampo('titulo')} value={v.titulo} onChange={alterar('titulo')} />
        </Campo>
        <Campo id="instituicao" rotulo="Instituição (opcional)" erro={erroDoCampo('instituicao')}>
          <Entrada id="instituicao" value={v.instituicao} onChange={alterar('instituicao')} />
        </Campo>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Campo id="dataInicio" rotulo="Início" erro={erroDoCampo('dataInicio')}>
            <Entrada id="dataInicio" type="date" max={hoje} erro={erroDoCampo('dataInicio')} value={v.dataInicio} onChange={alterar('dataInicio')} />
          </Campo>
          <Campo id="dataFim" rotulo="Término" erro={erroDoCampo('dataFim')}>
            <Entrada id="dataFim" type="date" max={hoje} erro={erroDoCampo('dataFim')} value={v.dataFim} onChange={alterar('dataFim')} />
          </Campo>
          <Campo id="horas" rotulo="Carga horária" erro={erroDoCampo('horasCertificado')} dica="Como está no certificado.">
            <Entrada
              id="horas"
              inputMode="decimal"
              placeholder="Ex.: 20"
              erro={erroDoCampo('horasCertificado')}
              value={v.horasCertificado}
              onChange={alterar('horasCertificado')}
            />
          </Campo>
        </div>
        {sugestao && (
          <PainelSugestao
            sugestao={sugestao}
            escolhido={v.itemNumero}
            aoEscolher={(numero) => setV((atual) => ({ ...atual, itemNumero: String(numero) }))}
          />
        )}
        <Campo id="item" rotulo="Item do regulamento" erro={erroDoCampo('itemNumero')}>
          <Selecao id="item" erro={erroDoCampo('itemNumero')} value={v.itemNumero} onChange={alterar('itemNumero')}>
            <option value="">Escolha o item…</option>
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
        </Campo>
        <Campo
          id="observacao"
          rotulo="Observação para a coordenação (opcional)"
          erro={erroDoCampo('observacao')}
          dica={`${v.observacao.length}/${LIMITE_OBSERVACAO} caracteres`}
        >
          <textarea
            id="observacao"
            rows={2}
            maxLength={LIMITE_OBSERVACAO}
            value={v.observacao}
            onChange={alterar('observacao')}
            placeholder="Ex.: fui monitor e também participante do evento."
            aria-describedby="observacao-dica"
            className={`${classeEntrada} h-auto py-3`}
          />
        </Campo>
      </div>

      <div className="flex flex-col gap-5 rounded-2xl bg-bordo p-6 text-white sm:p-7" aria-live="polite">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div className="flex flex-col gap-1">
            <span className="text-sm text-[#F1D9DE]">Este certificado vai contar</span>
            <span className="font-display text-4xl font-semibold">{resultado ? horas(resultado.horasQueContam) : '—'}</span>
          </div>
          {resultado && (
            <span className="max-w-[260px] text-right text-[13px] text-[#F1D9DE]">
              {resultado.cortes.length > 0
                ? resultado.cortes.join(' ')
                : `Item ${resultado.item.numero}: ${resultado.item.nome}.`}
            </span>
          )}
          {!resultado && <span className="text-[13px] text-[#F1D9DE]">Escolha o item e informe a carga horária.</span>}
        </div>
        <div className="h-px bg-white/20" />
        <p className="text-base font-semibold">As horas e a classificação estão corretas?</p>
        {erroGeral && <Alerta>{erroGeral}</Alerta>}
        <div className="flex flex-wrap gap-3">
          <button type="submit" disabled={enviando} className={`${classeBotaoPrimario} bg-white !text-bordo hover:bg-bordo-claro`}>
            {enviando ? 'Salvando…' : textoBotao}
          </button>
        </div>
      </div>
    </form>
  )
}
