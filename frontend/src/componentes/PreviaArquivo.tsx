/** Mostra o PDF ou a imagem do certificado. `url` pode ser um blob local ou o endereço da API. */
export function PreviaArquivo({ url, tipo, nome }: { url: string; tipo: string; nome: string }) {
  return (
    <div className="flex h-full min-h-[420px] flex-col gap-3 rounded-2xl bg-[#F3F0F1] p-4 sm:p-6">
      <div className="flex items-center justify-between gap-3 text-[13px] text-apagado">
        <span className="truncate">{nome}</span>
        <a href={url} target="_blank" rel="noreferrer" className="shrink-0 font-medium text-bordo">
          Abrir em nova aba
        </a>
      </div>
      {tipo === 'application/pdf' ? (
        <iframe title={`Pré-visualização de ${nome}`} src={url} className="min-h-[560px] w-full flex-1 rounded-md bg-white" />
      ) : (
        <img src={url} alt={`Certificado ${nome}`} className="mx-auto max-h-[640px] w-auto rounded-md bg-white object-contain" />
      )}
    </div>
  )
}
