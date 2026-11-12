import { Link } from 'react-router'
import { useCertificados, useSaldo, useUsuarioAtual } from '../api/consultas'
import type { Saldo } from '../api/tipos'
import { Cartao, classeBotaoPrimario, SeloStatus } from '../componentes/ui'
import { Barras, COR_DADO, COR_DADO_CLARO, ColunasEmpilhadas } from '../componentes/graficos'
import { horas } from '../formatos'
import { horasPorItem, horasPorSemestre } from './agregacoesAluno'

function Indicador({ titulo, valor, legenda, destaque }: { titulo: string; valor: string; legenda: string; destaque?: boolean }) {
  return (
    <Cartao className="flex flex-col gap-2 p-6">
      <span className="text-sm text-apagado">{titulo}</span>
      <span className={`font-display text-[32px] font-semibold ${destaque ? 'text-bordo' : ''}`}>{valor}</span>
      <span className="text-[13px] text-apagado">{legenda}</span>
    </Cartao>
  )
}

function Anel({ percentual }: { percentual: number }) {
  const raio = 62
  const volta = 2 * Math.PI * raio
  return (
    <div className="relative size-[148px] shrink-0">
      <svg width="148" height="148" viewBox="0 0 148 148" role="img" aria-label={`${percentual}% das horas exigidas`}>
        <circle cx="74" cy="74" r={raio} fill="none" stroke="var(--color-bordo-trilho)" strokeWidth="12" />
        <circle
          cx="74"
          cy="74"
          r={raio}
          fill="none"
          stroke="var(--color-bordo)"
          strokeWidth="12"
          strokeLinecap="round"
          strokeDasharray={`${(percentual / 100) * volta} ${volta}`}
          transform="rotate(-90 74 74)"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="font-display text-[32px] font-semibold">{percentual}%</span>
        <span className="text-xs text-apagado">concluído</span>
      </div>
    </div>
  )
}

function Resumo({ saldo }: { saldo: Saldo }) {
  if (saldo.percentual != null && saldo.exigidas != null) {
    return (
      <Cartao className="flex flex-wrap items-center gap-8 p-8">
        <Anel percentual={saldo.percentual} />
        <div className="flex flex-col gap-1.5">
          <span className="text-sm text-apagado">Integralizadas</span>
          <span className="font-display text-[40px] font-semibold">{horas(saldo.integralizadas)}</span>
          <span className="text-sm text-apagado">de {horas(saldo.exigidas)} exigidas</span>
        </div>
      </Cartao>
    )
  }
  return (
    <Cartao className="flex flex-col justify-center gap-1.5 p-8">
      <span className="text-sm text-apagado">Se tudo for validado</span>
      <span className="font-display text-[40px] font-semibold">{horas(saldo.projecao)}</span>
      <span className="text-sm text-apagado">O total exigido ainda não foi definido no regulamento.</span>
    </Cartao>
  )
}

export function Inicio() {
  const { data: usuario } = useUsuarioAtual()
  const { data: saldo, isError } = useSaldo()
  const { data: certificados } = useCertificados()
  const primeiroNome = usuario?.nome.split(' ')[0]
  const total = certificados?.length ?? 0
  const paraConferir = certificados?.filter((c) => c.status === 'AGUARDANDO_CONFIRMACAO') ?? []
  const emAjuste = certificados?.filter((c) => c.status === 'AJUSTE_SOLICITADO') ?? []
  const porSemestre = horasPorSemestre(certificados ?? [])
  const porItem = horasPorItem(certificados ?? [])
  const deferidos = saldo?.certificadosPorStatus.DEFERIDO ?? 0
  const escala = Math.max(saldo?.exigidas ?? 0, ...(saldo?.porMacro.map((m) => m.horasProjetadas) ?? [0]), 1)

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-1.5">
          <p className="text-sm text-apagado">Olá{primeiroNome ? `, ${primeiroNome}` : ''}</p>
          <h1 className="text-3xl font-semibold">Suas horas complementares</h1>
        </div>
        <Link to="/enviar" className={classeBotaoPrimario}>
          Enviar certificado
        </Link>
      </header>

      {isError && <p role="alert">Não foi possível carregar seu saldo.</p>}

      {paraConferir.length > 0 && (
        <section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl bg-bordo px-7 py-5 text-white">
          <div className="flex flex-col gap-1">
            <span className="text-[15px] font-semibold">
              {paraConferir.length === 1 ? '1 certificado espera sua conferência' : `${paraConferir.length} certificados esperam sua conferência`}
            </span>
            <span className="text-sm text-[#F1D9DE]">Eles só contam horas depois que você confere e escolhe o item.</span>
          </div>
          <Link to={`/certificados/${paraConferir[0].id}`} className="inline-flex h-10 items-center rounded-[10px] bg-white px-4 text-sm font-semibold text-bordo">
            Conferir agora
          </Link>
        </section>
      )}

      {emAjuste.length > 0 && (
        <section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-[#F0E2C4] bg-[#FFFBF3] px-7 py-5">
          <div className="flex flex-col gap-1">
            <span className="text-[15px] font-semibold text-[#6B4600]">
              {emAjuste.length === 1
                ? 'A coordenação pediu ajuste em 1 certificado'
                : `A coordenação pediu ajuste em ${emAjuste.length} certificados`}
            </span>
            <span className="text-sm text-[#6B5320]">Veja o motivo, corrija e salve para voltar à análise.</span>
          </div>
          <Link to={`/certificados/${emAjuste[0].id}`} className={classeBotaoPrimario}>
            Ver o ajuste
          </Link>
        </section>
      )}

      {saldo && (
        <>
          <section className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
            <Resumo saldo={saldo} />
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              <Indicador
                titulo="Faltam"
                valor={saldo.faltantes != null ? horas(saldo.faltantes) : '—'}
                legenda={saldo.faltantes != null ? 'para integralizar' : 'total exigido a definir'}
                destaque
              />
              <Indicador titulo="Em análise" valor={horas(saldo.pendentes)} legenda="aguardando a coordenação" />
              <Indicador titulo="Integralizadas" valor={horas(saldo.integralizadas)} legenda="já validadas" />
              <Indicador titulo="Certificados" valor={String(total)} legenda={`${deferidos} validados`} />
            </div>
          </section>

          <section className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
            <Cartao className="flex flex-col gap-6 p-7 sm:p-8">
              <h2 className="text-lg font-semibold">Por macroatividade</h2>
              <ul className="flex flex-col gap-5">
                {saldo.porMacro.map((m) => (
                  <li key={m.codigo} className="flex flex-col gap-2">
                    <div className="flex justify-between gap-3 text-sm">
                      <span>{m.nome}</span>
                      <span className="shrink-0 font-semibold">
                        {horas(m.horasProjetadas)}
                        {m.horasProjetadas > m.horas && (
                          <span className="font-normal text-apagado"> · {horas(m.horas)} validadas</span>
                        )}
                      </span>
                    </div>
                    <div className="relative h-2 rounded-full bg-bordo-trilho">
                      <div className="absolute inset-y-0 left-0 rounded-full bg-bordo-suave" style={{ width: `${(m.horasProjetadas / escala) * 100}%` }} />
                      <div className="absolute inset-y-0 left-0 rounded-full bg-bordo" style={{ width: `${(m.horas / escala) * 100}%` }} />
                    </div>
                  </li>
                ))}
              </ul>
              <p className="text-[13px] text-apagado">
                Escuro: validado. Claro: em análise. Limites por macroatividade aparecem aqui quando o regulamento os definir.
              </p>
            </Cartao>

            <Cartao className="flex flex-col gap-4 p-7 sm:p-8">
              <div className="flex items-baseline justify-between">
                <h2 className="text-lg font-semibold">Últimos envios</h2>
                {total > 0 && (
                  <Link to="/certificados" className="text-sm font-medium text-bordo">
                    Ver todos
                  </Link>
                )}
              </div>
              {total === 0 ? (
                <p className="text-sm text-apagado">Nenhum certificado ainda. Envie o primeiro.</p>
              ) : (
                <ul className="flex flex-col">
                  {certificados!.slice(0, 4).map((c) => (
                    <li key={c.id} className="border-b border-[#F1ECED] last:border-b-0">
                      <Link to={`/certificados/${c.id}`} className="flex items-center justify-between gap-3 py-3.5">
                        <span className="flex min-w-0 flex-col gap-1">
                          <span className="truncate text-sm font-medium">{c.titulo ?? c.arquivo.nome}</span>
                          <span className="text-xs text-apagado">
                            {c.item ? `Item ${c.item.numero} · ${c.item.nome} · ${horas(c.horasQueContam)}` : 'Esperando sua conferência'}
                          </span>
                        </span>
                        <SeloStatus status={c.status} />
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </Cartao>
          </section>

          {porSemestre.length > 0 && (
            <section className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
              <Cartao className="flex flex-col gap-5 p-7 sm:p-8">
                <div className="flex flex-col gap-1">
                  <h2 className="text-lg font-semibold">Horas por semestre</h2>
                  <p className="text-[13px] text-apagado">Pela data da atividade. Só o que conta (com os limites do regulamento).</p>
                </div>
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
              <Cartao className="flex flex-col gap-5 p-7 sm:p-8">
                <div className="flex flex-col gap-1">
                  <h2 className="text-lg font-semibold">Por item</h2>
                  <p className="text-[13px] text-apagado">Validadas e em análise, nos itens que você já usou.</p>
                </div>
                <Barras
                  dados={porItem.slice(0, 6).map((i) => ({
                    rotulo: `${i.numero} · ${i.nome} (${i.certificados}×)`,
                    valor: i.horas,
                  }))}
                  formatar={horas}
                />
                {porItem.length > 6 && (
                  <Link to="/certificados" className="text-sm font-medium text-bordo">
                    Ver todos os certificados
                  </Link>
                )}
              </Cartao>
            </section>
          )}
        </>
      )}
    </div>
  )
}
