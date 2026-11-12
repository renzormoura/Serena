import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useCertificados, useSaldo } from '../api/consultas'
import type { StatusCertificado } from '../api/tipos'
import { Icone } from '../componentes/Icone'
import { RequerimentoDoAluno } from '../componentes/RequerimentoDoAluno'
import { Cartao, classeBotaoPrimario, SeloStatus } from '../componentes/ui'
import { horas, rotuloStatus } from '../formatos'

type Filtro = 'TODOS' | StatusCertificado

export function Certificados() {
  const { data: certificados, isPending, isError } = useCertificados()
  const { data: saldo } = useSaldo()
  const [filtro, setFiltro] = useState<Filtro>('TODOS')
  const [busca, setBusca] = useState('')
  const navegar = useNavigate()

  if (isPending) return <p className="text-apagado">Carregando certificados…</p>
  if (isError) return <p role="alert">Não foi possível carregar seus certificados.</p>

  const contagem = (s: StatusCertificado) => certificados.filter((c) => c.status === s).length
  const filtros: Filtro[] = ['TODOS', ...(['AGUARDANDO_CONFIRMACAO', 'PROCESSANDO', 'CONFIRMADO', 'AJUSTE_SOLICITADO', 'DEFERIDO', 'INDEFERIDO'] as const).filter((s) => contagem(s) > 0)]
  const termo = busca.trim().toLocaleLowerCase('pt-BR')
  const visiveis = certificados
    .filter((c) => filtro === 'TODOS' || c.status === filtro)
    .filter((c) => !termo || `${c.titulo ?? c.arquivo.nome} ${c.item ? `${c.item.numero} ${c.item.nome}` : ''}`.toLocaleLowerCase('pt-BR').includes(termo))

  return (
    <div className="flex flex-col gap-7">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-3xl font-semibold">Meus certificados</h1>
          <p className="text-[15px] text-apagado">
            {certificados.length} {certificados.length === 1 ? 'certificado' : 'certificados'}
            {saldo && ` · ${horas(saldo.projecao)} no total, ${horas(saldo.integralizadas)} já validadas`}
          </p>
        </div>
        <Link to="/enviar" className={classeBotaoPrimario}>
          Enviar certificado
        </Link>
      </header>

      {certificados.length === 0 ? (
        <Cartao className="flex flex-col items-center gap-3 px-6 py-14 text-center">
          <span className="flex size-12 items-center justify-center rounded-xl bg-bordo-claro text-bordo">
            <Icone nome="certificados" tamanho={22} />
          </span>
          <p className="font-semibold">Nenhum certificado ainda</p>
          <p className="text-sm text-apagado">Envie o primeiro para começar a contar suas horas.</p>
        </Cartao>
      ) : (
        <>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div role="group" aria-label="Filtrar por status" className="flex flex-wrap gap-2">
              {filtros.map((f) => {
                const ativo = filtro === f
                return (
                  <button
                    key={f}
                    type="button"
                    aria-pressed={ativo}
                    onClick={() => setFiltro(f)}
                    className={`h-9 rounded-full border px-3.5 text-[13px] ${
                      ativo ? 'border-bordo bg-bordo font-semibold text-white' : 'border-linha-forte bg-white font-medium text-tinta-media'
                    }`}
                  >
                    {f === 'TODOS' ? `Todos · ${certificados.length}` : `${rotuloStatus[f]} · ${contagem(f)}`}
                  </button>
                )
              })}
            </div>
            <div className="w-full sm:w-72">
              <label htmlFor="busca" className="sr-only">
                Buscar certificado
              </label>
              <input
                id="busca"
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                placeholder="Buscar por atividade ou item"
                className="h-10 w-full rounded-[10px] border border-linha-forte bg-white px-3.5 text-sm"
              />
            </div>
          </div>

          <Cartao className="overflow-x-auto">
            <table className="w-full min-w-[760px] border-collapse text-sm">
              <thead>
                <tr className="text-left text-xs tracking-[0.06em] text-apagado uppercase">
                  <th className="border-b border-linha px-6 py-4 font-semibold">Atividade</th>
                  <th className="border-b border-linha px-3 py-4 font-semibold">Item</th>
                  <th className="border-b border-linha px-3 py-4 text-right font-semibold">No certificado</th>
                  <th className="border-b border-linha px-3 py-4 text-right font-semibold">Conta</th>
                  <th className="border-b border-linha px-3 py-4 font-semibold">Status</th>
                  <th className="border-b border-linha px-6 py-4">
                    <span className="sr-only">Abrir</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {visiveis.map((c) => (
                  <tr
                    key={c.id}
                    className="cursor-pointer hover:bg-fundo"
                    onClick={() => navegar(`/certificados/${c.id}`)}
                  >
                    <td className="border-b border-[#F3EFF0] px-6 py-3.5 font-medium">
                      {c.titulo ?? <span className="text-apagado">{c.arquivo.nome}</span>}
                    </td>
                    <td className="border-b border-[#F3EFF0] px-3 py-3.5 text-tinta-media">
                      {c.item ? `${c.item.numero} · ${c.item.nome}` : '—'}
                    </td>
                    <td className="border-b border-[#F3EFF0] px-3 py-3.5 text-right text-apagado">{horas(c.horasCertificado)}</td>
                    <td className="border-b border-[#F3EFF0] px-3 py-3.5 text-right font-semibold">{horas(c.horasQueContam)}</td>
                    <td className="border-b border-[#F3EFF0] px-3 py-3.5">
                      <SeloStatus status={c.status} />
                    </td>
                    <td className="border-b border-[#F3EFF0] px-6 py-3.5 text-right">
                      <Link
                        to={`/certificados/${c.id}`}
                        aria-label={`Abrir ${c.titulo ?? c.arquivo.nome}`}
                        className="inline-flex size-9 items-center justify-center rounded-lg text-apagado hover:text-bordo"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
                          <path d="M9 6l6 6-6 6" />
                        </svg>
                      </Link>
                    </td>
                  </tr>
                ))}
                {visiveis.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-6 py-8 text-center text-apagado">
                      Nenhum certificado com esse filtro.
                    </td>
                  </tr>
                )}
              </tbody>
              {filtro === 'TODOS' && !termo && saldo && (
                <tfoot>
                  <tr>
                    <td colSpan={3} className="px-6 py-4 font-semibold">
                      Total
                    </td>
                    <td className="px-3 py-4 text-right font-bold text-bordo">{horas(saldo.projecao)}</td>
                    <td colSpan={2} />
                  </tr>
                </tfoot>
              )}
            </table>
          </Cartao>
        </>
      )}
      <RequerimentoDoAluno
        validadas={contagem('DEFERIDO')}
        emAnalise={contagem('CONFIRMADO') + contagem('AJUSTE_SOLICITADO')}
      />
    </div>
  )
}
