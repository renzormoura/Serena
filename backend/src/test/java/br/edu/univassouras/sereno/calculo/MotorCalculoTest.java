package br.edu.univassouras.sereno.calculo;

import static br.edu.univassouras.sereno.calculo.CertificadoParaCalculo.Situacao.DEFERIDO;
import static br.edu.univassouras.sereno.calculo.CertificadoParaCalculo.Situacao.PENDENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.univassouras.sereno.calculo.CertificadoParaCalculo.Situacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MotorCalculoTest {

    private static final String CID = "CIDADANIA";
    private static final String CIE = "CIENTIFICO_ACADEMICO";
    private static final String EXT = "EXTERNAS";

    private final MotorCalculo motor = new MotorCalculo();

    /** Os itens usados no exemplo da planilha, com os mesmos valores fixos. */
    private static RegrasCalculo regrasDaPlanilha(Optional<BigDecimal> exigidas) {
        return new RegrasCalculo(exigidas,
                Map.of(4, RegrasCalculo.Item.fixo(4, CID, 20),
                        10, RegrasCalculo.Item.fixo(10, CIE, 20),
                        18, RegrasCalculo.Item.fixo(18, CIE, 50),
                        19, RegrasCalculo.Item.fixo(19, CIE, 10),
                        24, RegrasCalculo.Item.fixo(24, CIE, 50),
                        44, RegrasCalculo.Item.fixo(44, EXT, 20)),
                Map.of(CID, RegrasCalculo.Macro.semLimite(CID),
                        CIE, RegrasCalculo.Macro.semLimite(CIE),
                        EXT, RegrasCalculo.Macro.semLimite(EXT)));
    }

    /** As 13 linhas da aba DESCRITIVO do modelo: {item, horas do certificado}. */
    private static List<CertificadoParaCalculo> certificadosDaPlanilha(Situacao situacao) {
        int[][] linhas = {{10, 70}, {10, 32}, {10, 29}, {44, 8}, {4, 3}, {19, 20}, {24, 16},
                {24, 3}, {24, 3}, {24, 3}, {24, 3}, {44, 8}, {18, 50}};
        List<CertificadoParaCalculo> certificados = new ArrayList<>();
        for (int[] linha : linhas) {
            certificados.add(cert(linha[0], linha[1], situacao, null));
        }
        return certificados;
    }

    private static CertificadoParaCalculo cert(int item, double horas, Situacao situacao, LocalDate data) {
        return new CertificadoParaCalculo(UUID.randomUUID(), item, BigDecimal.valueOf(horas), Optional.ofNullable(data), situacao);
    }

    @Test
    void reproduzOTotalDaPlanilhaModelo() {
        SaldoHoras saldo = motor.calcular(regrasDaPlanilha(Optional.empty()), certificadosDaPlanilha(DEFERIDO));

        assertThat(saldo.integralizadas()).isEqualByComparingTo("430");
        assertThat(saldo.porItem()).hasEntrySatisfying(24, h -> assertThat(h).isEqualByComparingTo("250"));
        assertThat(saldo.porItem()).hasEntrySatisfying(10, h -> assertThat(h).isEqualByComparingTo("60"));
        assertThat(saldo.porMacro()).hasEntrySatisfying(CIE, h -> assertThat(h).isEqualByComparingTo("370"));
        assertThat(saldo.pendentes()).isEqualByComparingTo("0");
        assertThat(saldo.faltantes()).isEmpty();
        assertThat(saldo.percentual()).isEmpty();
    }

    @Test
    void valorFixoIgnoraACargaDoCertificado() {
        SaldoHoras saldo = motor.calcular(regrasDaPlanilha(Optional.empty()), List.of(cert(10, 70, DEFERIDO, null)));

        assertThat(saldo.detalhes()).singleElement().satisfies(d -> {
            assertThat(d.horasBrutas()).isEqualByComparingTo("20");
            assertThat(d.horasContabilizadas()).isEqualByComparingTo("20");
        });
    }

    @Test
    void separaIntegralizadasPendentesEFaltantes() {
        List<CertificadoParaCalculo> certificados = new ArrayList<>(certificadosDaPlanilha(DEFERIDO));
        certificados.add(cert(18, 60, PENDENTE, null));

        SaldoHoras saldo = motor.calcular(regrasDaPlanilha(Optional.of(new BigDecimal("500"))), certificados);

        assertThat(saldo.integralizadas()).isEqualByComparingTo("430");
        assertThat(saldo.pendentes()).isEqualByComparingTo("50");
        assertThat(saldo.projecao()).isEqualByComparingTo("480");
        assertThat(saldo.faltantes()).hasValueSatisfying(h -> assertThat(h).isEqualByComparingTo("70"));
        assertThat(saldo.percentual()).contains(86);
    }

    @Test
    void faltantesNuncaFicaNegativoEPercentualParaEm100() {
        SaldoHoras saldo = motor.calcular(regrasDaPlanilha(Optional.of(new BigDecimal("200"))), certificadosDaPlanilha(DEFERIDO));

        assertThat(saldo.faltantes()).hasValueSatisfying(h -> assertThat(h).isEqualByComparingTo("0"));
        assertThat(saldo.percentual()).contains(100);
    }

    @Test
    void aplicaTetoPorOcorrencia() {
        RegrasCalculo regras = new RegrasCalculo(Optional.empty(),
                Map.of(46, new RegrasCalculo.Item(46, EXT, BigDecimal.ZERO, EstrategiaCalculo.HORAS_CERTIFICADO_COM_TETO,
                        Optional.of(new BigDecimal("30")), Optional.empty(), Optional.empty())),
                Map.of(EXT, RegrasCalculo.Macro.semLimite(EXT)));

        SaldoHoras saldo = motor.calcular(regras, List.of(cert(46, 40, DEFERIDO, null), cert(46, 12.5, DEFERIDO, null)));

        assertThat(saldo.integralizadas()).isEqualByComparingTo("42.5");
    }

    @Test
    void limiteDeOcorrenciasDescartaOsMaisRecentes() {
        LocalDate antigo = LocalDate.of(2023, 3, 1);
        LocalDate recente = LocalDate.of(2024, 3, 1);
        RegrasCalculo regras = new RegrasCalculo(Optional.empty(),
                Map.of(36, new RegrasCalculo.Item(36, CID, new BigDecimal("20"), EstrategiaCalculo.FIXO_POR_OCORRENCIA,
                        Optional.empty(), Optional.of(1), Optional.empty())),
                Map.of(CID, RegrasCalculo.Macro.semLimite(CID)));
        CertificadoParaCalculo segundo = cert(36, 0, DEFERIDO, recente);

        SaldoHoras saldo = motor.calcular(regras, List.of(segundo, cert(36, 0, DEFERIDO, antigo)));

        assertThat(saldo.integralizadas()).isEqualByComparingTo("20");
        assertThat(saldo.detalhes()).filteredOn(d -> d.certificadoId().equals(segundo.id()))
                .singleElement()
                .satisfies(d -> assertThat(d.cortes()).singleElement().asString().contains("Limite de 1 certificado"));
    }

    @Test
    void limiteDaMacroCortaParcialmenteEPreservaOsDeferidos() {
        RegrasCalculo regras = new RegrasCalculo(Optional.empty(),
                Map.of(1, RegrasCalculo.Item.fixo(1, CID, 10), 5, RegrasCalculo.Item.fixo(5, CID, 25)),
                Map.of(CID, new RegrasCalculo.Macro(CID, Optional.of(new BigDecimal("40")))));
        CertificadoParaCalculo pendente = cert(5, 0, PENDENTE, LocalDate.of(2020, 1, 1));

        SaldoHoras saldo = motor.calcular(regras, List.of(
                pendente, cert(5, 0, DEFERIDO, LocalDate.of(2024, 1, 1)), cert(1, 0, DEFERIDO, LocalDate.of(2024, 2, 1))));

        // Deferidos consomem o limite primeiro (25 + 10 = 35); o pendente, apesar de mais antigo, só ganha 5.
        assertThat(saldo.integralizadas()).isEqualByComparingTo("35");
        assertThat(saldo.projecao()).isEqualByComparingTo("40");
        assertThat(saldo.pendentes()).isEqualByComparingTo("5");
        assertThat(saldo.detalhes()).filteredOn(d -> d.certificadoId().equals(pendente.id()))
                .singleElement()
                .satisfies(d -> assertThat(d.horasContabilizadas()).isEqualByComparingTo("5"));
    }

    @Test
    void recusaItemQueNaoExisteNoRegulamento() {
        assertThatThrownBy(() -> motor.calcular(regrasDaPlanilha(Optional.empty()), List.of(cert(99, 1, DEFERIDO, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void recusaTetoSemValor() {
        assertThatThrownBy(() -> new RegrasCalculo.Item(1, CID, BigDecimal.ONE, EstrategiaCalculo.HORAS_CERTIFICADO_COM_TETO,
                Optional.empty(), Optional.empty(), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
