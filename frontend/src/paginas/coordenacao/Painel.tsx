import { Link, useSearchParams } from 'react-router'
import { usePainel } from '../../api/consultas'
import { Barras, Colunas } from '../../componentes/graficos'
import { Alerta, Cartao, classeBotaoSecundario, classeEntrada } from '../../componentes/ui'
import { horas } from '../../formatos'

const numero = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 })

function Indicador({ titulo, valor, legenda }: { titulo: string; valor: string; legenda?: string }) {
  return (
    <Cartao className="flex flex-col gap-1.5 p-6">
      <span className="text-sm text-apagado">{titulo}</span>
      <span className="font-display text-[32px] font-semibold">{valor}</span>
      {legenda && <span className="text-[13px] text-apagado">{legenda}</span>}
    </Cartao>
  )
}

/** Painel da coordenação (design aprovado "Coord-Painel"): visão geral do escopo do coordenador. */
export function Painel() {
  const [parametros, setParametros] = useSearchParams()
  const curso = parametros.get('curso') ?? ''
  const ingresso = parametros.get('ingresso') ?? ''
  const { data: p, isPending, isError } = usePainel({ curso: curso || undefined, ingresso: ingresso || undefined })

  function filtrar(chave: 'curso' | 'ingresso', valor: string) {
    const novos = new URLSearchParams(parametros)
    if (valor) novos.set(chave, valor)
    else novos.delete(chave)
    setParametros(novos, { replace: true })
  }

  const consulta = new URLSearchParams({ ...(curso && { curso }), ...(ingresso && { ingresso }) }).toString()

  return (
    <div className="flex flex-col gap-7">
      <header className="flex flex-col gap-5">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-3xl font-semibold">Painel</h1>
          <p className="text-[15px] text-apagado">Visão geral das horas complementares dos seus alunos.</p>
        </div>
        <div className="flex flex-wrap items-end gap-3">
          <div className="flex w-full flex-col gap-1.5 sm:w-56">
            <label htmlFor="filtro-curso" className="text-xs font-medium text-apagado">
              Curso
            </label>
            <select id="filtro-curso" className={classeEntrada} value={curso} onChange={(e) => filtrar('curso', e.target.value)}>
              <option value="">Todos os cursos</option>
              {p?.filtros.cursos.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nome}
                </option>
              ))}
            </select>
          </div>
          <div className="flex w-full flex-col gap-1.5 sm:w-48">
            <label htmlFor="filtro-ingresso" className="text-xs font-medium text-apagado">
              Ano de ingresso
            </label>
            <select id="filtro-ingresso" className={classeEntrada} value={ingresso} onChange={(e) => filtrar('ingresso', e.target.value)}>
              <option value="">Todos os anos</option>
              {p?.filtros.anosIngresso.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </select>
          </div>
          <a href={`/api/v1/coordenacao/painel/xlsx${consulta ? `?${consulta}` : ''}`} className={`${classeBotaoSecundario} sm:ml-auto`}>
            Exportar XLSX
          </a>
        </div>
      </header>

      {isError && <Alerta>Não foi possível carregar o painel.</Alerta>}
      {isPending && <p className="text-apagado">Carregando…</p>}

      {p && (
        <>
          <section className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
            <Indicador titulo="Alunos" valor={String(p.alunos)} legenda={curso || ingresso ? 'com os filtros aplicados' : 'nos seus cursos'} />
            <Indicador
              titulo="Já integralizaram"
              valor={p.percentualConcluiram != null ? `${p.percentualConcluiram}%` : '—'}
              legenda={p.percentualConcluiram != null ? `${p.concluiram} de ${p.alunos} alunos` : 'total exigido a definir'}
            />
            <Link to="/coordenacao/fila" className="rounded-2xl focus-visible:outline-2">
              <Indicador titulo="Aguardando análise" valor={String(p.aguardandoAnalise)} legenda="certificados na fila" />
            </Link>
            <Indicador
              titulo="Tempo médio de análise"
              valor={p.diasMedioAnalise != null ? `${numero.format(p.diasMedioAnalise)} ${p.diasMedioAnalise === 1 ? 'dia' : 'dias'}` : '—'}
              legenda="do envio à decisão"
            />
          </section>

          <section className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
            <Cartao className="flex flex-col gap-5 p-7">
              <div className="flex flex-col gap-1">
                <h2 className="text-lg font-semibold">Alunos por faixa de progresso</h2>
                <p className="text-[13px] text-apagado">
                  {p.modoFaixas === 'PERCENTUAL'
                    ? `Percentual das ${horas(p.exigidas)} exigidas já validado.`
                    : 'Horas já validadas. Com o total exigido definido no regulamento, a distribuição passa a ser em percentual.'}
                </p>
              </div>
              <Colunas
                dados={p.faixas.map((f) => ({ rotulo: f.rotulo, valor: f.alunos }))}
                formatar={(v) => String(v)}
                rotuloSerie="alunos"
              />
            </Cartao>

            <Cartao className="flex flex-col gap-5 p-7">
              <div className="flex flex-col gap-1">
                <h2 className="text-lg font-semibold">Itens mais usados</h2>
                <p className="text-[13px] text-apagado">Certificados conferidos, sem os reprovados.</p>
              </div>
              {p.itensMaisUsados.length === 0 ? (
                <p className="text-sm text-apagado">Nenhum certificado conferido ainda.</p>
              ) : (
                <Barras
                  dados={p.itensMaisUsados.map((i) => ({ rotulo: `${i.numero} · ${i.nome}`, valor: i.certificados }))}
                  formatar={(v) => String(v)}
                />
              )}
            </Cartao>
          </section>

          <section className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
            <Cartao className="flex flex-col gap-4 overflow-x-auto p-7">
              <div className="flex flex-col gap-1">
                <h2 className="text-lg font-semibold">Alunos em atenção</h2>
                <p className="text-[13px] text-apagado">
                  A partir do {p.periodoAtencao}º período e abaixo de {p.percentualAtencao}% das horas exigidas (só alunos com ingresso informado).
                </p>
              </div>
              {!p.atencaoDisponivel ? (
                <p className="rounded-[10px] bg-fundo px-4 py-3 text-sm text-apagado">
                  Esta lista depende do total de horas exigidas, que ainda não foi definido no regulamento (decisão D1).
                </p>
              ) : p.emAtencao.length === 0 ? (
                <p className="text-sm text-apagado">Nenhum aluno nessa situação.</p>
              ) : (
                <table className="w-full min-w-[480px] text-left text-sm">
                  <thead className="text-xs text-apagado">
                    <tr>
                      <th scope="col" className="py-2 font-medium">Aluno</th>
                      <th scope="col" className="py-2 font-medium">Curso</th>
                      <th scope="col" className="py-2 text-right font-medium">Período</th>
                      <th scope="col" className="py-2 text-right font-medium">Horas</th>
                    </tr>
                  </thead>
                  <tbody>
                    {p.emAtencao.slice(0, 8).map((a) => (
                      <tr key={a.id} className="border-t border-linha">
                        <td className="py-2.5">
                          <Link to={`/coordenacao/alunos/${a.id}`} className="font-medium hover:text-bordo">
                            {a.nome}
                          </Link>
                        </td>
                        <td className="py-2.5 text-apagado">{a.curso}</td>
                        <td className="py-2.5 text-right tabular-nums">{a.periodo}º</td>
                        <td className="py-2.5 text-right tabular-nums">
                          {horas(a.integralizadas)} <span className="text-apagado">· {a.percentual}%</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {p.emAtencao.length > 8 && (
                <p className="text-[13px] text-apagado">E mais {p.emAtencao.length - 8}. A lista completa está na exportação.</p>
              )}
            </Cartao>

            <Cartao className="flex flex-col gap-4 p-7">
              <h2 className="text-lg font-semibold">Leitura automática</h2>
              {p.automacao.percentualAceitas == null ? (
                <p className="text-sm text-apagado">Ainda não há certificados com sugestão de item conferidos pelos alunos.</p>
              ) : (
                <>
                  <span className="font-display text-[40px] leading-none font-semibold">{p.automacao.percentualAceitas}%</span>
                  <p className="text-sm text-tinta-media">
                    das sugestões de item foram aceitas pelos alunos sem correção ({p.automacao.conferidos} certificados).
                  </p>
                  {p.automacao.trocasMaisComuns.length > 0 && (
                    <ul className="flex flex-col gap-1.5 text-[13px] text-apagado">
                      {p.automacao.trocasMaisComuns.map((t) => (
                        <li key={`${t.sugerido}-${t.escolhido}`}>
                          Sugerido {t.sugerido} ({t.nomeSugerido}), escolhido {t.escolhido} ({t.nomeEscolhido}):{' '}
                          <span className="font-semibold text-tinta">{t.vezes}×</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </>
              )}
            </Cartao>
          </section>

          <section className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Cartao className="flex flex-col gap-4 overflow-x-auto p-7">
              <h2 className="text-lg font-semibold">Decisões por item</h2>
              {p.decisoesPorItem.length === 0 ? (
                <p className="text-sm text-apagado">Nenhuma decisão ainda.</p>
              ) : (
                <table className="w-full min-w-[420px] text-left text-sm">
                  <thead className="text-xs text-apagado">
                    <tr>
                      <th scope="col" className="py-2 font-medium">Item</th>
                      <th scope="col" className="py-2 text-right font-medium">Validados</th>
                      <th scope="col" className="py-2 text-right font-medium">Reprovados</th>
                      <th scope="col" className="py-2 text-right font-medium">Ajustes</th>
                    </tr>
                  </thead>
                  <tbody>
                    {p.decisoesPorItem.map((d) => (
                      <tr key={d.numero} className="border-t border-linha">
                        <td className="py-2.5">
                          <span className="font-medium">{d.numero}</span> <span className="text-apagado">· {d.nome}</span>
                        </td>
                        <td className="py-2.5 text-right tabular-nums">{d.deferidos}</td>
                        <td className="py-2.5 text-right tabular-nums">{d.indeferidos}</td>
                        <td className="py-2.5 text-right tabular-nums">{d.ajustes}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </Cartao>

            <Cartao className="flex flex-col gap-4 overflow-x-auto p-7">
              <h2 className="text-lg font-semibold">Média de horas por ano de ingresso</h2>
              {p.porIngresso.length === 0 ? (
                <p className="text-sm text-apagado">Sem alunos.</p>
              ) : (
                <table className="w-full text-left text-sm">
                  <thead className="text-xs text-apagado">
                    <tr>
                      <th scope="col" className="py-2 font-medium">Ingresso</th>
                      <th scope="col" className="py-2 text-right font-medium">Alunos</th>
                      <th scope="col" className="py-2 text-right font-medium">Média integralizada</th>
                    </tr>
                  </thead>
                  <tbody>
                    {p.porIngresso.map((i) => (
                      <tr key={i.anoIngresso ?? "sem"} className="border-t border-linha">
                        <td className="py-2.5 font-medium">{i.anoIngresso ?? 'Não informado'}</td>
                        <td className="py-2.5 text-right tabular-nums">{i.alunos}</td>
                        <td className="py-2.5 text-right tabular-nums">{horas(i.mediaIntegralizadas)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </Cartao>
          </section>
        </>
      )}
    </div>
  )
}
