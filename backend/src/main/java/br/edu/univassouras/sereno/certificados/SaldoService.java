package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.calculo.CertificadoParaCalculo;
import br.edu.univassouras.sereno.calculo.MotorCalculo;
import br.edu.univassouras.sereno.calculo.RegrasCalculo;
import br.edu.univassouras.sereno.calculo.SaldoHoras;
import br.edu.univassouras.sereno.regulamento.CatalogoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Liga os certificados do aluno ao motor de cálculo. */
@Service
class SaldoService {

    private final MotorCalculo motor;
    private final CatalogoService catalogo;
    private final boolean exigeDeferimento;

    SaldoService(MotorCalculo motor, CatalogoService catalogo,
            @Value("${sereno.avaliacao.exige-deferimento:true}") boolean exigeDeferimento) {
        this.motor = motor;
        this.catalogo = catalogo;
        this.exigeDeferimento = exigeDeferimento;
    }

    SaldoHoras calcular(UUID regulamentoId, Collection<Certificado> certificados) {
        return motor.calcular(regras(regulamentoId), paraCalculo(certificados));
    }

    /** Quanto um certificado hipotético contaria, somado aos que o aluno já tem. */
    SaldoHoras.Detalhe simular(UUID regulamentoId, Collection<Certificado> existentes, int item, BigDecimal horas,
            Optional<LocalDate> data) {
        List<CertificadoParaCalculo> lista = new ArrayList<>(paraCalculo(existentes));
        UUID hipotetico = UUID.randomUUID();
        lista.add(new CertificadoParaCalculo(hipotetico, item, horas, data, CertificadoParaCalculo.Situacao.PENDENTE));
        return motor.calcular(regras(regulamentoId), lista).detalhes().stream()
                .filter(d -> d.certificadoId().equals(hipotetico))
                .findFirst()
                .orElseThrow();
    }

    private RegrasCalculo regras(UUID regulamentoId) {
        return catalogo.regras(regulamentoId)
                .orElseThrow(() -> new IllegalStateException("Regulamento " + regulamentoId + " não existe"));
    }

    private List<CertificadoParaCalculo> paraCalculo(Collection<Certificado> certificados) {
        return certificados.stream()
                .flatMap(c -> situacao(c.getStatus())
                        // Só certificados conferidos contam, e esses sempre têm item e horas (restrição no banco).
                        .map(s -> new CertificadoParaCalculo(c.getId(), c.getItemNumero().orElseThrow(),
                                c.getHorasCertificado().orElseThrow(), c.getDataInicio().or(c::getDataFim), s))
                        .stream())
                .toList();
    }

    /** Se o certificado já entra nas horas integralizadas (e no requerimento oficial). */
    boolean contaComoIntegralizado(StatusCertificado status) {
        return situacao(status).filter(s -> s == CertificadoParaCalculo.Situacao.DEFERIDO).isPresent();
    }

    /** Indeferidos e os que ainda esperam conferência do aluno não contam em nada. */
    private Optional<CertificadoParaCalculo.Situacao> situacao(StatusCertificado status) {
        return switch (status) {
            case DEFERIDO -> Optional.of(CertificadoParaCalculo.Situacao.DEFERIDO);
            case CONFIRMADO, AJUSTE_SOLICITADO -> Optional.of(exigeDeferimento
                    ? CertificadoParaCalculo.Situacao.PENDENTE
                    : CertificadoParaCalculo.Situacao.DEFERIDO);
            case PROCESSANDO, AGUARDANDO_CONFIRMACAO, INDEFERIDO -> Optional.empty();
        };
    }
}
