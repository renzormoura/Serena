package br.edu.univassouras.sereno.certificados;

import java.util.UUID;

/**
 * Evento de domínio: a coordenação decidiu sobre um certificado. Usado para avisar o aluno por e-mail.
 *
 * @param justificativa o que a coordenação escreveu ao aluno; pode ser nula num deferimento simples
 */
public record CertificadoAvaliado(UUID certificadoId, TipoAvaliacao decisao, String tituloCertificado,
        String justificativa, UUID usuarioAluno, String nomeAluno, String emailAluno) {
}
