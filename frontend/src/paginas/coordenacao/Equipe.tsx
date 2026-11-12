import { useState, type FormEvent } from 'react'
import { ErroApi, mensagemDeErro } from '../../api/cliente'
import { useCriarMembro, useEquipe } from '../../api/consultas'
import { useNotificar } from '../../componentes/notificar'
import { Alerta, Campo, Cartao, classeBotaoPrimario, classeBotaoSecundario, Entrada, Selecao } from '../../componentes/ui'
import { dataHora } from '../../formatos'
import { senhaAceita } from '../../senha'

const rotuloPapel = { COORDENADOR: 'Coordenação', ADMIN: 'Administração', ALUNO: 'Aluno' } as const

/**
 * Equipe da coordenação: quem analisa certificados. O coordenador cria coordenadores dos próprios cursos; a
 * administração cria coordenadores de qualquer curso e outros administradores.
 */
export function Equipe() {
  const { data: equipe, isPending, isError } = useEquipe()
  const criar = useCriarMembro()
  const notificar = useNotificar()
  const [aberto, setAberto] = useState(false)
  const [form, setForm] = useState({ nome: '', email: '', senha: '', papel: 'COORDENADOR' as 'COORDENADOR' | 'ADMIN' })
  const [cursos, setCursos] = useState<string[]>([])
  const [erroLocal, setErroLocal] = useState<Record<string, string>>({})

  const erroApi = criar.error instanceof ErroApi ? criar.error : undefined
  const campos: Record<string, string> = { ...(erroApi?.campos ?? {}), ...erroLocal }
  if (erroApi?.codigo === 'senha-fraca') campos.senha = 'Use pelo menos 8 caracteres, com uma letra e um caractere especial.'
  if (erroApi?.codigo === 'email-em-uso') campos.email = 'Já existe uma conta com este e-mail.'
  if (erroApi?.codigo === 'sem-curso') campos.cursoIds = 'Escolha pelo menos um curso.'
  const erroGeral = criar.error && Object.keys(campos).length === 0 ? mensagemDeErro(criar.error) : null

  function alterar(campo: keyof typeof form) {
    return (e: { target: { value: string } }) => setForm((f) => ({ ...f, [campo]: e.target.value }))
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    const erros: Record<string, string> = {}
    if (!senhaAceita(form.senha)) erros.senha = 'Use pelo menos 8 caracteres, com uma letra e um caractere especial.'
    if (form.papel === 'COORDENADOR' && cursos.length === 0) erros.cursoIds = 'Escolha pelo menos um curso.'
    setErroLocal(erros)
    if (Object.keys(erros).length > 0) return
    criar.mutate(
      { ...form, nome: form.nome.trim(), email: form.email.trim(), cursoIds: form.papel === 'COORDENADOR' ? cursos : [] },
      {
        onSuccess: (membro) => {
          notificar('Conta criada', `${membro.nome} já pode entrar com o e-mail ${membro.email} e a senha inicial.`)
          setForm({ nome: '', email: '', senha: '', papel: 'COORDENADOR' })
          setCursos([])
          setAberto(false)
        },
      },
    )
  }

  return (
    <div className="flex flex-col gap-7">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-3xl font-semibold">Equipe</h1>
          <p className="text-[15px] text-apagado">Contas da coordenação e da administração que analisam os certificados.</p>
        </div>
        {!aberto && (
          <button type="button" className={classeBotaoPrimario} onClick={() => setAberto(true)}>
            Nova conta
          </button>
        )}
      </header>

      {aberto && equipe && (
        <Cartao className="p-6 sm:p-7">
          <form onSubmit={enviar} noValidate className="flex flex-col gap-5">
            <h2 className="text-lg font-semibold">Nova conta da equipe</h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Campo id="membro-nome" rotulo="Nome" erro={campos.nome}>
                <Entrada id="membro-nome" erro={campos.nome} value={form.nome} onChange={alterar('nome')} />
              </Campo>
              <Campo id="membro-email" rotulo="E-mail" erro={campos.email}>
                <Entrada id="membro-email" type="email" erro={campos.email} value={form.email} onChange={alterar('email')} />
              </Campo>
              <Campo
                id="membro-senha"
                rotulo="Senha inicial"
                erro={campos.senha}
                dica="8 caracteres, uma letra e um especial. Passe à pessoa por um canal seguro."
              >
                <Entrada id="membro-senha" type="text" autoComplete="new-password" erro={campos.senha} value={form.senha} onChange={alterar('senha')} />
              </Campo>
              <Campo id="membro-papel" rotulo="Perfil" erro={campos.papel}>
                <Selecao id="membro-papel" value={form.papel} onChange={alterar('papel')}>
                  <option value="COORDENADOR">Coordenação (cursos escolhidos)</option>
                  {equipe.podeCriarAdmin && <option value="ADMIN">Administração (todos os cursos)</option>}
                </Selecao>
              </Campo>
            </div>

            {form.papel === 'COORDENADOR' && (
              <fieldset className="flex flex-col gap-2">
                <legend className="mb-2 text-sm font-medium">Cursos que vai coordenar</legend>
                <div className="flex flex-wrap gap-x-6 gap-y-2">
                  {equipe.cursosQuePodeAtribuir.map((c) => (
                    <label key={c.id} className="flex items-center gap-2 text-sm">
                      <input
                        type="checkbox"
                        className="size-4 accent-[var(--color-bordo)]"
                        checked={cursos.includes(c.id)}
                        onChange={(e) => setCursos((atual) => (e.target.checked ? [...atual, c.id] : atual.filter((id) => id !== c.id)))}
                      />
                      {c.nome}
                    </label>
                  ))}
                </div>
                {!equipe.podeCriarAdmin && (
                  <span className="text-[13px] text-apagado">Você só pode dar acesso aos cursos que coordena.</span>
                )}
                {campos.cursoIds && <span className="text-[13px] text-bordo">{campos.cursoIds}</span>}
              </fieldset>
            )}

            {erroGeral && <Alerta>{erroGeral}</Alerta>}
            <div className="flex gap-3">
              <button type="submit" className={classeBotaoPrimario} disabled={criar.isPending}>
                {criar.isPending ? 'Criando…' : 'Criar conta'}
              </button>
              <button type="button" className={classeBotaoSecundario} onClick={() => setAberto(false)}>
                Cancelar
              </button>
            </div>
          </form>
        </Cartao>
      )}

      {isError && <Alerta>Não foi possível carregar a equipe.</Alerta>}
      {isPending && <p className="text-apagado">Carregando…</p>}
      {equipe && (
        <Cartao className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead className="border-b border-linha text-xs text-apagado">
              <tr>
                <th scope="col" className="px-5 py-3 font-medium">Nome</th>
                <th scope="col" className="px-5 py-3 font-medium">Perfil</th>
                <th scope="col" className="px-5 py-3 font-medium">Cursos</th>
                <th scope="col" className="px-5 py-3 font-medium">Criada em</th>
              </tr>
            </thead>
            <tbody>
              {equipe.membros.map((m) => (
                <tr key={m.id} className="border-b border-[#F1ECED] last:border-b-0">
                  <td className="px-5 py-3.5">
                    <span className="block font-semibold">{m.nome}</span>
                    <span className="text-xs text-apagado">{m.email}</span>
                  </td>
                  <td className="px-5 py-3.5">{rotuloPapel[m.papel]}</td>
                  <td className="px-5 py-3.5 text-tinta-media">
                    {m.papel === 'ADMIN' ? 'Todos' : m.cursos.length > 0 ? m.cursos.map((c) => c.nome.replace('Engenharia ', '')).join(', ') : '—'}
                  </td>
                  <td className="px-5 py-3.5 whitespace-nowrap text-apagado">{dataHora(m.criadoEm)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Cartao>
      )}
    </div>
  )
}
