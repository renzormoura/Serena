import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ErroApi, json, requisitar } from './cliente'
import type {
  AlunoNaCoordenacao,
  CaixaDeAvisos,
  Equipe,
  FormularioMembro,
  MembroEquipe,
  Analise,
  Catalogo,
  Certificado,
  Curso,
  DadosCertificado,
  FichaAluno,
  FormularioCadastro,
  FormularioDecisao,
  FormularioSimulacao,
  ItemDaFila,
  Painel,
  Requerimento,
  ResultadoLote,
  ResultadoEnvio,
  Saldo,
  Simulacao,
  UsuarioAtual,
} from './tipos'

const chaves = {
  me: ['me'] as const,
  regulamento: ['regulamento', 'vigente'] as const,
  cursos: ['cursos'] as const,
  saldo: ['saldo'] as const,
  certificados: ['certificados'] as const,
  certificado: (id: string) => ['certificados', id] as const,
  avisos: ['avisos'] as const,
  meusRequerimentos: ['requerimentos', 'meus'] as const,
  coordenacao: ['coordenacao'] as const,
  equipe: ['coordenacao', 'equipe'] as const,
  fila: ['coordenacao', 'fila'] as const,
  analise: (id: string) => ['coordenacao', 'certificados', id] as const,
  alunos: ['coordenacao', 'alunos'] as const,
  painel: (filtros: object) => ['coordenacao', 'painel', filtros] as const,
  ficha: (id: string) => ['coordenacao', 'alunos', id] as const,
  requerimentos: (alunoId: string) => ['coordenacao', 'alunos', alunoId, 'requerimentos'] as const,
}

/** A conta é da coordenação (ou administração): vê a fila e os alunos, não envia certificados. */
export function ehCoordenacao(usuario: UsuarioAtual | null | undefined): boolean {
  return !!usuario?.papeis.some((p) => p === 'COORDENADOR' || p === 'ADMIN')
}

export function useUsuarioAtual() {
  return useQuery({
    queryKey: chaves.me,
    queryFn: async () => {
      try {
        return await requisitar<UsuarioAtual>('/api/v1/me')
      } catch (erro) {
        if (erro instanceof ErroApi && erro.status === 401) return null
        throw erro
      }
    },
    staleTime: 60_000,
  })
}

export function useCatalogo() {
  return useQuery({
    queryKey: chaves.regulamento,
    queryFn: () => requisitar<Catalogo>('/api/v1/regulamentos/vigente'),
    staleTime: 5 * 60_000,
  })
}

export function useCursos() {
  return useQuery({
    queryKey: chaves.cursos,
    queryFn: () => requisitar<Curso[]>('/api/v1/cursos'),
    staleTime: 5 * 60_000,
  })
}

export function useSaldo() {
  return useQuery({ queryKey: chaves.saldo, queryFn: () => requisitar<Saldo>('/api/v1/alunos/me/saldo') })
}

/** Enquanto algum certificado está sendo lido, consulta de novo a cada 1,5 s. */
const INTERVALO_LEITURA = 1500

export function useCertificados() {
  return useQuery({
    queryKey: chaves.certificados,
    queryFn: () => requisitar<Certificado[]>('/api/v1/certificados'),
    refetchInterval: (consulta) =>
      consulta.state.data?.some((c) => c.status === 'PROCESSANDO') ? INTERVALO_LEITURA : false,
  })
}

export function useCertificado(id: string) {
  const cliente = useQueryClient()
  return useQuery({
    queryKey: chaves.certificado(id),
    queryFn: async () => {
      const certificado = await requisitar<Certificado>(`/api/v1/certificados/${id}`)
      if (certificado.status !== 'PROCESSANDO') void cliente.invalidateQueries({ queryKey: chaves.certificados, exact: true })
      return certificado
    },
    refetchInterval: (consulta) => (consulta.state.data?.status === 'PROCESSANDO' ? INTERVALO_LEITURA : false),
  })
}

/** Quanto um certificado contaria. Só consulta com item e horas preenchidos. */
export function useSimulacao(form: Partial<FormularioSimulacao>) {
  const pronto = form.itemNumero != null && form.horasCertificado != null && !Number.isNaN(form.horasCertificado)
  return useQuery({
    queryKey: ['simulacao', form],
    queryFn: () => requisitar<Simulacao>('/api/v1/certificados/simular', json(form)),
    enabled: pronto,
    placeholderData: keepPreviousData,
    staleTime: 10_000,
  })
}

/** Garante o cookie XSRF-TOKEN antes do primeiro POST (o servidor o grava em qualquer resposta). */
async function prepararCsrf() {
  await fetch('/api/v1/regulamentos/vigente', { credentials: 'same-origin' })
}

export function useEntrar() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (dados: { email: string; senha: string }) => {
      await prepararCsrf()
      await requisitar<void>('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams(dados),
      })
    },
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaves.me }),
  })
}

export function useCadastrar() {
  return useMutation({
    mutationFn: async (dados: FormularioCadastro) => {
      await prepararCsrf()
      await requisitar<unknown>('/api/v1/cadastro', json(dados))
    },
  })
}

export function useSair() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: () => requisitar<void>('/api/v1/auth/logout', { method: 'POST' }),
    onSuccess: () => {
      cliente.clear()
      cliente.setQueryData(chaves.me, null)
    },
  })
}

/** Avisos do usuário (resultado das análises). Consulta de novo a cada minuto. */
export function useAvisos() {
  return useQuery({
    queryKey: chaves.avisos,
    queryFn: () => requisitar<CaixaDeAvisos>('/api/v1/avisos'),
    refetchInterval: 60_000,
  })
}

export function useMeusRequerimentos() {
  return useQuery({ queryKey: chaves.meusRequerimentos, queryFn: () => requisitar<Requerimento[]>('/api/v1/alunos/me/requerimentos') })
}

export function useGerarMeuRequerimento() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: () => requisitar<Requerimento>('/api/v1/alunos/me/requerimentos', { method: 'POST' }),
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaves.meusRequerimentos }),
  })
}

export function useMarcarAvisosLidos() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: () => requisitar<void>('/api/v1/avisos/lidos', { method: 'POST' }),
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaves.avisos }),
  })
}

function useAoMudarCertificados() {
  const cliente = useQueryClient()
  return () => {
    void cliente.invalidateQueries({ queryKey: chaves.certificados })
    void cliente.invalidateQueries({ queryKey: chaves.saldo })
    void cliente.invalidateQueries({ queryKey: ['simulacao'] })
  }
}

/** Envia um ou mais arquivos; a leitura automática roda no servidor e o status é acompanhado pela lista. */
export function useEnviarArquivos() {
  const aoMudar = useAoMudarCertificados()
  return useMutation({
    mutationFn: (arquivos: File[]) => {
      const corpo = new FormData()
      arquivos.forEach((a) => corpo.append('arquivos', a))
      return requisitar<ResultadoEnvio[]>('/api/v1/certificados/envios', { method: 'POST', body: corpo })
    },
    onSuccess: aoMudar,
  })
}

export function useAtualizarCertificado(id: string) {
  const aoMudar = useAoMudarCertificados()
  return useMutation({
    mutationFn: ({ versao, dados }: { versao: number; dados: DadosCertificado }) =>
      requisitar<Certificado>(`/api/v1/certificados/${id}`, json({ versao, dados }, 'PUT')),
    onSuccess: aoMudar,
  })
}

export function useExcluirCertificado() {
  const aoMudar = useAoMudarCertificados()
  return useMutation({
    mutationFn: (id: string) => requisitar<void>(`/api/v1/certificados/${id}`, { method: 'DELETE' }),
    onSuccess: aoMudar,
  })
}

// ---- Coordenação ----

export function useFila() {
  return useQuery({ queryKey: chaves.fila, queryFn: () => requisitar<ItemDaFila[]>('/api/v1/coordenacao/fila') })
}

export function useAnalise(id: string | undefined) {
  return useQuery({
    queryKey: chaves.analise(id ?? ''),
    queryFn: () => requisitar<Analise>(`/api/v1/coordenacao/certificados/${id}`),
    enabled: !!id,
  })
}

/** Depois de qualquer decisão: fila, fichas e a análise aberta mudam. */
function useAoDecidir() {
  const cliente = useQueryClient()
  return () => void cliente.invalidateQueries({ queryKey: chaves.coordenacao })
}

export function useDecidir(id: string) {
  const cliente = useQueryClient()
  const aoDecidir = useAoDecidir()
  return useMutation({
    mutationFn: (form: FormularioDecisao) =>
      requisitar<Analise>(`/api/v1/coordenacao/certificados/${id}/decisao`, json(form)),
    onSuccess: (analise) => {
      cliente.setQueryData(chaves.analise(id), analise)
      aoDecidir()
    },
  })
}

/** A coordenação exclui um certificado do aluno (motivo obrigatório; o aluno é avisado). */
export function useExcluirPelaCoordenacao(id: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (form: { versao: number; justificativa: string }) =>
      requisitar<void>(`/api/v1/coordenacao/certificados/${id}/exclusao`, json(form)),
    onSuccess: () => {
      cliente.removeQueries({ queryKey: chaves.analise(id) })
      void cliente.invalidateQueries({ queryKey: chaves.coordenacao })
    },
  })
}

export function useDeferirEmLote() {
  const aoDecidir = useAoDecidir()
  return useMutation({
    mutationFn: (certificados: { id: string; versao: number }[]) =>
      requisitar<ResultadoLote[]>('/api/v1/coordenacao/certificados/deferimentos', json({ certificados })),
    onSuccess: aoDecidir,
  })
}

/** Quanto o certificado contaria com o item ou as horas corrigidos pela coordenação. */
export function useSimulacaoCoordenacao(id: string, form: Partial<FormularioSimulacao>) {
  const pronto = form.itemNumero != null && form.horasCertificado != null && !Number.isNaN(form.horasCertificado)
  return useQuery({
    queryKey: ['coordenacao', 'simulacao', id, form],
    queryFn: () => requisitar<Simulacao>(`/api/v1/coordenacao/certificados/${id}/simular`, json(form)),
    enabled: pronto,
    placeholderData: keepPreviousData,
    staleTime: 10_000,
  })
}

export function usePainel(filtros: { curso?: string; ingresso?: string }) {
  const consulta = new URLSearchParams(Object.entries(filtros).filter((e): e is [string, string] => !!e[1])).toString()
  return useQuery({
    queryKey: chaves.painel(filtros),
    queryFn: () => requisitar<Painel>(`/api/v1/coordenacao/painel${consulta ? `?${consulta}` : ''}`),
    placeholderData: keepPreviousData,
  })
}

export function useEquipe() {
  return useQuery({ queryKey: chaves.equipe, queryFn: () => requisitar<Equipe>('/api/v1/coordenacao/equipe') })
}

export function useCriarMembro() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (form: FormularioMembro) => requisitar<MembroEquipe>('/api/v1/coordenacao/equipe', json(form)),
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaves.equipe }),
  })
}

export function useAlunosDaCoordenacao() {
  return useQuery({ queryKey: chaves.alunos, queryFn: () => requisitar<AlunoNaCoordenacao[]>('/api/v1/coordenacao/alunos') })
}

export function useFichaAluno(id: string) {
  return useQuery({ queryKey: chaves.ficha(id), queryFn: () => requisitar<FichaAluno>(`/api/v1/coordenacao/alunos/${id}`) })
}

export function useDefinirIngresso(alunoId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (ingresso: { anoIngresso?: number; semestreIngresso?: number }) =>
      requisitar<FichaAluno>(`/api/v1/coordenacao/alunos/${alunoId}/ingresso`, json(ingresso, 'PUT')),
    onSuccess: (ficha) => {
      cliente.setQueryData(chaves.ficha(alunoId), ficha)
      void cliente.invalidateQueries({ queryKey: chaves.alunos })
      void cliente.invalidateQueries({ queryKey: ['coordenacao', 'painel'] })
    },
  })
}

export function useRequerimentos(alunoId: string) {
  return useQuery({
    queryKey: chaves.requerimentos(alunoId),
    queryFn: () => requisitar<Requerimento[]>(`/api/v1/coordenacao/alunos/${alunoId}/requerimentos`),
  })
}

export function useGerarRequerimento(alunoId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: () => requisitar<Requerimento>(`/api/v1/coordenacao/alunos/${alunoId}/requerimentos`, { method: 'POST' }),
    onSuccess: () => cliente.invalidateQueries({ queryKey: chaves.requerimentos(alunoId) }),
  })
}
