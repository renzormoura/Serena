/**
 * Marca do Sereno: a logo original da Universidade de Vassouras, sem nenhuma alteração (nem cor, nem recorte,
 * nem redesenho), com o nome "sereno" escrito embaixo. A logo sempre aparece sobre fundo branco, como no original;
 * em fundo bordô ela vai dentro de uma caixa branca.
 */
export function Marca({ invertida = false, grande = false }: { invertida?: boolean; grande?: boolean }) {
  const conteudo = (
    <div className="flex flex-col items-center gap-1.5" role="img" aria-label="Universidade de Vassouras — Sereno">
      <img
        src="/logo-univassouras.png"
        alt=""
        width={grande ? 240 : 196}
        height={grande ? 62 : 51}
        className="h-auto max-w-full"
        draggable={false}
      />
      <span
        aria-hidden="true"
        className={`font-marca leading-none font-extrabold tracking-[-0.01em] text-[#9D9394] lowercase ${grande ? 'text-[34px]' : 'text-[26px]'}`}
      >
        sereno
      </span>
    </div>
  )
  if (!invertida) return conteudo
  return <div className="inline-flex self-start rounded-2xl bg-white px-6 py-4">{conteudo}</div>
}
