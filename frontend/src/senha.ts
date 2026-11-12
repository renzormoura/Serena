/** Mesma regra do servidor: pelo menos 8 caracteres, uma letra e um caractere especial (não letra, número ou espaço). */
export function senhaAceita(senha: string) {
  return senha.length >= 8 && /\p{L}/u.test(senha) && /[^\p{L}\p{N}\s]/u.test(senha)
}
