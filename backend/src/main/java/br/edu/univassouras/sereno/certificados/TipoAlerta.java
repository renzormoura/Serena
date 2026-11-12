package br.edu.univassouras.sereno.certificados;

/**
 * Alertas da triagem automática (Fase 5.1). A triagem só aponta: quem valida ou reprova é sempre a coordenação.
 */
public enum TipoAlerta {
    NOME_DIFERENTE(Gravidade.FORTE),
    ARQUIVO_DE_OUTRA_CONTA(Gravidade.FORTE),
    CERTIFICADO_REPETIDO(Gravidade.FORTE),
    HORAS_DIFERENTES(Gravidade.FORTE),
    ITEM_TROCADO(Gravidade.ATENCAO),
    ARQUIVO_ILEGIVEL(Gravidade.ATENCAO),
    DATA_SUSPEITA(Gravidade.ATENCAO),
    HORAS_FORA_DO_COMUM(Gravidade.ATENCAO),
    LIMITE_ATINGIDO(Gravidade.ATENCAO),
    NAO_PARECE_CERTIFICADO(Gravidade.ATENCAO),
    /** Gera falsos positivos (muitos certificados legítimos passam por editores): só informação. */
    PDF_EDITADO(Gravidade.INFORMACAO);

    public enum Gravidade {
        /** Vai para o topo da fila. */
        FORTE,
        /** Pede um olhar, mas é comum ter explicação simples. */
        ATENCAO,
        /** Não tira o certificado do grupo "sem alertas". */
        INFORMACAO
    }

    private final Gravidade gravidade;

    TipoAlerta(Gravidade gravidade) {
        this.gravidade = gravidade;
    }

    public Gravidade gravidade() {
        return gravidade;
    }
}
