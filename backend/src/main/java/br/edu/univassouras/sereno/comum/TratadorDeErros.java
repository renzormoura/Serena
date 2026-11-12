package br.edu.univassouras.sereno.comum;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Erros de validação com o detalhe por campo, e conflitos de versão como 409. */
@RestControllerAdvice
class TratadorDeErros extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        // Só o nome do campo ("dados.itemNumero" → "itemNumero"), que é o que o formulário conhece.
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(
                e.getField().substring(e.getField().lastIndexOf('.') + 1), e.getDefaultMessage()));
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Há campos inválidos.");
        problema.setType(URI.create("urn:sereno:erro:validacao"));
        problema.setProperty("codigo", "validacao");
        problema.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail conflitoDeVersao() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Este registro foi alterado em outra janela. Recarregue a página e tente de novo.");
        problema.setType(URI.create("urn:sereno:erro:versao"));
        problema.setProperty("codigo", "versao");
        return problema;
    }
}
