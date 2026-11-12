package br.edu.univassouras.sereno.extracao;

/** Como o texto do certificado foi obtido. */
public enum MetodoExtracao {
    /** Texto embutido no PDF. */
    TEXTO,
    /** Reconhecimento de caracteres sobre a imagem (foto ou PDF escaneado). */
    OCR,
    /** Não havia texto e o OCR não está disponível ou não leu nada. */
    SEM_TEXTO,
    /** O arquivo não pôde ser lido (corrompido, protegido por senha...). */
    FALHA
}
