package br.edu.univassouras.sereno.classificacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.univassouras.sereno.calculo.EstrategiaCalculo;
import br.edu.univassouras.sereno.regulamento.Catalogo;
import br.edu.univassouras.sereno.regulamento.OrigemEsperada;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Mecânica da pontuação, com um catálogo mínimo. O acerto com o catálogo real fica em {@code ClassificacaoRealIntegracaoTest}. */
class ClassificadorDeItensTest {

    private final ClassificadorDeItens classificador = new ClassificadorDeItens();

    private static Catalogo.ItemDto item(int numero, OrigemEsperada origem, String... palavras) {
        return new Catalogo.ItemDto(numero, "Item " + numero, null, BigDecimal.TEN, EstrategiaCalculo.FIXO_POR_OCORRENCIA,
                null, null, null, List.of(palavras), origem);
    }

    private static final Catalogo CATALOGO = new Catalogo(UUID.randomUUID(), "Teste", 1, null, List.of(
            new Catalogo.MacroDto("A", 1, "A", null, List.of(
                    item(10, OrigemEsperada.INTERNA, "curso de extensão", "minicurso"),
                    item(18, OrigemEsperada.INTERNA, "monitoria", "monitor"),
                    item(22, OrigemEsperada.INTERNA, "palestra"),
                    item(24, OrigemEsperada.INTERNA, "projeto de extensão", "mostra de extensão"),
                    item(46, OrigemEsperada.EXTERNA, "curso", "formação"),
                    item(48, OrigemEsperada.EXTERNA, "palestra"),
                    item(52, OrigemEsperada.QUALQUER, "projeto rondon")))));

    private SugestaoDeItem sugerir(String texto, String instituicao) {
        return classificador.sugerir(CATALOGO, texto, Optional.empty(), Optional.ofNullable(instituicao));
    }

    @Test
    void origemDesempataItensComAMesmaPalavra() {
        assertThat(sugerir("Universidade de Vassouras. Participou da palestra X.", null).principal())
                .hasValueSatisfying(c -> assertThat(c.numero()).isEqualTo(22));
        assertThat(sugerir("Participou da palestra X promovida pelo CREA.", "CREA").principal())
                .hasValueSatisfying(c -> assertThat(c.numero()).isEqualTo(48));
    }

    @Test
    void semOrigemConhecidaUmaPalavraEmDoisItensFicaComConfiancaBaixa() {
        SugestaoDeItem s = sugerir("Participou da palestra X.", null);

        assertThat(s.confianca()).isEqualTo(Confianca.BAIXA);
        assertThat(s.itemParaPreencher()).isEmpty();
        assertThat(s.candidatos()).extracting(SugestaoDeItem.Candidato::numero).containsExactly(22, 48);
    }

    @Test
    void expressaoEspecificaPesaMaisQueTermoGenerico() {
        SugestaoDeItem s = sugerir("Concluiu o curso de extensão em SQL.", null);

        assertThat(s.principal()).hasValueSatisfying(c -> assertThat(c.numero()).isEqualTo(10));
        assertThat(s.alternativas()).containsExactly(46);
    }

    @Test
    void palavrasDeLigacaoEAcentosNaoImportam() {
        assertThat(sugerir("IV MOSTRA DA EXTENSAO 2023", null).principal())
                .hasValueSatisfying(c -> assertThat(c.numero()).isEqualTo(24));
    }

    @Test
    void palavraDentroDeOutraNaoConta() {
        // "monitoramento" não é "monitor".
        assertThat(sugerir("Curso sobre monitoramento ambiental.", "Alura").principal())
                .hasValueSatisfying(c -> assertThat(c.numero()).isEqualTo(46));
    }

    @Test
    void certificadoQueSeDeclaraExternoContaComoExterno() {
        assertThat(sugerir("Palestra sobre carreira — evento externo.", null).principal())
                .hasValueSatisfying(c -> assertThat(c.numero()).isEqualTo(48));
    }

    @Test
    void tituloReforcaAPalavraChave() {
        SugestaoDeItem s = classificador.sugerir(CATALOGO, "Participou da palestra no projeto rondon.",
                Optional.of("Projeto Rondon"), Optional.empty());

        assertThat(s.principal()).hasValueSatisfying(c -> assertThat(c.numero()).isEqualTo(52));
        assertThat(s.confianca()).isEqualTo(Confianca.ALTA);
        assertThat(s.motivo()).contains("“projeto rondon”");
    }

    @Test
    void textoSemNenhumaPalavraChaveNaoSugereNada() {
        SugestaoDeItem s = sugerir("Declaração de comparecimento.", null);

        assertThat(s.principal()).isEmpty();
        assertThat(s.motivo()).contains("Escolha o item");
    }
}
