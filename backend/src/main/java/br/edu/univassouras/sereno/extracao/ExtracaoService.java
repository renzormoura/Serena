package br.edu.univassouras.sereno.extracao;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * API pública do módulo: lê um arquivo de certificado e sugere os campos. Não conhece o módulo de certificados;
 * recebe uma referência opaca para guardar o registro da leitura.
 */
@Service
public class ExtracaoService {

    private final LeitorDeArquivo leitor;
    private final ExtracaoRepository extracoes;
    private final Clock relogio;

    ExtracaoService(Ocr ocr, ExtracaoRepository extracoes, Optional<Clock> relogio) {
        this.leitor = new LeitorDeArquivo(ocr);
        this.extracoes = extracoes;
        this.relogio = relogio.orElse(Clock.systemDefaultZone());
    }

    /**
     * Lê o arquivo e registra o resultado. Pode levar segundos (OCR): não chamar dentro de uma transação longa.
     *
     * @param referencia   id do certificado, só para guardar o registro
     * @param nomeEsperado nome do aluno, para conferir se o certificado é dele
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResultadoExtracao extrair(UUID referencia, byte[] conteudo, String tipoMime, String nomeEsperado) {
        long inicio = System.nanoTime();
        LeitorDeArquivo.Leitura leitura = leitor.ler(conteudo, tipoMime);
        ResultadoExtracao resultado;
        if (leitura.metodo() == MetodoExtracao.TEXTO || leitura.metodo() == MetodoExtracao.OCR) {
            ExtratorDeCampos.Campos c = new ExtratorDeCampos(LocalDate.now(relogio)).extrair(leitura.texto(), nomeEsperado);
            List<String> avisos = new ArrayList<>();
            leitura.aviso().ifPresent(avisos::add);
            avisos.addAll(c.avisos());
            resultado = new ResultadoExtracao(leitura.metodo(), c.titulo(), c.instituicao(), c.dataInicio(), c.dataFim(),
                    c.horas(), c.nomeConfere(), c.trechos(), avisos, leitura.texto(), leitura.pdf());
        } else {
            ResultadoExtracao falha = ResultadoExtracao.falha(leitura.metodo(), leitura.aviso().orElse("Não foi possível ler o arquivo."));
            resultado = new ResultadoExtracao(falha.metodo(), falha.titulo(), falha.instituicao(), falha.dataInicio(),
                    falha.dataFim(), falha.horas(), falha.nomeConfere(), falha.trechos(), falha.avisos(), falha.texto(), leitura.pdf());
        }
        long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
        extracoes.save(new Extracao(referencia, leitura.texto(), resultado, duracaoMs));
        return resultado;
    }

    @Transactional(readOnly = true)
    public Optional<ResultadoExtracao> ultima(UUID referencia) {
        return extracoes.findFirstByCertificadoIdOrderByCriadoEmDesc(referencia).map(Extracao::resultado);
    }

    @Transactional(readOnly = true)
    public Map<UUID, ResultadoExtracao> ultimas(Collection<UUID> referencias) {
        if (referencias.isEmpty()) {
            return Map.of();
        }
        return extracoes.maisRecentes(referencias).stream()
                .collect(Collectors.toMap(Extracao::getCertificadoId, Extracao::resultado, (a, b) -> a));
    }
}
