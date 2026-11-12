package br.edu.univassouras.sereno.requerimento;

import br.edu.univassouras.sereno.academico.AlunoComConta;
import br.edu.univassouras.sereno.academico.AlunoResumo;
import br.edu.univassouras.sereno.academico.AlunoService;
import br.edu.univassouras.sereno.certificados.IntegralizacaoService;
import br.edu.univassouras.sereno.comum.ErroDeNegocio;
import br.edu.univassouras.sereno.identidade.Papel;
import br.edu.univassouras.sereno.regulamento.Catalogo;
import br.edu.univassouras.sereno.regulamento.CatalogoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RequerimentoService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    /** Quem gera: o nome vai impresso no requerimento. */
    record Coordenador(UUID id, String nome, Papel papel) {
    }

    record ArquivoGerado(byte[] conteudo, String nome) {
    }

    private final RequerimentoRepository requerimentos;
    private final AlunoService alunos;
    private final CatalogoService catalogo;
    private final IntegralizacaoService integralizacao;
    private final Clock relogio;

    RequerimentoService(RequerimentoRepository requerimentos, AlunoService alunos, CatalogoService catalogo,
            IntegralizacaoService integralizacao, Optional<Clock> relogio) {
        this.requerimentos = requerimentos;
        this.alunos = alunos;
        this.catalogo = catalogo;
        this.integralizacao = integralizacao;
        this.relogio = relogio.orElse(Clock.systemUTC());
    }

    /**
     * Gera o requerimento com o que já foi deferido. Certificados ainda em análise ficam de fora (o frontend avisa
     * antes). "Deferido em" é a data real da geração, não a data do dia em que o arquivo for aberto.
     */
    @Transactional
    RequerimentoDto gerar(Coordenador quem, UUID alunoId) {
        AlunoComConta aluno = alunos.visivel(quem.id(), quem.papel(), alunoId);
        Instant agora = relogio.instant();
        return RequerimentoDto.de(requerimentos.save(montar(aluno.aluno(), aluno.nome(), Optional.of(quem), agora)));
    }

    /**
     * O aluno gera o próprio requerimento, com as horas já validadas. "Deferido em" e a assinatura ficam em branco:
     * quem defere é a coordenação.
     */
    @Transactional
    RequerimentoDto gerarPeloAluno(UUID usuarioId, String nomeAluno) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        return RequerimentoDto.de(requerimentos.save(montar(aluno, nomeAluno, Optional.empty(), relogio.instant())));
    }

    @Transactional(readOnly = true)
    List<RequerimentoDto> listarDoAluno(UUID usuarioId) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        return requerimentos.findByAlunoIdOrderByNumeroDesc(aluno.id()).stream().map(RequerimentoDto::de).toList();
    }

    @Transactional(readOnly = true)
    ArquivoGerado arquivoDoAluno(UUID usuarioId, UUID id, boolean pdf) {
        AlunoResumo aluno = alunos.exigirAluno(usuarioId);
        Requerimento r = requerimentos.findById(id)
                .filter(req -> req.getAlunoId().equals(aluno.id()))
                .orElseThrow(() -> ErroDeNegocio.naoEncontrado("Requerimento não encontrado."));
        return pdf ? new ArquivoGerado(GeradorPdf.gerar(r), nomeArquivo(r, "pdf"))
                : new ArquivoGerado(GeradorXlsx.gerar(r), nomeArquivo(r, "xlsx"));
    }

    private Requerimento montar(AlunoResumo aluno, String nomeAluno, Optional<Coordenador> coordenador, Instant agora) {
        Catalogo regulamento = catalogo.catalogo(aluno.regulamentoId())
                .orElseThrow(() -> new IllegalStateException("Regulamento do aluno não existe"));
        IntegralizacaoService.Integralizacao integralizado = integralizacao.doAluno(aluno);
        if (integralizado.porItem().isEmpty()) {
            throw ErroDeNegocio.invalido("sem-horas", coordenador.isPresent()
                    ? "O aluno ainda não tem certificados validados."
                    : "Você ainda não tem certificados validados pela coordenação.");
        }
        List<Requerimento.Linha> linhas = regulamento.macroatividades().stream()
                .sorted(Comparator.comparingInt(Catalogo.MacroDto::ordem))
                .flatMap(m -> m.itens().stream()
                        .sorted(Comparator.comparingInt(Catalogo.ItemDto::numero))
                        .map(i -> {
                            IntegralizacaoService.PorItem feito = integralizado.porItem()
                                    .getOrDefault(i.numero(), new IntegralizacaoService.PorItem(0, BigDecimal.ZERO));
                            return new Requerimento.Linha(i.numero(), m.nome(), i.nome(), i.descricao(), i.horasPorOcorrencia(),
                                    feito.quantidade(), feito.horas());
                        }))
                .toList();
        return new Requerimento(aluno.id(), requerimentos.ultimoNumero(aluno.id()) + 1, nomeAluno, aluno.matricula(),
                aluno.curso(), coordenador.map(Coordenador::id).orElse(null), coordenador.map(Coordenador::nome).orElse(null),
                agora, coordenador.isPresent() ? LocalDate.ofInstant(agora, FUSO) : null, linhas, coordenador.isEmpty());
    }

    @Transactional(readOnly = true)
    List<RequerimentoDto> listar(Coordenador quem, UUID alunoId) {
        alunos.visivel(quem.id(), quem.papel(), alunoId);
        return requerimentos.findByAlunoIdOrderByNumeroDesc(alunoId).stream().map(RequerimentoDto::de).toList();
    }

    @Transactional(readOnly = true)
    ArquivoGerado pdf(Coordenador quem, UUID id) {
        Requerimento r = visivel(quem, id);
        return new ArquivoGerado(GeradorPdf.gerar(r), nomeArquivo(r, "pdf"));
    }

    @Transactional(readOnly = true)
    ArquivoGerado xlsx(Coordenador quem, UUID id) {
        Requerimento r = visivel(quem, id);
        return new ArquivoGerado(GeradorXlsx.gerar(r), nomeArquivo(r, "xlsx"));
    }

    private Requerimento visivel(Coordenador quem, UUID id) {
        Requerimento r = requerimentos.findById(id)
                .orElseThrow(() -> ErroDeNegocio.naoEncontrado("Requerimento não encontrado."));
        alunos.visivel(quem.id(), quem.papel(), r.getAlunoId());
        return r;
    }

    private static String nomeArquivo(Requerimento r, String extensao) {
        return "requerimento-" + r.getMatricula().replaceAll("[^A-Za-z0-9-]", "") + "-" + r.getNumero() + "." + extensao;
    }
}
