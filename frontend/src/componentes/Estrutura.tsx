import { NavLink, Outlet, useNavigate } from 'react-router'
import { ehCoordenacao, useAvisos, useFila, useSair, useUsuarioAtual } from '../api/consultas'
import { Icone, type NomeIcone } from './Icone'
import { Marca } from './Marca'

interface ItemMenu {
  para: string
  rotulo: string
  icone: NomeIcone
  fim?: boolean
  contador?: 'fila' | 'avisos'
}

const menuAluno: ItemMenu[] = [
  { para: '/', rotulo: 'Início', icone: 'inicio', fim: true },
  { para: '/enviar', rotulo: 'Enviar certificado', icone: 'enviar' },
  { para: '/certificados', rotulo: 'Meus certificados', icone: 'certificados' },
  { para: '/avisos', rotulo: 'Avisos', icone: 'avisos', contador: 'avisos' },
  { para: '/regulamento', rotulo: 'Regulamento', icone: 'regulamento' },
]

const menuCoordenacao: ItemMenu[] = [
  { para: '/coordenacao/painel', rotulo: 'Painel', icone: 'painel' },
  { para: '/coordenacao/fila', rotulo: 'Fila de análise', icone: 'fila', contador: 'fila' },
  { para: '/coordenacao/alunos', rotulo: 'Alunos', icone: 'alunos' },
  { para: '/coordenacao/equipe', rotulo: 'Equipe', icone: 'equipe' },
  { para: '/regulamento', rotulo: 'Regulamento', icone: 'regulamento' },
]

/** Avisos ainda não lidos pelo aluno. */
function ContadorAvisos() {
  const { data: caixa } = useAvisos()
  if (!caixa || caixa.naoLidos === 0) return null
  return (
    <span className="ml-auto rounded-full bg-bordo px-2 py-0.5 text-xs font-semibold text-white" aria-label={`${caixa.naoLidos} não lidos`}>
      {caixa.naoLidos}
    </span>
  )
}

/** Quantos certificados esperam a coordenação. */
function ContadorFila() {
  const { data: fila } = useFila()
  if (!fila || fila.length === 0) return null
  return (
    <span className="ml-auto rounded-full bg-bordo px-2 py-0.5 text-xs font-semibold text-white" aria-label={`${fila.length} na fila`}>
      {fila.length}
    </span>
  )
}

function iniciais(nome: string) {
  const partes = nome.trim().split(/\s+/)
  return ((partes[0]?.[0] ?? '') + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase()
}

/** Menu lateral + conteúdo. No celular o menu empilha acima do conteúdo. */
export function Estrutura() {
  const { data: usuario } = useUsuarioAtual()
  const sair = useSair()
  const navegar = useNavigate()
  const coordenacao = ehCoordenacao(usuario)
  const navegacao = coordenacao ? menuCoordenacao : menuAluno

  return (
    <div className="flex min-h-screen flex-col md:flex-row">
      <aside className="flex flex-col gap-8 border-b border-linha bg-white px-5 py-7 md:w-64 md:shrink-0 md:border-r md:border-b-0">
        <div className="flex flex-col items-center gap-1 px-2">
          <Marca />
          {coordenacao && <span className="text-xs font-semibold tracking-wide text-apagado uppercase">Coordenação</span>}
        </div>
        <nav aria-label="Principal" className="flex flex-col gap-1">
          {navegacao.map((item) => (
            <NavLink
              key={item.para}
              to={item.para}
              end={item.fim}
              className={({ isActive }) =>
                `flex h-11 items-center gap-3 rounded-[10px] px-3 text-[15px] ${
                  isActive ? 'bg-bordo-claro font-semibold text-bordo' : 'font-medium text-tinta-media hover:bg-fundo'
                }`
              }
            >
              <Icone nome={item.icone} />
              {item.rotulo}
              {item.contador === 'fila' && <ContadorFila />}
              {item.contador === 'avisos' && <ContadorAvisos />}
            </NavLink>
          ))}
        </nav>
        {usuario && (
          <div className="mt-auto flex items-center gap-3 border-t border-linha p-3">
            <div className="flex size-9 items-center justify-center rounded-full bg-bordo-suave text-[13px] font-semibold text-bordo">
              {iniciais(usuario.nome)}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold">{usuario.nome}</p>
              <p className="truncate text-xs text-apagado">{usuario.email}</p>
            </div>
            <button
              type="button"
              aria-label="Sair"
              className="flex size-9 items-center justify-center rounded-lg text-apagado hover:bg-fundo hover:text-bordo"
              onClick={() => sair.mutate(undefined, { onSuccess: () => navegar('/login') })}
            >
              <Icone nome="sair" />
            </button>
          </div>
        )}
      </aside>
      <main className="min-w-0 flex-1 px-4 py-8 sm:px-8 lg:px-12 lg:py-10">
        <div className="mx-auto max-w-[1080px]">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
