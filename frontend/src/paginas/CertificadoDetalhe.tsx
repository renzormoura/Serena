import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { mensagemDeErro } from '../api/cliente'
import { useAtualizarCertificado, useCertificado, useExcluirCertificado } from '../api/consultas'
import type { Certificado, Leitura } from '../api/tipos'
import { FormularioCertificado } from '../componentes/FormularioCertificado'
import { useNotificar } from '../componentes/notificar'
import { PreviaArquivo } from '../componentes/PreviaArquivo'
import { Alerta, Cartao, classeBotaoSecundario, SeloStatus } from '../componentes/ui'
import type { ValoresCertificado } from '../componentes/valoresCertificado'
import { data, horas } from '../formatos'

function valoresDe(c: Certificado): ValoresCertificado {
  return {
    titulo: c.titulo ?? '',
    instituicao: c.instituicao ?? '',
    dataInicio: c.dataInicio ?? '',
    dataFim: c.dataFim ?? '',
    horasCertificado: c.horasCertificado != null ? String(c.horasCertificado).replace('.', ',') : '',
    itemNumero: c.item ? String(c.item.numero) : '',
    observacao: c.observacaoAluno ?? '',
  }
}

const rotuloMetodo: Record<Leitura['metodo'], string> = {
  TEXTO: 'Lemos o texto do PDF.',
  OCR: 'Lemos a imagem com reconhecimento de texto.',
  SEM_TEXTO: 'Não havia texto para ler.',
  FALHA: 'Não conseguimos abrir o arquivo.',
}

/** O que a leitura automática encontrou, para o aluno saber de onde veio cada campo. */
function PainelLeitura({ leitura }: { leitura: Leitura }) {
  return (
    <Cartao className="flex flex-col gap-4 p-6">
      <div className="flex flex-col gap-1">
        <h2 className="text-base font-semibold">O que encontramos no certificado</h2>
        <p className="text-[13px] text-apagado">{rotuloMetodo[leitura.metodo]} Confira cada campo abaixo.</p>
      </div>
      {leitura.trechos.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {leitura.trechos.map((t) => (
            <li key={t} className="rounded-md bg-bordo-claro px-2.5 py-1 text-[13px] text-bordo-escuro">
              “{t}”
            </li>
          ))}
        </ul>
      )}
      {leitura.nomeConfere === true && <p className="text-sm text-ok">✓ Seu nome aparece no certificado.</p>}
      {leitura.avisos.length > 0 && (
        <ul className="flex flex-col gap-1.5 rounded-[10px] bg-alerta-fundo px-4 py-3 text-[13px] text-[#6B4600]">
          {leitura.avisos.map((a) => (
            <li key={a}>{a}</li>
          ))}
        </ul>
      )}
    </Cartao>
  )
}

export function CertificadoDetalhe() {
  const { id = '' } = useParams()
  const { data: certificado, isPending, isError } = useCertificado(id)
  const atualizar = useAtualizarCertificado(id)
  const excluir = useExcluirCertificado()
  const navegar = useNavigate()
  const notificar = useNotificar()
  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false)

  if (isPending) return <p className="text-apagado">Carregando…</p>
  if (isError || !certificado) {
    return (
      <div className="flex flex-col gap-3">
        <p role="alert">Certificado não encontrado.</p>
        <Link to="/certificados" className="font-semibold text-bordo">
          Voltar aos certificados
        </Link>
      </div>
    )
  }

  const urlArquivo = `/api/v1/certificados/${certificado.id}/arquivo`
  const conferindo = certificado.status === 'AGUARDANDO_CONFIRMACAO'

  return (
    <div className="flex flex-col gap-7">
      <Link to="/certificados" className="text-sm font-medium text-tinta-media hover:text-bordo">
        ← Meus certificados
      </Link>
      <header className="flex flex-col gap-2">
        <h1 className="text-[26px] font-semibold">
          {conferindo ? 'Confira os dados' : (certificado.titulo ?? certificado.arquivo.nome)}
        </h1>
        <div className="flex flex-wrap items-center gap-3 text-sm text-apagado">
          <SeloStatus status={certificado.status} />
          {certificado.item && (
            <span>
              Item {certificado.item.numero} · {certificado.item.nome}
            </span>
          )}
          {certificado.horasQueContam != null && <span>· conta {horas(certificado.horasQueContam)}</span>}
          {conferindo && <span>Preenchemos o que encontramos. Confira os dados e o item antes de salvar.</span>}
        </div>
      </header>

      {certificado.observacaoCoordenacao && (certificado.status === 'AJUSTE_SOLICITADO' || certificado.status === 'INDEFERIDO') && (
        <section
          aria-label="Mensagem da coordenação"
          className={`flex flex-col gap-1 rounded-2xl px-6 py-4 ${certificado.status === 'AJUSTE_SOLICITADO' ? 'bg-alerta-fundo text-[#6B4600]' : 'bg-bordo-claro text-bordo-escuro'}`}
        >
          <span className="text-sm font-semibold">
            {certificado.status === 'AJUSTE_SOLICITADO'
              ? 'A coordenação pediu um ajuste. Corrija e salve para voltar à análise.'
              : 'A coordenação reprovou este certificado. Ele não conta horas.'}
          </span>
          <span className="text-sm">“{certificado.observacaoCoordenacao}”</span>
        </section>
      )}

      <div className="flex flex-col gap-6 xl:flex-row">
        <section aria-label="Arquivo do certificado" className="min-w-0 xl:flex-1">
          <PreviaArquivo url={urlArquivo} tipo={certificado.arquivo.tipoMime} nome={certificado.arquivo.nome} />
        </section>

        <section className="flex min-w-0 flex-col gap-6 xl:w-[480px] xl:shrink-0">
          {certificado.status === 'PROCESSANDO' && (
            <Cartao className="flex flex-col gap-3 p-6" >
              <p className="font-semibold">Lendo o certificado…</p>
              <p className="text-sm text-apagado">Leva alguns segundos. Esta página atualiza sozinha.</p>
              <div className="h-1.5 overflow-hidden rounded-full bg-bordo-trilho" role="progressbar" aria-label="Lendo o certificado">
                <div className="h-1.5 w-1/3 animate-pulse rounded-full bg-bordo" />
              </div>
            </Cartao>
          )}

          {conferindo && certificado.leitura && <PainelLeitura leitura={certificado.leitura} />}

          {certificado.editavel && (
            <FormularioCertificado
              key={certificado.versao}
              inicial={valoresDe(certificado)}
              certificadoId={certificado.id}
              erro={atualizar.error}
              enviando={atualizar.isPending}
              textoBotao={conferindo ? 'Sim, salvar' : certificado.status === 'AJUSTE_SOLICITADO' ? 'Salvar e reenviar' : 'Salvar alterações'}
              sugestao={conferindo ? certificado.sugestaoItem : undefined}
              aoEnviar={(dados) =>
                atualizar.mutate(
                  { versao: certificado.versao, dados },
                  {
                    onSuccess: (salvo) => {
                      notificar('Certificado enviado para análise', `"${salvo.titulo}" está na fila da coordenação. Você será avisado do resultado.`)
                      navegar('/certificados')
                    },
                  },
                )
              }
            />
          )}

          {!certificado.editavel && certificado.status !== 'PROCESSANDO' && (
            <Cartao className="flex flex-col gap-4 p-6">
              <dl className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <dt className="text-apagado">Instituição</dt>
                  <dd className="font-medium">{certificado.instituicao ?? '—'}</dd>
                </div>
                <div>
                  <dt className="text-apagado">Período</dt>
                  <dd className="font-medium">
                    {data(certificado.dataInicio)} – {data(certificado.dataFim)}
                  </dd>
                </div>
                <div>
                  <dt className="text-apagado">No certificado</dt>
                  <dd className="font-medium">{horas(certificado.horasCertificado)}</dd>
                </div>
                <div>
                  <dt className="text-apagado">Conta</dt>
                  <dd className="font-medium">{horas(certificado.horasQueContam)}</dd>
                </div>
              </dl>
              <p className="text-sm text-apagado">Certificados já analisados pela coordenação não podem ser alterados.</p>
            </Cartao>
          )}

          {certificado.excluivel && (
            <Cartao className="flex flex-col gap-3 p-5">
              {!confirmandoExclusao ? (
                <button type="button" className={`${classeBotaoSecundario} text-bordo`} onClick={() => setConfirmandoExclusao(true)}>
                  Excluir certificado
                </button>
              ) : (
                <>
                  <p className="text-sm">Excluir este certificado? Ele sai da contagem e da fila da coordenação.</p>
                  <div className="flex gap-3">
                    <button
                      type="button"
                      disabled={excluir.isPending}
                      className="inline-flex h-11 items-center rounded-[10px] bg-bordo px-5 text-[15px] font-semibold text-white"
                      onClick={() => excluir.mutate(certificado.id, { onSuccess: () => navegar('/certificados') })}
                    >
                      {excluir.isPending ? 'Excluindo…' : 'Sim, excluir'}
                    </button>
                    <button type="button" className={classeBotaoSecundario} onClick={() => setConfirmandoExclusao(false)}>
                      Cancelar
                    </button>
                  </div>
                  {excluir.error && <Alerta>{mensagemDeErro(excluir.error)}</Alerta>}
                </>
              )}
            </Cartao>
          )}
        </section>
      </div>
    </div>
  )
}
