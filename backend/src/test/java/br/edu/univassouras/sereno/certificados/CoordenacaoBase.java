package br.edu.univassouras.sereno.certificados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import br.edu.univassouras.sereno.IntegracaoBase;
import br.edu.univassouras.sereno.identidade.ContasService;
import br.edu.univassouras.sereno.identidade.Papel;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Auxiliares dos testes da área da coordenação. */
abstract class CoordenacaoBase extends IntegracaoBase {

    /** Conta local criada pelo perfil dev e vinculada ao curso de Engenharia. */
    static final String COORDENACAO = "coordenacao@dev.local";
    static final String SENHA_DEV = "sereno-dev";

    @Autowired
    ContasService contas;

    record Lancado(String id, long versao) {
    }

    /** Lança um certificado já conferido pelo aluno: vai direto para a fila da coordenação. */
    Lancado lancar(Cookie aluno, int item, String horas) throws Exception {
        String marca = UUID.randomUUID().toString();
        String dados = """
                {"titulo":"Atividade %s","instituicao":"Universidade de Vassouras","horasCertificado":%s,"itemNumero":%d}"""
                .formatted(marca.substring(0, 8), horas, item);
        var resposta = mvc.post().uri("/api/v1/certificados").multipart()
                .file(new MockMultipartFile("arquivo", "c.pdf", "application/pdf",
                        ("%PDF-1.4\n% " + marca + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII)))
                .file(new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE, dados.getBytes(StandardCharsets.UTF_8)))
                .with(csrf()).cookie(aluno)
                .exchange();
        assertThat(resposta).hasStatus(HttpStatus.CREATED);
        String corpo = corpo(resposta);
        return new Lancado(JsonPath.read(corpo, "$.id"), JsonPath.<Integer>read(corpo, "$.versao"));
    }

    MvcTestResult decidir(Cookie sessao, String id, long versao, String extra) {
        return mvc.post().uri("/api/v1/coordenacao/certificados/" + id + "/decisao").with(csrf()).cookie(sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"versao\":%d,%s}".formatted(versao, extra))
                .exchange();
    }

    Cookie coordenacao() {
        return entrar(COORDENACAO, SENHA_DEV);
    }

    static String corpo(MvcTestResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    static long versao(MvcTestResult r) throws Exception {
        return JsonPath.<Integer>read(corpo(r), "$.certificado.versao");
    }
}
