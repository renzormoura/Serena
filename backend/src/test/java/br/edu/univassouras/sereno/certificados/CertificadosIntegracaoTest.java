package br.edu.univassouras.sereno.certificados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import br.edu.univassouras.sereno.IntegracaoBase;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class CertificadosIntegracaoTest extends IntegracaoBase {

    private static int contador;

    /** Um PDF mínimo; o sufixo torna cada arquivo único (hash diferente). */
    private static byte[] pdf(String marca) {
        return ("%PDF-1.4\n% " + marca + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII);
    }

    private MvcTestResult lancar(Cookie sessao, int item, String horas, byte[] conteudo) {
        return lancar(sessao, item, horas, conteudo, null);
    }

    private MvcTestResult lancar(Cookie sessao, int item, String horas, byte[] conteudo, LocalDate data) {
        String dados = """
                {"titulo":"Atividade %d","instituicao":"Universidade de Vassouras","dataInicio":%s,
                 "horasCertificado":%s,"itemNumero":%d}"""
                .formatted(++contador, data == null ? "null" : "\"" + data + "\"", horas, item);
        return mvc.post().uri("/api/v1/certificados").multipart()
                .file(new MockMultipartFile("arquivo", "certificado.pdf", "application/pdf", conteudo))
                .file(new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE, dados.getBytes(StandardCharsets.UTF_8)))
                .with(csrf()).cookie(sessao)
                .exchange();
    }

    private static String id(MvcTestResult resultado) throws java.io.UnsupportedEncodingException {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
    }

    @Test
    void lancaCertificadoQueJaNasceConfirmadoEContaOValorDoItem() throws Exception {
        Conta aluno = novoAluno();

        var resultado = lancar(aluno.sessao(), 10, "70", pdf("gerente-agil"));

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        assertThat(resultado).bodyJson().extractingPath("$.status").isEqualTo("CONFIRMADO");
        assertThat(resultado).bodyJson().extractingPath("$.horasQueContam").isEqualTo(20.0);
        assertThat(resultado).bodyJson().extractingPath("$.item.nome").isEqualTo("CURSOS LIVRES");
        assertThat(resultado).bodyJson().extractingPath("$.arquivo.nome").isEqualTo("certificado.pdf");
    }

    @Test
    void osTrezeCertificadosDaPlanilhaSomam430HorasPendentes() {
        Conta aluno = novoAluno();
        int[][] linhas = {{10, 70}, {10, 32}, {10, 29}, {44, 8}, {4, 3}, {19, 20}, {24, 16},
                {24, 3}, {24, 3}, {24, 3}, {24, 3}, {44, 8}, {18, 50}};
        for (int i = 0; i < linhas.length; i++) {
            assertThat(lancar(aluno.sessao(), linhas[i][0], String.valueOf(linhas[i][1]), pdf("linha-" + i)))
                    .hasStatus(HttpStatus.CREATED);
        }

        var saldo = mvc.get().uri("/api/v1/alunos/me/saldo").cookie(aluno.sessao()).exchange();

        // Sem deferimento da coordenação (fase F4), tudo conta como pendente.
        assertThat(saldo).hasStatusOk();
        assertThat(saldo).bodyJson().extractingPath("$.projecao").isEqualTo(430.0);
        assertThat(saldo).bodyJson().extractingPath("$.pendentes").isEqualTo(430.0);
        assertThat(saldo).bodyJson().extractingPath("$.integralizadas").isEqualTo(0);
        assertThat(saldo).bodyJson().extractingPath("$.certificadosPorStatus.CONFIRMADO").isEqualTo(13);
        assertThat(saldo).bodyJson().extractingPath("$.faltantes").isNull();
    }

    @Test
    void recusaArquivoRepetidoEArquivoQueNaoEhPdfNemImagem() throws Exception {
        Conta aluno = novoAluno();
        byte[] conteudo = pdf("repetido");
        assertThat(lancar(aluno.sessao(), 22, "2", conteudo)).hasStatus(HttpStatus.CREATED);

        assertThat(lancar(aluno.sessao(), 22, "2", conteudo))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("arquivo-duplicado");

        // Extensão .pdf não basta: o conteúdo é verificado.
        assertThat(lancar(aluno.sessao(), 22, "2", "<html>falso</html>".getBytes(StandardCharsets.UTF_8)))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("arquivo-tipo");
    }

    @Test
    void recusaItemInexistenteEDataNoFuturo() throws Exception {
        Conta aluno = novoAluno();

        assertThat(lancar(aluno.sessao(), 99, "2", pdf("item-99")))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("item-invalido");
        assertThat(lancar(aluno.sessao(), 22, "2", pdf("futuro"), LocalDate.now().plusDays(10)))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("data-futura");
    }

    @Test
    void outroAlunoNaoEnxergaECoordenacaoNaoUsaRotasDeAluno() throws Exception {
        Conta dono = novoAluno();
        Conta intruso = novoAluno();
        String certificado = id(lancar(dono.sessao(), 18, "50", pdf("monitoria")));

        assertThat(mvc.get().uri("/api/v1/certificados/" + certificado).cookie(intruso.sessao()))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/v1/certificados/" + certificado + "/arquivo").cookie(intruso.sessao()))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/v1/certificados/" + certificado).with(csrf()).cookie(intruso.sessao()))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/v1/certificados").cookie(intruso.sessao()))
                .hasStatusOk().bodyJson().extractingPath("$.length()").isEqualTo(0);

        Cookie coordenacao = entrar("coordenacao@dev.local", "sereno-dev");
        assertThat(mvc.get().uri("/api/v1/certificados").cookie(coordenacao)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void editarTrocaOItemEVersaoAntigaDaConflito() throws Exception {
        Conta aluno = novoAluno();
        String certificado = id(lancar(aluno.sessao(), 22, "2", pdf("palestra")));
        String edicao = """
                {"versao":%d,"dados":{"titulo":"Palestra externa","horasCertificado":2,"itemNumero":48}}""";

        assertThat(mvc.put().uri("/api/v1/certificados/" + certificado).with(csrf()).cookie(aluno.sessao())
                .contentType(MediaType.APPLICATION_JSON).content(edicao.formatted(0)))
                .hasStatusOk()
                .bodyJson().extractingPath("$.item.numero").isEqualTo(48);

        assertThat(mvc.put().uri("/api/v1/certificados/" + certificado).with(csrf()).cookie(aluno.sessao())
                .contentType(MediaType.APPLICATION_JSON).content(edicao.formatted(0)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("versao");
    }

    @Test
    void excluirTiraDaListaELiberaOMesmoArquivo() throws Exception {
        Conta aluno = novoAluno();
        byte[] conteudo = pdf("excluir");
        String certificado = id(lancar(aluno.sessao(), 22, "2", conteudo));

        assertThat(mvc.delete().uri("/api/v1/certificados/" + certificado).with(csrf()).cookie(aluno.sessao()))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/v1/certificados").cookie(aluno.sessao()))
                .bodyJson().extractingPath("$.length()").isEqualTo(0);
        assertThat(lancar(aluno.sessao(), 22, "2", conteudo)).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void baixaOArquivoOriginal() throws Exception {
        Conta aluno = novoAluno();
        byte[] conteudo = pdf("download");
        String certificado = id(lancar(aluno.sessao(), 22, "2", conteudo));

        var resposta = mvc.get().uri("/api/v1/certificados/" + certificado + "/arquivo").cookie(aluno.sessao()).exchange();

        assertThat(resposta).hasStatusOk().hasContentType(MediaType.APPLICATION_PDF);
        assertThat(resposta.getResponse().getContentAsByteArray()).isEqualTo(conteudo);
        assertThat(resposta.getResponse().getHeader("Content-Disposition")).startsWith("inline");
    }

    @Test
    void simulacaoMostraQuantoUmCertificadoContaria() throws Exception {
        Conta aluno = novoAluno();

        assertThat(mvc.post().uri("/api/v1/certificados/simular").with(csrf()).cookie(aluno.sessao())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"itemNumero":24,"horasCertificado":3}"""))
                .hasStatusOk()
                .bodyJson().extractingPath("$.horasQueContam").isEqualTo(50.0);
    }
}
