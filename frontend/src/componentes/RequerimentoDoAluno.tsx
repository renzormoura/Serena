import { mensagemDeErro } from '../api/cliente'
import { useGerarMeuRequerimento, useMeusRequerimentos } from '../api/consultas'
import { horas, dataHora } from '../formatos'
import { useNotificar } from './notificar'
import { Alerta, Cartao, classeBotaoSecundario } from './ui'

/**
 * O aluno gera o requerimento oficial (modelo da aba INTEGRALIZAÇÃO) com as horas já validadas. "Deferido em" e a
 * assinatura ficam em branco: quem preenche é a coordenação.
 */
export function RequerimentoDoAluno({ emAnalise, validadas }: { emAnalise: number; validadas: number }) {
  const { data: requerimentos } = useMeusRequerimentos()
  const gerar = useGerarMeuRequerimento()
  const notificar = useNotificar()

  return (
    <Cartao className="flex flex-col gap-4 p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex max-w-xl flex-col gap-1">
          <h2 className="text-lg font-semibold">Requerimento oficial</h2>
          <p className="text-[13px] text-apagado">
            O mesmo modelo usado pela coordenação, com todas as suas horas já validadas, item por item. "Deferido em" e a
            assinatura ficam em branco para a coordenação preencher.
          </p>
        </div>
        <button
          type="button"
          className={classeBotaoSecundario}
          disabled={gerar.isPending || validadas === 0}
          onClick={() => gerar.mutate(undefined, { onSuccess: (r) => notificar('Requerimento gerado', `Nº ${r.numero} · ${horas(r.totalHoras)}. Baixe em PDF ou XLSX.`) })}
        >
          {gerar.isPending ? 'Gerando…' : 'Gerar requerimento'}
        </button>
      </div>
      {validadas === 0 && (
        <p className="text-[13px] text-apagado">Disponível quando a coordenação validar o primeiro certificado.</p>
      )}
      {validadas > 0 && emAnalise > 0 && (
        <p className="rounded-[10px] bg-alerta-fundo px-4 py-3 text-[13px] text-[#6B4600]">
          {emAnalise === 1 ? '1 certificado ainda está em análise e fica' : `${emAnalise} certificados ainda estão em análise e ficam`} de
          fora. Gere de novo depois que forem validados.
        </p>
      )}
      {gerar.error && <Alerta>{mensagemDeErro(gerar.error)}</Alerta>}
      {requerimentos && requerimentos.length > 0 && (
        <ul className="flex flex-col">
          {requerimentos.map((r) => (
            <li key={r.id} className="flex flex-wrap items-center justify-between gap-3 border-t border-linha py-3">
              <span className="flex flex-col">
                <span className="text-sm font-semibold">
                  Nº {r.numero} · {horas(r.totalHoras)}
                </span>
                <span className="text-xs text-apagado">
                  {r.geradoPeloAluno ? 'Gerado por você' : `Deferido por ${r.coordenador}`} em {dataHora(r.geradoEm)}
                </span>
              </span>
              <span className="flex gap-4 text-sm font-semibold">
                <a href={`/api/v1/alunos/me/requerimentos/${r.id}/pdf`} className="text-bordo hover:text-bordo-escuro">
                  PDF
                </a>
                <a href={`/api/v1/alunos/me/requerimentos/${r.id}/xlsx`} className="text-bordo hover:text-bordo-escuro">
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
