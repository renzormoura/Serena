import { useState, type DragEvent } from 'react'
import { Link } from 'react-router'
import { mensagemDeErro } from '../api/cliente'
import { useCertificados, useEnviarArquivos } from '../api/consultas'
import type { Certificado, ResultadoEnvio } from '../api/tipos'
import { Icone } from '../componentes/Icone'
import { Alerta, Cartao, classeBotaoPrimario, SeloStatus } from '../componentes/ui'
import { horas, tamanhoArquivo } from '../formatos'

const ACEITOS = ['application/pdf', 'image/jpeg', 'image/png']
const LIMITE = 10 * 1024 * 1024
const POR_VEZ = 10

interface Recusado {
  nome: string
  motivo: string
}

function separar(lista: FileList | null): { validos: File[]; recusados: Recusado[] } {
  const validos: File[] = []
  const recusados: Recusado[] = []
  for (const arquivo of Array.from(lista ?? [])) {
    if (!ACEITOS.includes(arquivo.type)) recusados.push({ nome: arquivo.name, motivo: 'Envie PDF, JPG ou PNG.' })
    else if (arquivo.size > LIMITE) recusados.push({ nome: arquivo.name, motivo: 'Passa de 10 MB.' })
    else validos.push(arquivo)
  }
  return { validos, recusados }
}

function LinhaEnvio({ resultado, atual }: { resultado: ResultadoEnvio; atual?: Certificado }) {
  const certificado = atual ?? resultado.certificado
  const icone = (
    <span className="flex size-10 shrink-0 items-center justify-center rounded-[10px] bg-bordo-claro text-[11px] font-bold text-bordo">
      {resultado.nomeArquivo.split('.').pop()?.toUpperCase().slice(0, 4) ?? 'ARQ'}
    </span>
  )

  if (!certificado) {
    return (
      <li className="flex flex-wrap items-center gap-4 rounded-[14px] border border-[#F0E2C4] bg-[#FFFBF3] px-5 py-4">
        {icone}
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <span className="truncate text-[15px] font-medium">{resultado.nomeArquivo}</span>
          <span className="text-[13px] text-[#6B5320]">{resultado.erroMensagem}</span>
        </div>
      </li>
    )
  }

  const lendo = certificado.status === 'PROCESSANDO'
  const resumo = lendo
    ? 'Lendo o certificado…'
    : certificado.status === 'AGUARDANDO_CONFIRMACAO'
      ? [
          certificado.titulo,
          certificado.horasCertificado != null ? `${horas(certificado.horasCertificado)} encontradas` : 'carga horária não encontrada',
          certificado.sugestaoItem ? `sugerido item ${certificado.sugestaoItem.item.numero}` : null,
        ]
          .filter(Boolean)
          .join(' · ')
      : 'Conferido'

  return (
    <li className="flex flex-wrap items-center gap-4 rounded-[14px] border border-linha bg-white px-5 py-4">
      {icone}
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <span className="truncate text-[15px] font-medium">{resultado.nomeArquivo}</span>
        {lendo ? (
          <div className="h-1.5 overflow-hidden rounded-full bg-bordo-trilho" role="progressbar" aria-label="Lendo o certificado">
            <div className="h-1.5 w-1/3 animate-pulse rounded-full bg-bordo" />
          </div>
        ) : (
          <span className="text-[13px] text-apagado">{resumo}</span>
        )}
      </div>
      {certificado.status === 'AGUARDANDO_CONFIRMACAO' ? (
        <Link to={`/certificados/${certificado.id}`} className={`${classeBotaoPrimario} h-10 px-4 text-sm`}>
          Conferir
        </Link>
      ) : (
        <SeloStatus status={certificado.status} />
      )}
    </li>
  )
}

export function Enviar() {
  const [arrastando, setArrastando] = useState(false)
  const [recusados, setRecusados] = useState<Recusado[]>([])
  const [enviados, setEnviados] = useState<ResultadoEnvio[]>([])
  const enviar = useEnviarArquivos()
  const { data: certificados } = useCertificados()
  const atuais = new Map(certificados?.map((c) => [c.id, c]))

  function receber(lista: FileList | null) {
    const { validos, recusados: fora } = separar(lista)
    setRecusados(fora)
    if (validos.length === 0) return
    if (validos.length > POR_VEZ) {
      setRecusados([...fora, { nome: `${validos.length} arquivos`, motivo: `Envie no máximo ${POR_VEZ} por vez.` }])
      return
    }
    enviar.mutate(validos, { onSuccess: (resultados) => setEnviados((anteriores) => [...resultados, ...anteriores]) })
  }

  function soltar(evento: DragEvent) {
    evento.preventDefault()
    setArrastando(false)
    receber(evento.dataTransfer.files)
  }

  const paraConferir = enviados.filter((r) => r.certificado && atuais.get(r.certificado.id)?.status === 'AGUARDANDO_CONFIRMACAO')

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-col gap-2">
        <h1 className="text-3xl font-semibold">Enviar certificados</h1>
        <p className="text-[15px] text-apagado">
          O Sereno lê cada arquivo e preenche a carga horária, as datas e o nome da atividade. Você confere tudo e escolhe o
          item antes de salvar.
        </p>
      </header>

      <div className="flex flex-col gap-3">
        <label
          htmlFor="arquivos"
          onDragOver={(e) => {
            e.preventDefault()
            setArrastando(true)
          }}
          onDragLeave={() => setArrastando(false)}
          onDrop={soltar}
          className={`flex cursor-pointer flex-col items-center justify-center gap-3.5 rounded-2xl border-[1.5px] border-dashed px-6 py-14 text-center ${
            arrastando ? 'border-bordo bg-bordo-claro' : 'border-[#D9C3C8] bg-white'
          }`}
        >
          <span className="flex size-14 items-center justify-center rounded-[14px] bg-bordo-claro text-bordo">
            <Icone nome="enviar" tamanho={26} />
          </span>
          <span className="text-[17px] font-semibold">{enviar.isPending ? 'Enviando…' : 'Arraste os certificados aqui'}</span>
          <span className="text-sm text-apagado">
            ou <span className="font-semibold text-bordo">escolha arquivos</span> · PDF, JPG ou PNG · até 10 MB cada ·{' '}
            {POR_VEZ} por vez
          </span>
        </label>
        <input
          id="arquivos"
          type="file"
          multiple
          accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
          className="sr-only"
          disabled={enviar.isPending}
          onChange={(e) => {
            receber(e.target.files)
            e.target.value = ''
          }}
        />
        {enviar.error && <Alerta>{mensagemDeErro(enviar.error, 'Não foi possível enviar. Tente novamente.')}</Alerta>}
        {recusados.length > 0 && (
          <Alerta>
            {recusados.map((r) => (
              <span key={r.nome} className="block">
                {r.nome}: {r.motivo}
              </span>
            ))}
          </Alerta>
        )}
      </div>

      {enviados.length > 0 && (
        <section className="flex flex-col gap-3.5" aria-live="polite">
          <div className="flex items-baseline justify-between">
            <h2 className="text-lg font-semibold">Enviados agora</h2>
            <span className="text-[13px] text-apagado">
              {paraConferir.length > 0 ? `${paraConferir.length} para conferir` : `${enviados.length} arquivo(s)`}
            </span>
          </div>
          <ul className="flex flex-col gap-3">
            {enviados.map((r, i) => (
              <LinhaEnvio key={r.certificado?.id ?? `${r.nomeArquivo}-${i}`} resultado={r} atual={r.certificado ? atuais.get(r.certificado.id) : undefined} />
            ))}
          </ul>
        </section>
      )}

      {enviados.length === 0 && (
        <Cartao className="flex flex-col gap-2 p-6 text-sm text-apagado">
          <span className="font-semibold text-tinta">Como funciona</span>
          <span>1. Envie o PDF (ou uma foto nítida) do certificado.</span>
          <span>2. O Sereno lê o texto e sugere carga horária, datas e o nome da atividade.</span>
          <span>3. Você confere, escolhe o item do regulamento e salva. O certificado vai para a coordenação.</span>
        </Cartao>
      )}
      <p className="text-[13px] text-apagado">Tamanho máximo por arquivo: {tamanhoArquivo(LIMITE)}.</p>
    </div>
  )
}
