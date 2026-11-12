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

class CoordenacaoIntegracaoTest extends CoordenacaoBase {

    @Test
    void coordenacaoVeNaFilaDefereEAsHorasViramIntegralizadas() throws Exception {
        Conta aluno = novoAluno();
        Lancado certificado = lancar(aluno.sessao(), 18, "60");
        Cookie coord = coordenacao();

        var fila = mvc.get().uri("/api/v1/coordenacao/fila").cookie(coord).exchange();
        assertThat(fila).hasStatusOk();
        List<String> naFila = JsonPath.read(corpo(fila), "$[*].certificado.id");
        assertThat(naFila).contains(certificado.id());
        assertThat(JsonPath.<List<String>>read(corpo(fila), "$[?(@.certificado.id == '" + certificado.id() + "')].aluno.matricula"))
                .containsExactly(aluno.matricula());

        var deferido = decidir(coord, certificado.id(), certificado.versao(), "\"decisao\":\"DEFERIR\"");
        assertThat(deferido).hasStatusOk();
        assertThat(deferido).bodyJson().extractingPath("$.certificado.status").isEqualTo("DEFERIDO");
        assertThat(deferido).bodyJson().extractingPath("$.historico[0].decisao").isEqualTo("DEFERIDO");
        assertThat(deferido).bodyJson().extractingPath("$.historico[0].avaliador").isEqualTo("Coordenação (dev)");

        // Para o aluno: integralizado, e não pode mais editar nem excluir.
        assertThat(mvc.get().uri("/api/v1/alunos/me/saldo").cookie(aluno.sessao()))
                .bodyJson().extractingPath("$.integralizadas").isEqualTo(50.0);
        var visto = mvc.get().uri("/api/v1/certificados/" + certificado.id()).cookie(aluno.sessao()).exchange();
        assertThat(visto).bodyJson().extractingPath("$.editavel").isEqualTo(false);
        assertThat(visto).bodyJson().extractingPath("$.excluivel").isEqualTo(false);

        // Sai da fila.
        assertThat(JsonPath.<List<String>>read(corpo(mvc.get().uri("/api/v1/coordenacao/fila").cookie(coord).exchange()),
                "$[*].certificado.id")).doesNotContain(certificado.id());

        // Reabrir exige justificativa e devolve à fila.
        long v = versao(deferido);
        assertThat(decidir(coord, certificado.id(), v, "\"decisao\":\"REABRIR\"")).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("justificativa-obrigatoria");
        assertThat(decidir(coord, certificado.id(), v, "\"decisao\":\"REABRIR\",\"justificativa\":\"Deferido por engano\""))
                .hasStatusOk().bodyJson().extractingPath("$.certificado.status").isEqualTo("CONFIRMADO");
    }

    @Test
    void ajusteSolicitadoVoltaParaOAlunoComOMotivoEDepoisParaAFila() throws Exception {
        Conta aluno = novoAluno();
        Lancado certificado = lancar(aluno.sessao(), 21, "4");
        Cookie coord = coordenacao();

        assertThat(decidir(coord, certificado.id(), certificado.versao(), "\"decisao\":\"SOLICITAR_AJUSTE\""))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        var ajuste = decidir(coord, certificado.id(), certificado.versao(),
                "\"decisao\":\"SOLICITAR_AJUSTE\",\"justificativa\":\"Envie o certificado com a carga horária visível.\"");
        assertThat(ajuste).hasStatusOk().bodyJson().extractingPath("$.certificado.status").isEqualTo("AJUSTE_SOLICITADO");

        var visto = mvc.get().uri("/api/v1/certificados/" + certificado.id()).cookie(aluno.sessao()).exchange();
        String corpo = corpo(visto);
        assertThat(JsonPath.<String>read(corpo, "$.observacaoCoordenacao")).isEqualTo("Envie o certificado com a carga horária visível.");
        assertThat(JsonPath.<Boolean>read(corpo, "$.editavel")).isTrue();

        // O aluno corrige e salva: volta para a fila.
        assertThat(mvc.put().uri("/api/v1/certificados/" + certificado.id()).with(csrf()).cookie(aluno.sessao())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"versao":%d,"dados":{"titulo":"Oficina corrigida","horasCertificado":6,"itemNumero":21}}"""
                        .formatted(JsonPath.<Integer>read(corpo, "$.versao"))))
                .hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("CONFIRMADO");
        assertThat(JsonPath.<List<String>>read(corpo(mvc.get().uri("/api/v1/coordenacao/fila").cookie(coord).exchange()),
                "$[*].certificado.id")).contains(certificado.id());
    }

    @Test
    void indeferirExigeJustificativaENaoContaNada() throws Exception {
        Conta aluno = novoAluno();
        Lancado certificado = lancar(aluno.sessao(), 10, "20");
        Cookie coord = coordenacao();

        assertThat(decidir(coord, certificado.id(), certificado.versao(), "\"decisao\":\"INDEFERIR\",\"justificativa\":\"  \""))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(decidir(coord, certificado.id(), certificado.versao(),
                "\"decisao\":\"INDEFERIR\",\"justificativa\":\"Curso sem relação com a engenharia.\""))
                .hasStatusOk().bodyJson().extractingPath("$.certificado.status").isEqualTo("INDEFERIDO");

        var saldo = mvc.get().uri("/api/v1/alunos/me/saldo").cookie(aluno.sessao()).exchange();
        assertThat(saldo).bodyJson().extractingPath("$.projecao").isEqualTo(0);
        assertThat(saldo).bodyJson().extractingPath("$.certificadosPorStatus.INDEFERIDO").isEqualTo(1);
    }

    @Test
    void corrigirItemAoDeferirExigeJustificativaEFicaNoHistorico() throws Exception {
        Conta aluno = novoAluno();
        // O aluno pôs um curso de plataforma externa em "Cursos livres" (10); o certo é o 46.
        Lancado certificado = lancar(aluno.sessao(), 10, "40");
        Cookie coord = coordenacao();

        assertThat(decidir(coord, certificado.id(), certificado.versao(), "\"decisao\":\"DEFERIR\",\"itemNumero\":46"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("justificativa-obrigatoria");
        assertThat(decidir(coord, certificado.id(), certificado.versao(), "\"decisao\":\"DEFERIR\",\"itemNumero\":99,\"justificativa\":\"x\""))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("item-invalido");

        var deferido = decidir(coord, certificado.id(), certificado.versao(),
                "\"decisao\":\"DEFERIR\",\"itemNumero\":46,\"justificativa\":\"Curso de plataforma externa é o item 46.\"");
        assertThat(deferido).hasStatusOk();
        assertThat(deferido).bodyJson().extractingPath("$.certificado.item.numero").isEqualTo(46);
        assertThat(deferido).bodyJson().extractingPath("$.historico[0].itemAnterior").isEqualTo(10);
        assertThat(deferido).bodyJson().extractingPath("$.historico[0].itemNovo").isEqualTo(46);
        assertThat(mvc.get().uri("/api/v1/alunos/me/saldo").cookie(aluno.sessao()))
                .bodyJson().extractingPath("$.integralizadas").isEqualTo(30.0);
    }

    @Test
    void versaoDesatualizadaEhRecusadaEDecidirDuasVezesNao() throws Exception {
        Conta aluno = novoAluno();
        Lancado certificado = lancar(aluno.sessao(), 21, "4");
        Cookie coord = coordenacao();

        assertThat(decidir(coord, certificado.id(), certificado.versao() + 7, "\"decisao\":\"DEFERIR\""))
                .hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.codigo").isEqualTo("versao");
        var deferido = decidir(coord, certificado.id(), certificado.versao(), "\"decisao\":\"DEFERIR\"");
        assertThat(decidir(coord, certificado.id(), versao(deferido), "\"decisao\":\"INDEFERIR\",\"justificativa\":\"x\""))
                .hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.codigo").isEqualTo("status-invalido");
    }

    @Test
    void deferimentoEmLoteTrataCadaCertificadoAParte() throws Exception {
        Conta aluno = novoAluno();
        Lancado a = lancar(aluno.sessao(), 21, "4");
        Lancado b = lancar(aluno.sessao(), 22, "2");
        Cookie coord = coordenacao();

        var lote = mvc.post().uri("/api/v1/coordenacao/certificados/deferimentos").with(csrf()).cookie(coord)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"certificados":[{"id":"%s","versao":%d},{"id":"%s","versao":%d}]}"""
                        .formatted(a.id(), a.versao(), b.id(), b.versao() + 3))
                .exchange();

        assertThat(lote).hasStatusOk();
        assertThat(lote).bodyJson().extractingPath("$[0].deferido").isEqualTo(true);
        assertThat(lote).bodyJson().extractingPath("$[1].deferido").isEqualTo(false);
        assertThat(lote).bodyJson().extractingPath("$[1].erroCodigo").isEqualTo("versao");
        assertThat(mvc.get().uri("/api/v1/alunos/me/saldo").cookie(aluno.sessao()))
                .bodyJson().extractingPath("$.integralizadas").isEqualTo(10.0);
    }

    @Test
    void listaEFichaDoAlunoMostramSaldoECertificados() throws Exception {
        Conta aluno = novoAluno();
        Lancado certificado = lancar(aluno.sessao(), 18, "60");
        Cookie coord = coordenacao();
        decidir(coord, certificado.id(), certificado.versao(), "\"decisao\":\"DEFERIR\"");
        lancar(aluno.sessao(), 21, "4");

        var lista = mvc.get().uri("/api/v1/coordenacao/alunos").cookie(coord).exchange();
        assertThat(lista).hasStatusOk();
        String filtro = "$[?(@.matricula == '" + aluno.matricula() + "')]";
        assertThat(JsonPath.<List<Double>>read(corpo(lista), filtro + ".integralizadas")).containsExactly(50.0);
        assertThat(JsonPath.<List<Integer>>read(corpo(lista), filtro + ".paraAnalisar")).containsExactly(1);
        String alunoId = JsonPath.<List<String>>read(corpo(lista), filtro + ".id").getFirst();

        var ficha = mvc.get().uri("/api/v1/coordenacao/alunos/" + alunoId).cookie(coord).exchange();
        assertThat(ficha).hasStatusOk();
        assertThat(ficha).bodyJson().extractingPath("$.certificados.length()").isEqualTo(2);
        assertThat(ficha).bodyJson().extractingPath("$.saldo.integralizadas").isEqualTo(50.0);
        assertThat(ficha).bodyJson().extractingPath("$.saldo.pendentes").isEqualTo(10.0);
    }

    @Test
    void alunoNaoEntraNaAreaDaCoordenacao() {
        Conta aluno = novoAluno();
        assertThat(mvc.get().uri("/api/v1/coordenacao/fila").cookie(aluno.sessao())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/v1/coordenacao/alunos").cookie(aluno.sessao())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void coordenadorSemVinculoComOCursoNaoVeOsAlunos() throws Exception {
        Conta aluno = novoAluno();
        Lancado certificado = lancar(aluno.sessao(), 21, "4");
        String email = "coord-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.local";
        contas.criarConta(new ContasService.NovaConta(email, "Outra coordenação", SENHA, Papel.COORDENADOR));
        Cookie outro = entrar(email, SENHA);

        assertThat(mvc.get().uri("/api/v1/coordenacao/fila").cookie(outro)).hasStatusOk().bodyJson().isEqualTo("[]");
        assertThat(mvc.get().uri("/api/v1/coordenacao/certificados/" + certificado.id()).cookie(outro)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(decidir(outro, certificado.id(), certificado.versao(), "\"decisao\":\"DEFERIR\"")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/v1/coordenacao/certificados/" + certificado.id() + "/arquivo").cookie(outro))
                .hasStatus(HttpStatus.NOT_FOUND);

        // A coordenação do curso abre o arquivo normalmente.
        assertThat(mvc.get().uri("/api/v1/coordenacao/certificados/" + certificado.id() + "/arquivo").cookie(coordenacao()))
                .hasStatusOk().hasContentType("application/pdf");
    }
}
