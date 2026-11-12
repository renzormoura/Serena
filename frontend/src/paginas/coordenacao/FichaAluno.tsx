import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { mensagemDeErro } from '../../api/cliente'
import { useDefinirIngresso, useFichaAluno, useGerarRequerimento, useRequerimentos } from '../../api/consultas'
import type { FichaAluno as Ficha } from '../../api/tipos'
import { Barras, COR_DADO, COR_DADO_CLARO, ColunasEmpilhadas } from '../../componentes/graficos'
import { horasPorItem, horasPorSemestre } from '../agregacoesAluno'
import { Alerta, Cartao, classeBotaoPrimario, classeBotaoSecundario, classeEntrada, SeloStatus } from '../../componentes/ui'
import { data, dataHora, horas } from '../../formatos'

function Numero({ rotulo, valor, legenda }: { rotulo: string; valor: string; legenda?: string }) {
  return (
    <Cartao className="flex flex-col gap-1.5 p-5">
      <span className="text-sm text-apagado">{rotulo}</span>
      <span className="font-display text-[28px] font-semibold">{valor}</span>
      {legenda && <span className="text-xs text-apagado">{legenda}</span>}
    </Cartao>
  )
}

/** Ingresso do aluno (o cadastro não pede): a coordenação informa para calcular o período. */
function Ingresso({ aluno }: { aluno: Ficha['aluno'] }) {
  const [editando, setEditando] = useState(false)
  const [ano, setAno] = useState(aluno.anoIngresso != null ? String(aluno.anoIngresso) : '')
  const [semestre, setSemestre] = useState(aluno.semestreIngresso != null ? String(aluno.semestreIngresso) : '1')
  const definir = useDefinirIngresso(aluno.id)

  if (!editando) {
    return (
      <span className="inline-flex flex-wrap items-center gap-2">
        {aluno.anoIngresso != null ? (
          <span>
            ingresso {aluno.anoIngresso}.{aluno.semestreIngresso} · {aluno.periodo}º período
          </span>
        ) : (
          <span>ingresso não informado</span>
        )}
        <button type="button" className="font-semibold text-bordo hover:text-bordo-escuro" onClick={() => setEditando(true)}>
          {aluno.anoIngresso != null ? 'alterar' : 'informar'}
        </button>
      </span>
    )
  }
  return (
    <form
      className="flex flex-wrap items-end gap-2"
      onSubmit={(e) => {
        e.preventDefault()
        definir.mutate(ano ? { anoIngresso: Number(ano), semestreIngresso: Number(semestre) } : {}, { onSuccess: () => setEditando(false) })
      }}
    >
      <label className="flex flex-col gap-1 text-xs">
        Ano de ingresso
        <input className={`${classeEntrada} h-10 w-28`} inputMode="numeric" value={ano} onChange={(e) => setAno(e.target.value)} placeholder="2022" />
      </label>
      <label className="flex flex-col gap-1 text-xs">
        Semestre
        <select className={`${classeEntrada} h-10 w-32`} value={semestre} onChange={(e) => setSemestre(e.target.value)}>
          <option value="1">1º semestre</option>
          <option value="2">2º semestre</option>
        </select>
      </label>
      <button type="submit" className={`${classeBotaoPrimario} h-10`} disabled={definir.isPending}>
        Salvar
      </button>
      <button type="button" className={`${classeBotaoSecundario} h-10`} onClick={() => setEditando(false)}>
        Cancelar
      </button>
      {definir.error && <span className="w-full text-[13px] text-bordo">{mensagemDeErro(definir.error)}</span>}
    </form>
  )
}

function Requerimentos({ alunoId, emAnalise }: { alunoId: string; emAnalise: number }) {
  const { data: requerimentos } = useRequerimentos(alunoId)
  const gerar = useGerarRequerimento(alunoId)

  return (
    <Cartao className="flex flex-col gap-4 p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 className="text-lg font-semibold">Requerimento oficial</h2>
          <p className="text-[13px] text-apagado">
            No modelo da aba INTEGRALIZAÇÃO, com as horas validadas, a data de hoje e o seu nome como coordenação.
          </p>
        </div>
        <button type="button" className={classeBotaoPrimario} disabled={gerar.isPending} onClick={() => gerar.mutate()}>
          {gerar.isPending ? 'Gerando…' : 'Gerar requerimento'}
        </button>
      </div>
      {emAnalise > 0 && (
        <p className="rounded-[10px] bg-alerta-fundo px-4 py-3 text-[13px] text-[#6B4600]">
          {emAnalise === 1 ? '1 certificado ainda não foi analisado' : `${emAnalise} certificados ainda não foram analisados`} e fica
          {emAnalise === 1 ? '' : 'm'} de fora do requerimento.
        </p>
      )}
      {gerar.error && <Alerta>{mensagemDeErro(gerar.error)}</Alerta>}
      {requerimentos && requerimentos.length > 0 && (
        <ul className="flex flex-col">
          {requerimentos.map((r) => (
            <li key={r.id} className="flex flex-wrap items-center justify-between gap-3 border-b border-[#F1ECED] py-3 last:border-b-0">
              <span className="flex flex-col">
                <span className="text-sm font-semibold">
                  Nº {r.numero} · {horas(r.totalHoras)}
                </span>
                <span className="text-xs text-apagado">
                  {r.geradoPeloAluno ? 'Gerado pelo aluno, sem deferimento' : `Deferido em ${data(r.dataDeferimento)} por ${r.coordenador}`} · gerado {dataHora(r.geradoEm)}
                </span>
              </span>
              <span className="flex gap-4 text-sm font-semibold">
                <a href={`/api/v1/coordenacao/requerimentos/${r.id}/pdf`} className="text-bordo hover:text-bordo-escuro">
                  PDF
                </a>
                <a href={`/api/v1/coordenacao/requerimentos/${r.id}/xlsx`} className="text-bordo hover:text-bordo-escuro">
                  XLSX
                </a>
              </span>
            </li>
          ))}
        </ul>
      )}
    </Cartao>
  )
}

export function FichaAluno() {
  const { id = '' } = useParams()
  const { data: ficha, isPending, isError } = useFichaAluno(id)

  if (isPending) return <p className="text-apagado">Carregando…</p>
  if (isError || !ficha) {
    return (
      <div className="flex flex-col gap-3">
        <p role="alert">Aluno não encontrado.</p>
        <Link to="/coordenacao/alunos" className="font-semibold text-bordo">
          Voltar aos alunos
        </Link>
      </div>
    )
  }

  const { aluno, saldo, certificados } = ficha
  const porItem = horasPorItem(certificados)
  const porSemestre = horasPorSemestre(certificados)
  return (
    <div className="flex flex-col gap-7">
      <Link to="/coordenacao/alunos" className="text-sm font-medium text-tinta-media hover:text-bordo">
        ← Alunos
      </Link>
      <header className="flex flex-col gap-1.5">
        <h1 className="text-3xl font-semibold">{aluno.nome}</h1>
        <p className="text-[15px] text-apagado">
          Matrícula {aluno.matricula} · {aluno.curso} · {aluno.email}
        </p>
        <div className="text-sm text-apagado">
          <Ingresso aluno={aluno} />
        </div>
      </header>

      <section className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <Numero rotulo="Integralizadas" valor={horas(saldo.integralizadas)} legenda={saldo.percentual != null ? `${saldo.percentual}% do exigido` : 'já validadas'} />
        <Numero rotulo="Em análise" valor={horas(saldo.pendentes)} legenda={`${aluno.paraAnalisar} na fila`} />
        <Numero rotulo="Faltam" valor={saldo.faltantes != null ? horas(saldo.faltantes) : '—'} legenda={saldo.faltantes != null ? undefined : 'total exigido a definir'} />
        <Numero rotulo="Certificados" valor={String(certificados.length)} legenda={`${saldo.certificadosPorStatus.DEFERIDO} validados`} />
      </section>

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Cartao className="flex flex-col gap-4 p-6">
          <h2 className="text-lg font-semibold">Por macroatividade</h2>
          <table className="w-full text-left text-sm">
            <thead className="text-xs text-apagado">
              <tr>
                <th scope="col" className="py-2 font-medium">Macroatividade</th>
                <th scope="col" className="py-2 text-right font-medium">Validadas</th>
                <th scope="col" className="py-2 text-right font-medium">Em análise</th>
              </tr>
            </thead>
            <tbody>
              {saldo.porMacro.map((m) => (
                <tr key={m.codigo} className="border-t border-linha">
                  <td className="py-2.5">{m.nome}</td>
                  <td className="py-2.5 text-right font-semibold tabular-nums">{horas(m.horas)}</td>
                  <td className="py-2.5 text-right tabular-nums text-apagado">{horas(m.horasProjetadas - m.horas)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Cartao>
        <Cartao className="flex flex-col gap-4 p-6">
          <h2 className="text-lg font-semibold">Por item</h2>
          {porItem.length === 0 ? (
            <p className="text-sm text-apagado">Nenhum certificado conferido.</p>
          ) : (
            <Barras dados={porItem.map((i) => ({ rotulo: `${i.numero} · ${i.nome} (${i.certificados}×)`, valor: i.horas }))} formatar={horas} />
          )}
        </Cartao>
      </section>

      {porSemestre.length > 0 && (
        <Cartao className="flex flex-col gap-4 p-6">
          <h2 className="text-lg font-semibold">Horas por semestre da atividade</h2>
          <ColunasEmpilhadas
            dados={porSemestre.map((p) => ({ rotulo: p.rotulo, deferidas: p.deferidas, emAnalise: p.emAnalise }))}
            series={[
              { chave: 'deferidas', rotulo: 'Validadas', cor: COR_DADO },
              { chave: 'emAnalise', rotulo: 'Em análise', cor: COR_DADO_CLARO },
            ]}
            formatar={horas}
            rotuloCategoria="Semestre"
          />
        </Cartao>
      )}

      <Requerimentos alunoId={aluno.id} emAnalise={aluno.paraAnalisar + aluno.emAjuste} />

      <Cartao className="overflow-x-auto">
        <table className="w-full min-w-[640px] text-left text-sm">
          <caption className="px-5 pt-5 pb-2 text-left text-lg font-semibold">Certificados</caption>
          <thead className="border-b border-linha text-xs text-apagado">
            <tr>
              <th scope="col" className="px-5 py-3 font-medium">Atividade</th>
              <th scope="col" className="px-5 py-3 font-medium">Data</th>
              <th scope="col" className="px-5 py-3 font-medium">Item</th>
              <th scope="col" className="px-5 py-3 text-right font-medium">Conta</th>
              <th scope="col" className="px-5 py-3 font-medium">Situação</th>
              <th scope="col" className="px-5 py-3 font-medium">
                <span className="sr-only">Arquivo</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {certificados.map((c) => (
              <tr key={c.id} className="border-b border-[#F1ECED] last:border-b-0 hover:bg-fundo">
                <td className="px-5 py-3.5">
                  <Link to={`/coordenacao/certificados/${c.id}`} className="font-medium hover:text-bordo">
                    {c.titulo ?? c.arquivo.nome}
                  </Link>
                </td>
                <td className="px-5 py-3.5 whitespace-nowrap tabular-nums">{data(c.dataInicio ?? c.dataFim)}</td>
                <td className="px-5 py-3.5">{c.item ? `${c.item.numero} · ${c.item.nome}` : '—'}</td>
                <td className="px-5 py-3.5 text-right tabular-nums">{horas(c.horasQueContam)}</td>
                <td className="px-5 py-3.5">
                  <SeloStatus status={c.status} />
                </td>
                <td className="px-5 py-3.5 text-right">
                  <a
                    href={`/api/v1/coordenacao/certificados/${c.id}/arquivo`}
                    target="_blank"
                    rel="noreferrer"
                    className="text-[13px] font-semibold whitespace-nowrap text-bordo hover:text-bordo-escuro"
                  >
                    Ver arquivo
                  </a>
                </td>
              </tr>
            ))}
            {certificados.length === 0 && (
              <tr>
                <td colSpan={6} className="px-5 py-8 text-center text-apagado">
                  Nenhum certificado enviado.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Cartao>
    </div>
  )
}
