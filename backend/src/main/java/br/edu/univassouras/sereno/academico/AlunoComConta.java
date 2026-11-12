package br.edu.univassouras.sereno.academico;

/** O aluno com o nome e o e-mail da conta, para as telas da coordenação. */
public record AlunoComConta(AlunoResumo aluno, String nome, String email) {
}
