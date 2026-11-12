package br.edu.univassouras.sereno.certificados;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * Calcula os indicadores do painel a partir dos alunos (já com saldo calculado) e dos certificados. Função pura:
 * sem banco e sem relógio, para os números poderem ser conferidos à mão nos testes.
 */
final class MontadorDoPainel {

    static final int ITENS_MAIS_USADOS = 5;
    static final int ITENS_COM_DECISOES = 10;
    static final int TROCAS = 3;
    private static final int SEM_INGRESSO = 9999;

    /** Certificados que o aluno já conferiu: contam como "uso" de um item. */
    private static final Set<StatusCertificado> CONFERIDOS = EnumSet.of(StatusCertificado.CONFIRMADO,
            StatusCertificado.AJUSTE_SOLICITADO, StatusCertificado.DEFERIDO, StatusCertificado.INDEFERIDO);
    private static final Set<StatusCertificado> DECIDIDOS = EnumSet.of(StatusCertificado.AJUSTE_SOLICITADO,
            StatusCertificado.DEFERIDO, StatusCertificado.INDEFERIDO);

    record AlunoParaPainel(UUID id, String nome, String matricula, String curso, Optional<Integer> anoIngresso,
            Optional<Integer> periodo,
            BigDecimal integralizadas, Optional<BigDecimal> exigidas) {

        Optional<Integer> percentual() {
            return exigidas.filter(e -> e.signum() > 0)
                    .map(e -> Math.min(100, integralizadas.multiply(BigDecimal.valueOf(100)).divide(e, 0, RoundingMode.DOWN).intValue()));
        }
    }

    record CertificadoParaPainel(Optional<Integer> item, StatusCertificado status, Optional<Integer> itemSugerido,
            Optional<Instant> confirmadoEm, Optional<Instant> avaliadoEm) {
    }

    record Limiares(int periodo, int percentual) {
    }

    private MontadorDoPainel() {
    }

    static PainelDto montar(PainelDto.FiltrosDisponiveis filtros, List<AlunoParaPainel> alunos,
            List<CertificadoParaPainel> certificados, IntFunction<String> nomeDoItem, Limiares limiares) {
        List<AlunoParaPainel> comTotal = alunos.stream().filter(a -> a.percentual().isPresent()).toList();
        boolean temTotal = !alunos.isEmpty() && comTotal.size() == alunos.size();
        long concluiram = comTotal.stream().filter(a -> a.percentual().get() >= 100).count();
        Optional<BigDecimal> exigidas = alunos.stream().map(AlunoParaPainel::exigidas).flatMap(Optional::stream).findFirst();

        return new PainelDto(
                filtros,
                alunos.size(),
                exigidas.orElse(null),
                concluiram,
                comTotal.isEmpty() ? null : (int) (concluiram * 100 / comTotal.size()),
                certificados.stream().filter(c -> c.status() == StatusCertificado.CONFIRMADO).count(),
                diasMedioAnalise(certificados),
                temTotal ? "PERCENTUAL" : "HORAS",
                temTotal ? faixasPorPercentual(alunos) : faixasPorHoras(alunos),
                porIngresso(alunos),
                itensMaisUsados(certificados, nomeDoItem),
                decisoesPorItem(certificados, nomeDoItem),
                !comTotal.isEmpty(),
                limiares.periodo(),
                limiares.percentual(),
                emAtencao(comTotal, limiares),
                automacao(certificados, nomeDoItem));
    }

    /** Média, em dias com uma casa, entre o aluno salvar e a coordenação decidir. */
    static Double diasMedioAnalise(List<CertificadoParaPainel> certificados) {
        List<Duration> tempos = certificados.stream()
                .filter(c -> DECIDIDOS.contains(c.status()) && c.confirmadoEm().isPresent() && c.avaliadoEm().isPresent())
                .map(c -> Duration.between(c.confirmadoEm().get(), c.avaliadoEm().get()))
                .filter(d -> !d.isNegative())
                .toList();
        if (tempos.isEmpty()) {
            return null;
        }
        double mediaSegundos = tempos.stream().mapToLong(Duration::toSeconds).average().orElse(0);
        return Math.round(mediaSegundos / 86_400 * 10) / 10.0;
    }

    static List<PainelDto.Faixa> faixasPorPercentual(List<AlunoParaPainel> alunos) {
        long[] n = new long[5];
        for (AlunoParaPainel a : alunos) {
            int p = a.percentual().orElse(0);
            n[p >= 100 ? 4 : p >= 75 ? 3 : p >= 50 ? 2 : p >= 25 ? 1 : 0]++;
        }
        return List.of(new PainelDto.Faixa("0–24%", n[0]), new PainelDto.Faixa("25–49%", n[1]),
                new PainelDto.Faixa("50–74%", n[2]), new PainelDto.Faixa("75–99%", n[3]), new PainelDto.Faixa("Concluído", n[4]));
    }

    /** Sem total exigido, a distribuição é por horas integralizadas. */
    static List<PainelDto.Faixa> faixasPorHoras(List<AlunoParaPainel> alunos) {
        String[] rotulos = {"0 h", "1–50 h", "51–100 h", "101–150 h", "151–200 h", "Mais de 200 h"};
        long[] n = new long[rotulos.length];
        for (AlunoParaPainel a : alunos) {
            double h = a.integralizadas().doubleValue();
            n[h <= 0 ? 0 : h <= 50 ? 1 : h <= 100 ? 2 : h <= 150 ? 3 : h <= 200 ? 4 : 5]++;
        }
        List<PainelDto.Faixa> faixas = new ArrayList<>();
        for (int i = 0; i < rotulos.length; i++) {
            faixas.add(new PainelDto.Faixa(rotulos[i], n[i]));
        }
        return faixas;
    }

    static List<PainelDto.PorIngresso> porIngresso(List<AlunoParaPainel> alunos) {
        return alunos.stream()
                // Sem ingresso informado vai para o fim (chave 9999, devolvida como nulo).
                .collect(Collectors.groupingBy(a -> a.anoIngresso().orElse(SEM_INGRESSO), TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new PainelDto.PorIngresso(e.getKey() == SEM_INGRESSO ? null : e.getKey(), e.getValue().size(), e.getValue().stream()
                        .map(AlunoParaPainel::integralizadas)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(e.getValue().size()), 1, RoundingMode.HALF_UP)))
                .toList();
    }

    static List<PainelDto.ItemUsado> itensMaisUsados(List<CertificadoParaPainel> certificados, IntFunction<String> nome) {
        return certificados.stream()
                .filter(c -> CONFERIDOS.contains(c.status()) && c.status() != StatusCertificado.INDEFERIDO && c.item().isPresent())
                .collect(Collectors.groupingBy(c -> c.item().get(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(ITENS_MAIS_USADOS)
                .map(e -> new PainelDto.ItemUsado(e.getKey(), nome.apply(e.getKey()), e.getValue()))
                .toList();
    }

    static List<PainelDto.DecisoesPorItem> decisoesPorItem(List<CertificadoParaPainel> certificados, IntFunction<String> nome) {
        Map<Integer, long[]> porItem = new TreeMap<>();
        for (CertificadoParaPainel c : certificados) {
            if (!DECIDIDOS.contains(c.status()) || c.item().isEmpty()) {
                continue;
            }
            long[] n = porItem.computeIfAbsent(c.item().get(), i -> new long[3]);
            n[switch (c.status()) {
                case DEFERIDO -> 0;
                case INDEFERIDO -> 1;
                default -> 2;
            }]++;
        }
        return porItem.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<Integer, long[]> e) -> e.getValue()[0] + e.getValue()[1] + e.getValue()[2])
                        .reversed().thenComparing(Map.Entry::getKey))
                .limit(ITENS_COM_DECISOES)
                .map(e -> new PainelDto.DecisoesPorItem(e.getKey(), nome.apply(e.getKey()), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();
    }

    /** Períodos finais com menos da metade das horas: quem corre risco de não integralizar a tempo. */
    static List<PainelDto.AlunoEmAtencao> emAtencao(List<AlunoParaPainel> comTotal, Limiares limiares) {
        return comTotal.stream()
                .filter(a -> a.periodo().isPresent() && a.periodo().get() >= limiares.periodo()
                        && a.percentual().get() < limiares.percentual())
                .sorted(Comparator.comparingInt((AlunoParaPainel a) -> a.percentual().get())
                        .thenComparing(a -> a.periodo().get(), Comparator.reverseOrder()))
                .map(a -> new PainelDto.AlunoEmAtencao(a.id(), a.nome(), a.matricula(), a.curso(), a.periodo().get(),
                        a.integralizadas(), a.percentual().get()))
                .toList();
    }

    static PainelDto.Automacao automacao(List<CertificadoParaPainel> certificados, IntFunction<String> nome) {
        List<CertificadoParaPainel> comSugestao = certificados.stream()
                .filter(c -> CONFERIDOS.contains(c.status()) && c.itemSugerido().isPresent() && c.item().isPresent())
                .toList();
        long aceitas = comSugestao.stream().filter(c -> Objects.equals(c.itemSugerido().get(), c.item().get())).count();
        List<PainelDto.Troca> trocas = comSugestao.stream()
                .filter(c -> !Objects.equals(c.itemSugerido().get(), c.item().get()))
                .collect(Collectors.groupingBy(c -> List.of(c.itemSugerido().get(), c.item().get()), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<List<Integer>, Long>comparingByValue().reversed()
                        .thenComparing(e -> e.getKey().get(0)).thenComparing(e -> e.getKey().get(1)))
                .limit(TROCAS)
                .map(e -> new PainelDto.Troca(e.getKey().get(0), nome.apply(e.getKey().get(0)), e.getKey().get(1),
                        nome.apply(e.getKey().get(1)), e.getValue()))
                .toList();
        return new PainelDto.Automacao(comSugestao.size(),
                comSugestao.isEmpty() ? null : (int) (aceitas * 100 / comSugestao.size()), trocas);
    }
}
