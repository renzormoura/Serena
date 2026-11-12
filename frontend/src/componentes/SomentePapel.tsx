import { Navigate, Outlet } from 'react-router'
import { ehCoordenacao, useUsuarioAtual } from '../api/consultas'

/**
 * Separa as áreas: a coordenação não tem telas de aluno, e vice-versa. O servidor também barra (403); isto só
 * evita mostrar uma tela que não vai funcionar.
 */
export function SomentePapel({ coordenacao }: { coordenacao: boolean }) {
  const { data: usuario } = useUsuarioAtual()
  if (ehCoordenacao(usuario) !== coordenacao) return <Navigate to={coordenacao ? '/' : '/coordenacao/painel'} replace />
  return <Outlet />
}
