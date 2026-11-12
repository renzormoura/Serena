package br.edu.univassouras.sereno.certificados;

import br.edu.univassouras.sereno.certificados.armazenamento.ArmazenamentoArquivos;
import br.edu.univassouras.sereno.classificacao.ClassificadorDeItens;
import br.edu.univassouras.sereno.classificacao.SugestaoDeItem;
import br.edu.univassouras.sereno.regulamento.CatalogoService;
import br.edu.univassouras.sereno.extracao.ExtracaoService;
import br.edu.univassouras.sereno.extracao.ResultadoExtracao;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lê os certificados enviados, em segundo plano. Sem transação longa: carrega o arquivo numa transação curta,
 * lê fora de transação (pode levar segundos com OCR) e grava a sugestão em outra transação curta.
 *
 * <p>Qualquer falha na leitura vira "preencha manualmente": o certificado nunca fica preso em PROCESSANDO.
 */
@Component
class LeituraDeCertificados {

    private static final Logger log = LoggerFactory.getLogger(LeituraDeCertificados.class);

    private final CertificadoRepository certificados;
    private final ArmazenamentoArquivos armazenamento;
    private final ExtracaoService extracao;
    private final ClassificadorDeItens classificador;
    private final CatalogoService catalogo;
    private final TransactionTemplate transacao;

    LeituraDeCertificados(CertificadoRepository certificados, ArmazenamentoArquivos armazenamento,
            ExtracaoService extracao, ClassificadorDeItens classificador, CatalogoService catalogo,
            TransactionTemplate transacao) {
        this.certificados = certificados;
        this.armazenamento = armazenamento;
        this.extracao = extracao;
        this.classificador = classificador;
        this.catalogo = catalogo;
        this.transacao = transacao;
    }

    private record ArquivoParaLer(String chave, String tipoMime, java.util.UUID regulamentoId) {
    }

    @Async
    @TransactionalEventListener
    void aoEnviar(CertificadoEnviado evento) {
        Optional<ArquivoParaLer> arquivo = transacao.execute(status -> certificados.findById(evento.certificadoId())
                .filter(c -> c.getStatus() == StatusCertificado.PROCESSANDO)
                .map(c -> new ArquivoParaLer(c.getArquivo().getChave(), c.getArquivo().getTipoMime(), c.getRegulamentoId())));
        if (arquivo == null || arquivo.isEmpty()) {
            return;   // excluído ou já lido (evento reenviado após reinício)
        }

        Certificado.Sugestao sugestao;
        try (InputStream entrada = armazenamento.abrir(arquivo.get().chave()).getInputStream()) {
            ResultadoExtracao r = extracao.extrair(evento.certificadoId(), entrada.readAllBytes(), arquivo.get().tipoMime(),
                    evento.nomeAluno());
            SugestaoDeItem item = catalogo.catalogo(arquivo.get().regulamentoId())
                    .map(cat -> classificador.sugerir(cat, r.texto(), r.titulo(), r.instituicao()))
                    .orElse(SugestaoDeItem.NENHUMA);
            sugestao = new Certificado.Sugestao(r.titulo(), r.instituicao(), r.dataInicio(), r.dataFim(), r.horas(), item);
        } catch (IOException | RuntimeException e) {
            log.warn("Falha ao ler o certificado {}: {}", evento.certificadoId(), e.toString());
            sugestao = Certificado.Sugestao.VAZIA;
        }

        Certificado.Sugestao finalSugestao = sugestao;
        transacao.executeWithoutResult(status -> certificados.findById(evento.certificadoId()).ifPresent(c -> {
            if (c.receberLeitura(finalSugestao)) {
                certificados.save(c);
            }
        }));
    }
}
