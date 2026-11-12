package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.academico.AlunoComConta;
import br.edu.univassouras.sereno.academico.AlunoService;
import br.edu.univassouras.sereno.calculo.SaldoHoras;
import br.edu.univassouras.sereno.regulamento.Catalogo;
import br.edu.univassouras.sereno.regulamento.CatalogoService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Junta alunos, saldos e certificados do escopo do coordenador e entrega ao {@link MontadorDoPainel}. */
@Service
class PainelService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    record Filtro(Optional<UUID> cursoId, Optional<Integer> anoIngresso) {
    }

    /** O que a exportação precisa além do painel: a linha de cada aluno. */
    record Dados(PainelDto painel, List<LinhaAluno> alunos) {
    }

    record LinhaAluno(AlunoComConta aluno, Optional<Integer> periodo, SaldoHoras saldo, long paraAnalisar, long emAjuste) {
    }

    private final CertificadoRepository certificados;
    private final AlunoService alunos;
    private final SaldoService saldo;
    private final CatalogoService catalogo;
    private final Clock relogio;
    private final MontadorDoPainel.Limiares limiares;

    PainelService(CertificadoRepository certificados, AlunoService alunos, SaldoService saldo, CatalogoService catalogo,
            Optional<Clock> relogio,
            @Value("${sereno.painel.periodo-atencao:8}") int periodoAtencao,
            @Value("${sereno.painel.percentual-atencao:50}") int percentualAtencao) {
        this.certificados = certificados;
        this.alunos = alunos;
        this.saldo = saldo;
        this.catalogo = catalogo;
        this.relogio = relogio.orElse(Clock.systemUTC());
        this.limiares = new MontadorDoPainel.Limiares(periodoAtencao, percentualAtencao);
    }

    @Transactional(readOnly = true)
    Dados dados(CoordenacaoService.Avaliador quem, Filtro filtro) {
        List<AlunoComConta> visiveis = alunos.visiveis(quem.id(), quem.papel());
        PainelDto.FiltrosDisponiveis filtros = filtrosDisponiveis(visiveis);
        List<AlunoComConta> selecionados = visiveis.stream()
                .filter(a -> filtro.cursoId().map(c -> c.equals(a.aluno().cursoId())).orElse(true))
                .filter(a -> filtro.anoIngresso().map(ano -> ano.equals(a.aluno().anoIngresso())).orElse(true))
                .toList();

        Map<UUID, List<Certificado>> porAluno = selecionados.isEmpty() ? Map.of()
                : certificados.ativosDosAlunos(selecionados.stream().map(a -> a.aluno().id()).toList()).stream()
                        .collect(Collectors.groupingBy(Certificado::getAlunoId));
        Map<UUID, Catalogo> regulamentos = new HashMap<>();
        LocalDate hoje = LocalDate.now(relogio.withZone(FUSO));

        List<LinhaAluno> linhas = selecionados.stream()
                .map(a -> {
                    List<Certificado> ativos = porAluno.getOrDefault(a.aluno().id(), List.of());
                    return new LinhaAluno(a, a.aluno().periodoEm(hoje),
                            saldo.calcular(a.aluno().regulamentoId(), ativos),
                            ativos.stream().filter(c -> c.getStatus() == StatusCertificado.CONFIRMADO).count(),
                            ativos.stream().filter(c -> c.getStatus() == StatusCertificado.AJUSTE_SOLICITADO).count());
                })
                .sorted(Comparator.comparing(l -> l.aluno().nome(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<MontadorDoPainel.AlunoParaPainel> paraPainel = linhas.stream()
                .map(l -> new MontadorDoPainel.AlunoParaPainel(l.aluno().aluno().id(), l.aluno().nome(),
                        l.aluno().aluno().matricula(), l.aluno().aluno().curso(), Optional.ofNullable(l.aluno().aluno().anoIngresso()), l.periodo(),
                        l.saldo().integralizadas(), l.saldo().exigidas()))
                .toList();
        List<MontadorDoPainel.CertificadoParaPainel> certificadosParaPainel = porAluno.values().stream()
                .flatMap(List::stream)
                .map(c -> new MontadorDoPainel.CertificadoParaPainel(c.getItemNumero(), c.getStatus(), c.getItemSugerido(),
                        c.getConfirmadoEm(), c.getAvaliadoEm()))
                .toList();

        // Nomes dos itens: do regulamento de cada aluno; na prática todos usam o vigente.
        selecionados.forEach(a -> regulamentos.computeIfAbsent(a.aluno().regulamentoId(), id -> catalogo.catalogo(id).orElseThrow()));
        Optional<Catalogo> referencia = regulamentos.values().stream().findFirst().or(catalogo::vigente);
        PainelDto painel = MontadorDoPainel.montar(filtros, paraPainel, certificadosParaPainel,
                n -> referencia.flatMap(c -> c.item(n)).map(i -> i.item().nome()).orElse("Item " + n), limiares);
        return new Dados(painel, linhas);
    }

    @Transactional(readOnly = true)
    PainelDto painel(CoordenacaoService.Avaliador quem, Filtro filtro) {
        return dados(quem, filtro).painel();
    }

    private static PainelDto.FiltrosDisponiveis filtrosDisponiveis(List<AlunoComConta> visiveis) {
        List<PainelDto.CursoOpcao> cursos = visiveis.stream()
                .map(a -> new PainelDto.CursoOpcao(a.aluno().cursoId(), a.aluno().curso()))
                .distinct()
                .sorted(Comparator.comparing(PainelDto.CursoOpcao::nome))
                .toList();
        List<Integer> anos = visiveis.stream().map(a -> a.aluno().anoIngresso()).filter(java.util.Objects::nonNull)
                .distinct().sorted(Comparator.reverseOrder()).toList();
        return new PainelDto.FiltrosDisponiveis(cursos, anos);
    }
}
