package br.edu.univassouras.sereno.comum;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Erro esperado do domínio, devolvido ao cliente como Problem Details (RFC 9457). O {@code codigo} é estável e
 * pode ser usado pelo frontend para escolher a mensagem.
 */
public class ErroDeNegocio extends ErrorResponseException {

    private ErroDeNegocio(HttpStatus status, String codigo, String mensagem) {
        super(status, corpo(status, codigo, mensagem), null);
    }

    private static ProblemDetail corpo(HttpStatus status, String codigo, String mensagem) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensagem);
        problema.setType(URI.create("urn:sereno:erro:" + codigo));
        problema.setProperty("codigo", codigo);
        return problema;
    }

    public String codigo() {
        return (String) getBody().getProperties().get("codigo");
    }

    public static ErroDeNegocio naoEncontrado(String mensagem) {
        return new ErroDeNegocio(HttpStatus.NOT_FOUND, "nao-encontrado", mensagem);
    }

    public static ErroDeNegocio conflito(String codigo, String mensagem) {
        return new ErroDeNegocio(HttpStatus.CONFLICT, codigo, mensagem);
    }

    public static ErroDeNegocio invalido(String codigo, String mensagem) {
        return new ErroDeNegocio(HttpStatus.UNPROCESSABLE_CONTENT, codigo, mensagem);
    }

    public static ErroDeNegocio proibido(String mensagem) {
        return new ErroDeNegocio(HttpStatus.FORBIDDEN, "proibido", mensagem);
    }
}
