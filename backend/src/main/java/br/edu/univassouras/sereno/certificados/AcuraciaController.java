package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.comum.Anulavel;
import br.edu.univassouras.sereno.regulamento.Catalogo;
import br.edu.univassouras.sereno.regulamento.CatalogoService;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quanto as regras de sugestão acertam: compara o item sugerido com o que o aluno confirmou. Orienta o ajuste
 * das palavras-chave (PLANO §10.2).
 */
@RestController
@PreAuthorize("hasAnyRole('COORDENADOR', 'ADMIN')")
class AcuraciaController {

    private static final int TROCAS_MOSTRADAS = 10;

    private final CertificadoRepository certificados;
    private final CatalogoService catalogo;

    AcuraciaController(CertificadoRepository certificados, CatalogoService catalogo) {
        this.certificados = certificados;
        this.catalogo = catalogo;
    }

    record ItemAcuracia(int numero, String nome, long sugeridos, long acertos, int percentual) {
    }

    record Troca(int sugerido, String nomeSugerido, int escolhido, String nomeEscolhido, long vezes) {
    }

    /** @param percentual nulo enquanto nenhum certificado com sugestão foi conferido */
    record AcuraciaDto(long avaliados, long acertos, @Anulavel Integer percentual, List<ItemAcuracia> porItem,
            List<Troca> trocasMaisComuns) {
    }

    @GetMapping("/api/v1/admin/classificacao/acuracia")
    @Transactional(readOnly = true)
    AcuraciaDto acuracia() {
        Catalogo vigente = catalogo.vigente().orElseThrow();
        List<CertificadoRepository.SugeridoConfirmado> pares = certificados.sugeridoContraConfirmado(
                EnumSet.of(StatusCertificado.CONFIRMADO, StatusCertificado.AJUSTE_SOLICITADO,
                        StatusCertificado.DEFERIDO, StatusCertificado.INDEFERIDO));

        long avaliados = pares.stream().mapToLong(CertificadoRepository.SugeridoConfirmado::total).sum();
        long acertos = pares.stream().filter(p -> p.sugerido() == p.confirmado())
                .mapToLong(CertificadoRepository.SugeridoConfirmado::total).sum();

        Map<Integer, long[]> porItem = new TreeMap<>();
        for (var p : pares) {
            long[] contagem = porItem.computeIfAbsent(p.sugerido(), n -> new long[2]);
            contagem[0] += p.total();
            if (p.sugerido() == p.confirmado()) {
                contagem[1] += p.total();
            }
        }
        List<ItemAcuracia> itens = porItem.entrySet().stream()
                .map(e -> new ItemAcuracia(e.getKey(), nome(vigente, e.getKey()), e.getValue()[0], e.getValue()[1],
                        percentual(e.getValue()[1], e.getValue()[0])))
                .sorted(Comparator.comparingInt(ItemAcuracia::percentual).thenComparing(ItemAcuracia::sugeridos, Comparator.reverseOrder()))
                .toList();
        List<Troca> trocas = pares.stream()
                .filter(p -> p.sugerido() != p.confirmado())
                .sorted(Comparator.comparingLong(CertificadoRepository.SugeridoConfirmado::total).reversed())
                .limit(TROCAS_MOSTRADAS)
                .map(p -> new Troca(p.sugerido(), nome(vigente, p.sugerido()), p.confirmado(), nome(vigente, p.confirmado()), p.total()))
                .toList();
        return new AcuraciaDto(avaliados, acertos, avaliados == 0 ? null : percentual(acertos, avaliados), itens, trocas);
    }

    private static int percentual(long parte, long total) {
        return total == 0 ? 0 : (int) (parte * 100 / total);
    }

    private static String nome(Catalogo catalogo, int numero) {
        return catalogo.item(numero).map(i -> i.item().nome()).orElse("Item " + numero);
    }
}
