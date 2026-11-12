package br.edu.univassouras.sereno.notificacao;

import br.edu.univassouras.sereno.certificados.CertificadoAvaliado;
import br.edu.univassouras.sereno.certificados.TipoAvaliacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Avisa o aluno quando precisa agir (ajuste solicitado) ou quando um certificado é indeferido. Deferimentos não
 * geram e-mail, para não lotar a caixa do aluno num deferimento em lote; ele vê no painel.
 *
 * <p>Roda depois do commit, em segundo plano, pelo registro de eventos do Spring Modulith: se o servidor de e-mail
 * falhar, o aviso fica pendente e é reenviado quando a aplicação subir de novo.
 */
@Component
class AvisosPorEmail {

    private static final Logger log = LoggerFactory.getLogger(AvisosPorEmail.class);

    private final ObjectProvider<JavaMailSender> carteiro;
    private final boolean ativo;
    private final String remetente;
    private final String enderecoDoSistema;

    AvisosPorEmail(ObjectProvider<JavaMailSender> carteiro,
            @Value("${sereno.notificacao.email-ativo:false}") boolean ativo,
            @Value("${sereno.notificacao.remetente:Sereno <nao-responda@univassouras.edu.br>}") String remetente,
            @Value("${sereno.notificacao.url-base:http://localhost:5173}") String enderecoDoSistema) {
        this.carteiro = carteiro;
        this.ativo = ativo;
        this.remetente = remetente;
        this.enderecoDoSistema = enderecoDoSistema.replaceAll("/+$", "");
    }

    @ApplicationModuleListener
    void aoAvaliar(CertificadoAvaliado evento) {
        if (evento.decisao() != TipoAvaliacao.AJUSTE_SOLICITADO && evento.decisao() != TipoAvaliacao.INDEFERIDO
                && evento.decisao() != TipoAvaliacao.EXCLUIDO) {
            return;
        }
        SimpleMailMessage mensagem = mensagem(evento);
        JavaMailSender envio = carteiro.getIfAvailable();
        if (!ativo || envio == null) {
            log.info("E-mail desligado; aviso não enviado a {}: {}", evento.emailAluno(), mensagem.getSubject());
            return;
        }
        envio.send(mensagem);
        log.debug("Aviso enviado a {}: {}", evento.emailAluno(), mensagem.getSubject());
    }

    SimpleMailMessage mensagem(CertificadoAvaliado evento) {
        boolean ajuste = evento.decisao() == TipoAvaliacao.AJUSTE_SOLICITADO;
        boolean excluido = evento.decisao() == TipoAvaliacao.EXCLUIDO;
        String link = enderecoDoSistema + "/certificados/" + evento.certificadoId();
        String motivo = evento.justificativa() == null ? "" : "\n\nMensagem da coordenação:\n" + evento.justificativa();
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(remetente);
        m.setTo(evento.emailAluno());
        m.setSubject(ajuste
                ? "Ajuste solicitado no certificado \"" + evento.tituloCertificado() + "\""
                : excluido
                        ? "Certificado excluído: \"" + evento.tituloCertificado() + "\""
                        : "Certificado reprovado: \"" + evento.tituloCertificado() + "\"");
        m.setText("Olá, " + evento.nomeAluno() + ".\n\n"
                + (ajuste
                        ? "A coordenação pediu um ajuste no certificado \"" + evento.tituloCertificado() + "\". "
                                + "Corrija e salve de novo para ele voltar à análise."
                        : excluido
                                ? "A coordenação excluiu o certificado \"" + evento.tituloCertificado() + "\". Ele não aparece mais na sua lista."
                                : "A coordenação reprovou o certificado \"" + evento.tituloCertificado() + "\". Ele não conta nas suas horas.")
                + motivo
                + "\n\nVer o certificado: " + link
                + "\n\n— Sereno, horas complementares das Engenharias da Universidade de Vassouras");
        return m;
    }
}
