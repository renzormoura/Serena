package br.edu.univassouras.sereno.extracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Textos no formato dos certificados mais comuns (sintéticos: nomes e eventos fictícios). Quando houver
 * certificados reais anonimizados em {@code samples/}, eles entram aqui como mais casos (PLANO §10.2).
 */
class ExtratorDeCamposTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 11);
    private static final String ALUNO = "Lucas Andrade Silva";

    private final ExtratorDeCampos extrator = new ExtratorDeCampos(HOJE);

    record Caso(String nome, String texto, String horas, LocalDate inicio, LocalDate fim, String tituloContem,
            String instituicao) {
        @Override
        public String toString() {
            return nome;
        }
    }

    static Stream<Caso> casos() {
        return Stream.of(
                new Caso("alura", """
                        CERTIFICADO
                        Lucas Andrade Silva
                        concluiu o curso Formação Gerente Ágil com carga horária estimada de 70 horas.
                        Alura — 12/03/2023
                        """, "70", LocalDate.of(2023, 3, 12), null, "Formação Gerente Ágil", "Alura"),
                new Caso("udemy-h-decimal", """
                        Certificate of completion... CERTIFICADO DE CONCLUSÃO
                        Udemy  Lucas Andrade Silva concluiu o curso Docker do Zero, com duração total de 12,5 horas
                        Data: 05/08/2024
                        """, "12.5", LocalDate.of(2024, 8, 5), null, "Docker do Zero", "Udemy"),
                new Caso("extensao-periodo-mesmo-mes", """
                        UNIVERSIDADE DE VASSOURAS
                        Pró-Reitoria de Extensão
                        Certificamos que LUCAS ANDRADE SILVA participou da IV MOSTRA DE EXTENSÃO, realizada nos dias
                        12 a 14 de maio de 2023, com carga horária de 3 (três) horas.
                        Vassouras, 30 de junho de 2023.
                        """, "3", LocalDate.of(2023, 5, 12), LocalDate.of(2023, 5, 14), "Iv mostra de extensão",
                        "Universidade de Vassouras"),
                new Caso("monitoria-periodo-numerico", """
                        Declaramos que Lucas A. Silva atuou como monitor da disciplina Banco de Dados, no período de
                        01/08/2023 a 15/12/2023, perfazendo um total de 50 horas.
                        Univassouras
                        """, "50", LocalDate.of(2023, 8, 1), LocalDate.of(2023, 12, 15), "monitor da disciplina Banco de Dados",
                        "Universidade de Vassouras"),
                new Caso("por-extenso", """
                        Certificamos que Lucas Andrade Silva participou do Trote Solidário 2024, promovido pelo DCE,
                        com carga horária de vinte horas. Vassouras, 20 de fevereiro de 2024.
                        """, "20", LocalDate.of(2024, 2, 20), null, "Trote Solidário 2024", null),
                new Caso("horas-e-minutos", """
                        SEBRAE — Certificado. Lucas Andrade Silva concluiu o curso de Como Abrir uma Empresa.
                        Carga horária: 4h30. Emitido em 10/10/2022.
                        """, "4.5", LocalDate.of(2022, 10, 10), null, "Como Abrir uma Empresa", "Sebrae"),
                new Caso("palestra-sem-gatilho", """
                        Certificamos que Lucas Andrade Silva participou da palestra Inteligência Artificial na Engenharia
                        no dia 18/10/2025 (2h).
                        """, "2", LocalDate.of(2025, 10, 18), null, "palestra Inteligência Artificial na Engenharia", null),
                new Caso("congresso-meses-diferentes", """
                        CONGRESSO NACIONAL DE ENGENHARIA
                        Certificamos que Lucas Andrade Silva participou do Congresso Nacional de Engenharia realizado de
                        30 de maio a 2 de junho de 2024, com carga horária total de 32 horas.
                        """, "32", LocalDate.of(2024, 5, 30), LocalDate.of(2024, 6, 2), "Congresso Nacional de Engenharia", null),
                new Caso("horas-aula", """
                        Fundação Bradesco — Escola Virtual. O aluno Lucas Andrade Silva concluiu o curso Excel Básico
                        com carga horária de 15 horas-aula em 03/02/2021.
                        """, "15", LocalDate.of(2021, 2, 3), null, "Excel Básico", "Fundação Bradesco"),
                new Caso("voluntariado-minutos", """
                        Declaração. Lucas Andrade Silva participou como voluntário da Campanha do Agasalho, com duração de
                        90 minutos, em 15/07/2025.
                        """, "1.5", LocalDate.of(2025, 7, 15), null, "Campanha do Agasalho", null),
                new Caso("ocr-com-ruido", """
                        CERT1FICADO
                        Certificamos que LUCAS ANDRADE SILVA particip0u do Desafio das Engenharias 2022
                        com carga hor ária de 16 h0ras. Carga horária: 16h
                        Vassouras , 10/11/2022
                        """, "16", LocalDate.of(2022, 11, 10), null, "Desafio das Engenharias 2022", null),
                new Caso("enade", """
                        Declaramos que Lucas Andrade Silva realizou a prova do ENADE 2025 em 23/11/2025.
                        """, null, LocalDate.of(2025, 11, 23), null, null, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casos")
    void extraiOsCamposEsperados(Caso caso) {
        ExtratorDeCampos.Campos c = extrator.extrair(caso.texto(), ALUNO);

        if (caso.horas() == null) {
            assertThat(c.horas()).isEmpty();
        } else {
            assertThat(c.horas()).hasValueSatisfying(h -> assertThat(h).isEqualByComparingTo(caso.horas()));
        }
        assertThat(c.dataInicio()).isEqualTo(Optional.ofNullable(caso.inicio()));
        assertThat(c.dataFim()).isEqualTo(Optional.ofNullable(caso.fim()));
        if (caso.tituloContem() != null) {
            assertThat(c.titulo()).hasValueSatisfying(t -> assertThat(t).containsIgnoringCase(caso.tituloContem()));
        }
        assertThat(c.instituicao()).isEqualTo(Optional.ofNullable(caso.instituicao()));
    }

    @Test
    void nomeComNomeDoMeioAbreviadoConfere() {
        assertThat(extrator.extrair("Certificamos que Lucas A. Silva participou do evento XYZ de engenharia.", ALUNO)
                .nomeConfere()).contains(true);
    }

    @Test
    void nomeDeOutraPessoaNaoConfereEAvisa() {
        ExtratorDeCampos.Campos c = extrator.extrair(
                "Certificamos que Maria Souza participou do evento XYZ com carga horária de 4 horas.", ALUNO);

        assertThat(c.nomeConfere()).contains(false);
        assertThat(c.avisos()).anySatisfy(a -> assertThat(a).contains("Seu nome não aparece"));
    }

    @Test
    void acentosEMaiusculasNaoAtrapalhamONome() {
        assertThat(extrator.extrair("Certificamos que JOÃO PEDRO CONCEIÇÃO participou do evento de engenharia.",
                "João Pedro da Conceição").nomeConfere()).contains(true);
    }

    @Test
    void variasCargasDiferentesAvisamOAluno() {
        ExtratorDeCampos.Campos c = extrator.extrair("""
                Certificamos que Lucas Andrade Silva participou da Semana Acadêmica com carga horária de 20 horas,
                sendo 8 horas de minicursos e 12 horas de palestras.
                """, ALUNO);

        assertThat(c.horas()).hasValueSatisfying(h -> assertThat(h).isEqualByComparingTo("20"));
        assertThat(c.avisos()).isEmpty();
    }

    @Test
    void valoresDiferentesComPalavraChaveGeramAviso() {
        ExtratorDeCampos.Campos c = extrator.extrair(
                "Lucas Andrade Silva. Carga horária: 40 horas. Duração: 30 horas.", ALUNO);

        assertThat(c.horas()).hasValueSatisfying(h -> assertThat(h).isEqualByComparingTo("40"));
        assertThat(c.avisos()).anySatisfy(a -> assertThat(a).contains("mais de uma carga horária"));
    }

    @Test
    void datasNoFuturoOuImpossiveisSaoIgnoradas() {
        ExtratorDeCampos.Campos c = extrator.extrair(
                "Lucas Andrade Silva participou do evento X em 31/02/2024 e 10/01/2030, carga horária 4 horas.", ALUNO);

        assertThat(c.dataInicio()).isEmpty();
    }

    @Test
    void semCargaHorariaAvisaParaPreencher() {
        assertThat(extrator.extrair("Certificamos que Lucas Andrade Silva participou do evento X.", ALUNO).avisos())
                .anySatisfy(a -> assertThat(a).contains("Não encontramos a carga horária"));
    }

    @Test
    void trechosVemComoEstaoNoCertificado() {
        ExtratorDeCampos.Campos c = extrator.extrair(
                "Lucas Andrade Silva participou da Oficina de Robótica, com carga horária de 8 horas.", ALUNO);

        assertThat(c.trechos()).contains("carga horária de 8 horas");
    }

    @Test
    void numeroPorExtenso() {
        assertThat(NumeroPorExtenso.valor("cento e vinte")).hasValue(120);
        assertThat(NumeroPorExtenso.valor("quarenta e cinco")).hasValue(45);
        assertThat(NumeroPorExtenso.valor("duas")).hasValue(2);
        assertThat(NumeroPorExtenso.valor("muitas")).isEmpty();
    }

    @Test
    void acuraciaDeHorasNoConjuntoDeExemplos() {
        List<Caso> todos = casos().toList();
        long certos = todos.stream().filter(caso -> {
            Optional<BigDecimal> h = extrator.extrair(caso.texto(), ALUNO).horas();
            return caso.horas() == null ? h.isEmpty() : h.map(v -> v.compareTo(new BigDecimal(caso.horas())) == 0).orElse(false);
        }).count();
        // Meta do plano (F2): ≥ 90% de acerto de horas.
        assertThat((double) certos / todos.size()).isGreaterThanOrEqualTo(0.9);
    }
}
