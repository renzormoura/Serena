package br.edu.univassouras.sereno.calculo;

/**
 * Como um certificado de um item se converte em horas antes de aplicar os limites.
 */
public enum EstrategiaCalculo {

    /** Cada certificado vale o valor fixo do item, qualquer que seja a carga do certificado (regra da planilha atual). */
    FIXO_POR_OCORRENCIA,

    /** Vale a carga horária informada no certificado. */
    HORAS_CERTIFICADO,

    /** Vale a carga do certificado, limitada ao teto por ocorrência do item. */
    HORAS_CERTIFICADO_COM_TETO
}
