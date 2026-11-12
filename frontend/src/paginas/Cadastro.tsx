import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { ErroApi, mensagemDeErro } from '../api/cliente'
import { useCadastrar, useCursos, useEntrar, useUsuarioAtual } from '../api/consultas'
import { Marca } from '../componentes/Marca'
import { senhaAceita } from '../senha'
import { Alerta, Campo, classeBotaoPrimario, Entrada, Selecao } from '../componentes/ui'


export function Cadastro() {
  const { data: usuario } = useUsuarioAtual()
  const { data: cursos } = useCursos()
  const cadastrar = useCadastrar()
  const entrar = useEntrar()
  const navegar = useNavigate()
  const [form, setForm] = useState({
    nome: '',
    email: '',
    senha: '',
    matricula: '',
    cursoId: '',
  })
  const [senhaFraca, setSenhaFraca] = useState(false)

  if (usuario) return <Navigate to="/" replace />

  const erroApi = cadastrar.error instanceof ErroApi ? cadastrar.error : undefined
  const campos: Record<string, string> = { ...(erroApi?.campos ?? {}) }
  if (senhaFraca || erroApi?.codigo === 'senha-fraca') {
    campos.senha = 'Use pelo menos 8 caracteres, com uma letra e um caractere especial.'
  }
  const erroGeral =
    cadastrar.error && Object.keys(campos).length === 0 ? mensagemDeErro(cadastrar.error) : null
  const cursoId = form.cursoId || cursos?.[0]?.id || ''

  function alterar(campo: keyof typeof form) {
    return (e: { target: { value: string } }) => setForm((f) => ({ ...f, [campo]: e.target.value }))
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (!senhaAceita(form.senha)) {
      setSenhaFraca(true)
      return
    }
    setSenhaFraca(false)
    const dados = {
      ...form,
      nome: form.nome.trim(),
      email: form.email.trim(),
      matricula: form.matricula.trim(),
      cursoId,
    }
    cadastrar.mutate(dados, {
      onSuccess: () =>
        entrar.mutate({ email: dados.email, senha: dados.senha }, { onSuccess: () => navegar('/', { replace: true }) }),
    })
  }

  return (
    <div className="flex min-h-screen flex-col items-center bg-fundo px-4 py-10 sm:py-16">
      <div className="mb-10">
        <Marca />
      </div>
      <form
        onSubmit={enviar}
        noValidate
        className="flex w-full max-w-[520px] flex-col gap-6 rounded-2xl border border-linha bg-white p-6 sm:p-10"
      >
        <div className="flex flex-col gap-2">
          <h1 className="text-[28px] font-semibold">Criar conta</h1>
          <p className="text-[15px] text-apagado">Para alunos das Engenharias da Universidade de Vassouras.</p>
        </div>

        <Campo id="nome" rotulo="Nome completo" erro={campos.nome}>
          <Entrada id="nome" erro={campos.nome} autoComplete="name" value={form.nome} onChange={alterar('nome')} />
        </Campo>
        <Campo id="email" rotulo="E-mail" erro={campos.email}>
          <Entrada
            id="email"
            type="email"
            autoComplete="email"
            erro={campos.email}
            value={form.email}
            onChange={alterar('email')}
          />
        </Campo>
        <Campo id="senha" rotulo="Senha" erro={campos.senha} dica="Pelo menos 8 caracteres, com uma letra e um caractere especial (ex.: ! @ # -).">
          <Entrada
            id="senha"
            type="password"
            autoComplete="new-password"
            erro={campos.senha}
            value={form.senha}
            onChange={alterar('senha')}
          />
        </Campo>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Campo id="matricula" rotulo="Matrícula" erro={campos.matricula}>
            <Entrada id="matricula" erro={campos.matricula} value={form.matricula} onChange={alterar('matricula')} />
          </Campo>
          <Campo id="curso" rotulo="Curso" erro={campos.cursoId}>
            <Selecao id="curso" erro={campos.cursoId} value={cursoId} onChange={alterar('cursoId')}>
              {cursos?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nome}
                </option>
              ))}
            </Selecao>
          </Campo>
        </div>

        {erroGeral && <Alerta>{erroGeral}</Alerta>}

        <button type="submit" disabled={cadastrar.isPending || entrar.isPending} className={classeBotaoPrimario}>
          {cadastrar.isPending ? 'Criando conta…' : 'Criar conta'}
        </button>
        <p className="text-center text-sm text-apagado">
          Já tem conta?{' '}
          <Link to="/login" className="font-semibold text-bordo">
            Entrar
          </Link>
        </p>
      </form>
    </div>
  )
}
