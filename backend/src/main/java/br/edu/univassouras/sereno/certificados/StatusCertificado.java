package br.edu.univassouras.sereno.certificados;

import java.util.EnumSet;
import java.util.Set;

/** Ciclo de vida do certificado (PLANO-DO-PROJETO.md §5.1). */
public enum StatusCertificado {
    /** Arquivo recebido; a leitura automática está rodando. */
    PROCESSANDO,
    /** Leitura terminada, esperando o aluno conferir. */
    AGUARDANDO_CONFIRMACAO,
    /** Conferido pelo aluno, aguardando a coordenação. */
    CONFIRMADO,
    /** A coordenação pediu correção ao aluno. */
    AJUSTE_SOLICITADO,
    DEFERIDO,
    INDEFERIDO;

    private static final Set<StatusCertificado> EDITAVEIS = EnumSet.of(AGUARDANDO_CONFIRMACAO, CONFIRMADO, AJUSTE_SOLICITADO);
    private static final Set<StatusCertificado> EXCLUIVEIS = EnumSet.of(PROCESSANDO, AGUARDANDO_CONFIRMACAO, CONFIRMADO, AJUSTE_SOLICITADO);

    /** O aluno pode alterar os dados (durante a leitura, não: o resultado sobrescreveria a edição). */
    public boolean editavelPeloAluno() {
        return EDITAVEIS.contains(this);
    }

    public boolean excluivelPeloAluno() {
        return EXCLUIVEIS.contains(this);
    }
}
