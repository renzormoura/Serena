package br.edu.univassouras.sereno.certificados;

/** O que a coordenação decidiu sobre um certificado. Fica no histórico (tabela {@code avaliacao}). */
public enum TipoAvaliacao {
    DEFERIDO,
    INDEFERIDO,
    AJUSTE_SOLICITADO,
    /** Um deferimento ou indeferimento desfeito: o certificado voltou para a fila. */
    REABERTO,
    /** A coordenação excluiu o certificado (ex.: enviado por engano, documento inválido). */
    EXCLUIDO
}
