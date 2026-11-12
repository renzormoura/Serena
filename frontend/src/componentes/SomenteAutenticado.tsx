import { Navigate, Outlet } from 'react-router'
import { useUsuarioAtual } from '../api/consultas'

/** Redireciona para o login quando não há sessão. */
export function SomenteAutenticado() {
  const { data: usuario, isPending } = useUsuarioAtual()
  if (isPending) return null
  return usuario ? <Outlet /> : <Navigate to="/login" replace />
}
