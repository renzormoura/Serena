package br.edu.univassouras.sereno.certificados;

import java.util.UUID;

/**
 * Evento de domínio: um arquivo chegou e precisa ser lido. Guardado no registro de eventos do Spring Modulith
 * junto com a transação do envio, então a leitura acontece mesmo se a aplicação reiniciar no meio.
 *
 * @param nomeAluno usado para conferir se o certificado é do aluno
 */
public record CertificadoEnviado(UUID certificadoId, String nomeAluno) {
}
