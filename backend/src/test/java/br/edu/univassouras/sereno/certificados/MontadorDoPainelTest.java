package br.edu.univassouras.sereno.certificados;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.univassouras.sereno.academico.Periodos;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Indicadores do painel com números pequenos, conferíveis à mão. */
class MontadorDoPainelTest {

    static final PainelDto.FiltrosDisponiveis SEM_FILTROS = new PainelDto.FiltrosDisponiveis(List.of(), List.of());
    static final MontadorDoPainel.Limiares LIMIARES = new MontadorDoPainel.Limiares(8, 50);
    static final Instant T0 = Instant.parse("2026-10-01T12:00:00Z");

    static MontadorDoPainel.AlunoParaPainel aluno(String nome, int ingresso, int periodo, int horas, Integer exigidas) {
        return new MontadorDoPainel.AlunoParaPainel(UUID.randomUUID(), nome, "M-" + nome, "Engenharia", Optional.of(ingresso),
                Optional.of(periodo),
                BigDecimal.valueOf(horas), Optional.ofNullable(exigidas).map(BigDecimal::valueOf));
    }

    static MontadorDoPainel.CertificadoParaPainel certificado(Integer item, StatusCertificado status, Integer sugerido,
            Duration analise) {
        return new MontadorDoPainel.CertificadoParaPainel(Optional.ofNullable(item), status, Optional.ofNullable(sugerido),
                Optional.of(T0), Optional.ofNullable(analise).map(T0::plus));
    }

    static PainelDto montar(List<MontadorDoPainel.AlunoParaPainel> alunos, List<MontadorDoPainel.CertificadoParaPainel> certificados) {
        return MontadorDoPainel.montar(SEM_FILTROS, alunos, certificados, n -> "Item " + n, LIMIARES);
    }

    @Test
    void comTotalExigidoCalculaPercentuaisFaixasEAlunosEmAtencao() {
        // Total exigido de 200 h.
        var alunos = List.of(
                aluno("Ana", 2020, 10, 220, 200),   // 100% (passou do total)
                aluno("Bruno", 2020, 10, 60, 200),  // 30%: em atenção (10º período, < 50%)
                aluno("Carla", 2021, 8, 20, 200),   // 10%: em atenção, a mais crítica
                aluno("Davi", 2024, 3, 0, 200));    // 0%, mas no 3º período: não está em atenção

        PainelDto p = montar(alunos, List.of());

        assertThat(p.alunos()).isEqualTo(4);
        assertThat(p.exigidas()).isEqualByComparingTo("200");
        assertThat(p.concluiram()).isEqualTo(1);
        assertThat(p.percentualConcluiram()).isEqualTo(25);
        assertThat(p.modoFaixas()).isEqualTo("PERCENTUAL");
        assertThat(p.faixas()).extracting(PainelDto.Faixa::alunos).containsExactly(2L, 1L, 0L, 0L, 1L);
        assertThat(p.atencaoDisponivel()).isTrue();
        assertThat(p.emAtencao()).extracting(PainelDto.AlunoEmAtencao::nome).containsExactly("Carla", "Bruno");
        assertThat(p.emAtencao()).extracting(PainelDto.AlunoEmAtencao::percentual).containsExactly(10, 30);
        assertThat(p.porIngresso()).extracting(PainelDto.PorIngresso::anoIngresso).containsExactly(2020, 2021, 2024);
        assertThat(p.porIngresso().getFirst().mediaIntegralizadas()).isEqualByComparingTo("140.0");
    }

    @Test
    void semTotalExigidoUsaFaixasDeHorasESemListaDeAtencao() {
        var alunos = List.of(aluno("Ana", 2020, 10, 0, null), aluno("Bruno", 2021, 9, 50, null),
                aluno("Carla", 2021, 9, 51, null), aluno("Davi", 2022, 7, 430, null));

        PainelDto p = montar(alunos, List.of());

        assertThat(p.exigidas()).isNull();
        assertThat(p.percentualConcluiram()).isNull();
        assertThat(p.modoFaixas()).isEqualTo("HORAS");
        assertThat(p.faixas()).extracting(PainelDto.Faixa::alunos).containsExactly(1L, 1L, 1L, 0L, 0L, 1L);
        assertThat(p.atencaoDisponivel()).isFalse();
        assertThat(p.emAtencao()).isEmpty();
    }

    @Test
    void contaFilaTempoDeAnaliseItensDecisoesEAcertoDaSugestao() {
        var certificados = List.of(
                certificado(18, StatusCertificado.CONFIRMADO, 18, null),                           // na fila
                certificado(18, StatusCertificado.DEFERIDO, 18, Duration.ofDays(1)),
                certificado(10, StatusCertificado.DEFERIDO, 10, Duration.ofDays(2)),
                certificado(46, StatusCertificado.DEFERIDO, 10, Duration.ofDays(3)),                // aluno trocou 10 → 46
                certificado(46, StatusCertificado.INDEFERIDO, 10, Duration.ofDays(6)),              // trocou de novo
                certificado(21, StatusCertificado.AJUSTE_SOLICITADO, null, Duration.ofHours(12)),
                certificado(null, StatusCertificado.AGUARDANDO_CONFIRMACAO, 22, null));              // não conferido: fora

        PainelDto p = montar(List.of(aluno("Ana", 2022, 9, 0, null)), certificados);

        assertThat(p.aguardandoAnalise()).isEqualTo(1);
        // (1 + 2 + 3 + 6 + 0,5) / 5 = 2,5 dias.
        assertThat(p.diasMedioAnalise()).isEqualTo(2.5);
        // Indeferidos não contam como uso; 18 e 46 empatam com 2 e o desempate é pelo número.
        assertThat(p.itensMaisUsados()).extracting(PainelDto.ItemUsado::numero).containsExactly(18, 10, 21, 46);
        assertThat(p.itensMaisUsados()).extracting(PainelDto.ItemUsado::certificados).containsExactly(2L, 1L, 1L, 1L);
        assertThat(p.decisoesPorItem()).first().satisfies(d -> {
            assertThat(d.numero()).isEqualTo(46);
            assertThat(d.deferidos()).isEqualTo(1);
            assertThat(d.indeferidos()).isEqualTo(1);
        });
        // Com sugestão e conferidos: 18, 18, 10, 46←10, 46←10 → 3 de 5 aceitas.
        assertThat(p.automacao().conferidos()).isEqualTo(5);
        assertThat(p.automacao().percentualAceitas()).isEqualTo(60);
        assertThat(p.automacao().trocasMaisComuns()).singleElement().satisfies(t -> {
            assertThat(t.sugerido()).isEqualTo(10);
            assertThat(t.escolhido()).isEqualTo(46);
            assertThat(t.vezes()).isEqualTo(2);
        });
    }

    @Test
    void semDecisoesNaoInventaTempoMedio() {
        PainelDto p = montar(List.of(), List.of(certificado(18, StatusCertificado.CONFIRMADO, null, null)));
        assertThat(p.diasMedioAnalise()).isNull();
        assertThat(p.automacao().percentualAceitas()).isNull();
        assertThat(p.modoFaixas()).isEqualTo("HORAS");
    }

    @Test
    void periodoContaSemestresDesdeOIngresso() {
        assertThat(Periodos.atual(2021, 1, LocalDate.of(2026, 11, 11))).isEqualTo(12);
        assertThat(Periodos.atual(2026, 2, LocalDate.of(2026, 11, 11))).isEqualTo(1);
        assertThat(Periodos.atual(2026, 1, LocalDate.of(2026, 3, 1))).isEqualTo(1);
        assertThat(Periodos.atual(2025, 2, LocalDate.of(2026, 3, 1))).isEqualTo(2);
        assertThat(Periodos.atual(2027, 1, LocalDate.of(2026, 3, 1))).isEqualTo(1);
    }
}
