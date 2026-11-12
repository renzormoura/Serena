package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.academico.AlunoResumo;
import br.edu.univassouras.sereno.calculo.SaldoHoras;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API pública: o que já está integralizado, item a item. Base do requerimento oficial. */
@Service
public class IntegralizacaoService {

    /**
     * @param quantidade certificados que contam neste item (coluna QTD do requerimento)
     * @param horas      horas integralizadas no item, já com limites aplicados
     */
    public record PorItem(int quantidade, BigDecimal horas) {
    }

    /**
     * @param naoAnalisados certificados conferidos pelo aluno que a coordenação ainda não decidiu (ficam de fora)
     */
    public record Integralizacao(BigDecimal total, Map<Integer, PorItem> porItem, long naoAnalisados) {
    }

    private final CertificadoRepository certificados;
    private final SaldoService saldo;

    IntegralizacaoService(CertificadoRepository certificados, SaldoService saldo) {
        this.certificados = certificados;
        this.saldo = saldo;
    }

    @Transactional(readOnly = true)
    public Integralizacao doAluno(AlunoResumo aluno) {
        List<Certificado> ativos = certificados.ativosDoAluno(aluno.id());
        SaldoHoras s = saldo.calcular(aluno.regulamentoId(), ativos);
        Map<Integer, Long> quantidades = ativos.stream()
                .filter(c -> saldo.contaComoIntegralizado(c.getStatus()))
                .collect(Collectors.groupingBy(c -> c.getItemNumero().orElseThrow(), Collectors.counting()));
        Map<Integer, PorItem> porItem = quantidades.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> new PorItem(e.getValue().intValue(), s.porItem().getOrDefault(e.getKey(), BigDecimal.ZERO))));
        long naoAnalisados = ativos.stream()
                .filter(c -> c.getStatus() == StatusCertificado.CONFIRMADO || c.getStatus() == StatusCertificado.AJUSTE_SOLICITADO)
                .filter(c -> !saldo.contaComoIntegralizado(c.getStatus()))
                .count();
        return new Integralizacao(s.integralizadas(), porItem, naoAnalisados);
    }
}
