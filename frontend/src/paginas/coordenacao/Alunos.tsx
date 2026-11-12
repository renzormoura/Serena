import { useState } from 'react'
import { Link } from 'react-router'
import { useAlunosDaCoordenacao } from '../../api/consultas'
import { Alerta, Cartao, classeBotaoSecundario, classeEntrada } from '../../componentes/ui'
import { horas } from '../../formatos'

/** Tira acentos e caixa para a busca: "joão" encontra "Joao". */
function normalizar(texto: string) {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

export function Alunos() {
  const { data: alunos, isPending, isError } = useAlunosDaCoordenacao()
  const [busca, setBusca] = useState('')
  const termo = normalizar(busca.trim())
  const lista = (alunos ?? []).filter(
    (a) => !termo || normalizar(a.nome).includes(termo) || a.matricula.includes(termo) || normalizar(a.email).includes(termo),
  )

  return (
    <div className="flex flex-col gap-7">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-3xl font-semibold">Alunos</h1>
          <p className="text-[15px] text-apagado">Saldo de cada aluno, certificados para analisar e requerimento oficial.</p>
        </div>
        <div className="flex w-full flex-wrap items-center gap-3 sm:w-auto">
          <a href="/api/v1/coordenacao/painel/xlsx" className={classeBotaoSecundario}>
            Exportar XLSX
          </a>
          <div className="w-full sm:w-80">
          <label htmlFor="busca" className="sr-only">
            Buscar aluno
          </label>
          <input
            id="busca"
            type="search"
            placeholder="Buscar por nome, matrícula ou e-mail"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            className={classeEntrada}
          />
          </div>
        </div>
      </header>

      {isError && <Alerta>Não foi possível carregar os alunos.</Alerta>}
      {isPending && <p className="text-apagado">Carregando…</p>}

      {alunos && (
        <Cartao className="overflow-x-auto">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-linha text-xs text-apagado">
              <tr>
                <th scope="col" className="px-5 py-3 font-medium">Aluno</th>
                <th scope="col" className="px-5 py-3 font-medium">Matrícula</th>
                <th scope="col" className="px-5 py-3 font-medium">Ingresso</th>
                <th scope="col" className="px-5 py-3 text-right font-medium">Período</th>
                <th scope="col" className="px-5 py-3 text-right font-medium">Integralizadas</th>
                <th scope="col" className="px-5 py-3 text-right font-medium">Em análise</th>
                <th scope="col" className="px-5 py-3 font-medium">Pendências</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((a) => (
                <tr key={a.id} className="border-b border-[#F1ECED] last:border-b-0 hover:bg-fundo">
                  <td className="px-5 py-3.5">
                    <Link to={`/coordenacao/alunos/${a.id}`} className="flex flex-col">
                      <span className="font-semibold text-tinta hover:text-bordo">{a.nome}</span>
                      <span className="text-xs text-apagado">{a.email}</span>
                    </Link>
                  </td>
                  <td className="px-5 py-3.5 tabular-nums">{a.matricula}</td>
                  <td className="px-5 py-3.5 tabular-nums">
                    {a.anoIngresso != null ? `${a.anoIngresso}.${a.semestreIngresso}` : '—'}
                  </td>
                  <td className="px-5 py-3.5 text-right tabular-nums">{a.periodo != null ? `${a.periodo}º` : '—'}</td>
                  <td className="px-5 py-3.5 text-right font-semibold tabular-nums">
                    {horas(a.integralizadas)}
                    {a.percentual != null && <span className="font-normal text-apagado"> · {a.percentual}%</span>}
                  </td>
                  <td className="px-5 py-3.5 text-right tabular-nums">{horas(a.pendentes)}</td>
                  <td className="px-5 py-3.5 text-[13px]">
                    {a.paraAnalisar > 0 && <span className="block text-bordo">{a.paraAnalisar} para analisar</span>}
                    {a.emAjuste > 0 && <span className="block text-alerta">{a.emAjuste} em ajuste</span>}
                    {a.paraAnalisar === 0 && a.emAjuste === 0 && <span className="text-apagado">—</span>}
                  </td>
                </tr>
              ))}
              {lista.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-5 py-8 text-center text-apagado">
                    {alunos.length === 0 ? 'Nenhum aluno nos seus cursos ainda.' : 'Nenhum aluno encontrado.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </Cartao>
      )}
    </div>
  )
}
