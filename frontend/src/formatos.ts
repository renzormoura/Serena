import type { StatusCertificado } from './api/tipos'

const numero = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })

/** 20 → "20 h"; 4.5 → "4,5 h". */
export function horas(valor: number | null | undefined): string {
  return valor == null ? '—' : `${numero.format(valor)} h`
}

/** "2024-05-22" → "22/05/2024". Sem fuso: datas da API são datas puras. */
export function data(iso: string | null | undefined): string {
  if (!iso) return '—'
  const [ano, mes, dia] = iso.slice(0, 10).split('-')
  return `${dia}/${mes}/${ano}`
}

/** Data de hoje no fuso do navegador, no formato do <input type="date"> (AAAA-MM-DD). */
export function hojeLocal(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export function tamanhoArquivo(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${numero.format(Math.round(bytes / 1024))} KB`
  return `${numero.format(Math.round((bytes / 1024 / 1024) * 10) / 10)} MB`
}

export const rotuloStatus: Record<StatusCertificado, string> = {
  PROCESSANDO: 'Lendo…',
  AGUARDANDO_CONFIRMACAO: 'Conferir',
  CONFIRMADO: 'Em análise',
  AJUSTE_SOLICITADO: 'Ajuste solicitado',
  DEFERIDO: 'Validado',
  INDEFERIDO: 'Reprovado',
}

/** Classes do selo de status: cor e texto juntos, nunca só cor. */
export const estiloStatus: Record<StatusCertificado, string> = {
  PROCESSANDO: 'bg-[#F3F0F1] text-tinta-media',
  AGUARDANDO_CONFIRMACAO: 'bg-bordo-claro text-bordo',
  CONFIRMADO: 'bg-alerta-fundo text-alerta',
  AJUSTE_SOLICITADO: 'bg-alerta-fundo text-alerta',
  DEFERIDO: 'bg-ok-fundo text-ok',
  INDEFERIDO: 'bg-bordo-claro text-bordo-escuro',
}

/** "hoje", "ontem", "há 3 dias" — para a fila da coordenação. */
export function tempoRelativo(iso: string | null | undefined, agora = new Date()): string {
  if (!iso) return '—'
  const inicioDoDia = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const dias = Math.round((inicioDoDia(agora) - inicioDoDia(new Date(iso))) / 86_400_000)
  if (dias <= 0) return 'hoje'
  if (dias === 1) return 'ontem'
  return `há ${dias} dias`
}

/** Data e hora locais de um instante da API: "11/11/2026 às 14:05". */
export function dataHora(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  const dois = (n: number) => String(n).padStart(2, '0')
  return `${dois(d.getDate())}/${dois(d.getMonth() + 1)}/${d.getFullYear()} às ${dois(d.getHours())}:${dois(d.getMinutes())}`
}

export const rotuloDecisao: Record<'DEFERIDO' | 'INDEFERIDO' | 'AJUSTE_SOLICITADO' | 'REABERTO' | 'EXCLUIDO', string> = {
  DEFERIDO: 'Validado',
  INDEFERIDO: 'Reprovado',
  AJUSTE_SOLICITADO: 'Ajuste pedido',
  REABERTO: 'Reaberto',
  EXCLUIDO: 'Excluído',
}
