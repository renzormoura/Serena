package br.edu.univassouras.sereno.notificacao;

import br.edu.univassouras.sereno.certificados.CertificadoAvaliado;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Cria um aviso no site para o aluno a cada decisão da coordenação (validado, reprovado, ajuste pedido, reaberto),
 * com a mensagem que a coordenação escreveu. Roda depois do commit, como o e-mail.
 */
@Component
class AvisosNoSite {

    private final AvisoRepository avisos;

    AvisosNoSite(AvisoRepository avisos) {
        this.avisos = avisos;
    }

    @ApplicationModuleListener
    void aoAvaliar(CertificadoAvaliado evento) {
        String certificado = "\"" + evento.tituloCertificado() + "\"";
        String motivo = evento.justificativa() == null ? "" : " Mensagem da coordenação: " + evento.justificativa();
        Aviso aviso = switch (evento.decisao()) {
            case DEFERIDO -> new Aviso(evento.usuarioAluno(), "Certificado validado",
                    "A coordenação validou o certificado " + certificado + ". As horas já contam no seu saldo." + motivo,
                    link(evento));
            case INDEFERIDO -> new Aviso(evento.usuarioAluno(), "Certificado reprovado",
                    "A coordenação reprovou o certificado " + certificado + ". Ele não conta horas." + motivo, link(evento));
            case AJUSTE_SOLICITADO -> new Aviso(evento.usuarioAluno(), "Ajuste pedido",
                    "A coordenação pediu um ajuste no certificado " + certificado + ". Corrija e salve para voltar à análise."
                            + motivo, link(evento));
            case EXCLUIDO -> new Aviso(evento.usuarioAluno(), "Certificado excluído pela coordenação",
                    "A coordenação excluiu o certificado " + certificado + ". Ele não aparece mais na sua lista nem conta horas."
                            + motivo, null);
            case REABERTO -> new Aviso(evento.usuarioAluno(), "Certificado em análise de novo",
                    "O certificado " + certificado + " voltou para a análise da coordenação." + motivo, link(evento));
        };
        avisos.save(aviso);
    }

    private static String link(CertificadoAvaliado evento) {
        return "/certificados/" + evento.certificadoId();
    }
}
