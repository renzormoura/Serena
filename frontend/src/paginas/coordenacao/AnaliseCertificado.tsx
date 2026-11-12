import { useNavigate, useParams } from 'react-router'
import { PainelAnalise } from './PainelAnalise'

/** Um certificado aberto a partir da ficha do aluno (fora da fila). */
export function AnaliseCertificado() {
  const { id = '' } = useParams()
  const navegar = useNavigate()
  return (
    <div className="flex flex-col gap-6">
      <button type="button" onClick={() => navegar(-1)} className="self-start text-sm font-medium text-tinta-media hover:text-bordo">
        ← Voltar
      </button>
      <PainelAnalise id={id} aoExcluir={(alunoId) => navegar(`/coordenacao/alunos/${alunoId}`, { replace: true })} />
    </div>
  )
}
