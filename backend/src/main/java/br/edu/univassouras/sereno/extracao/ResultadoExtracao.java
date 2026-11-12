package br.edu.univassouras.sereno.extracao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * O que a leitura automática encontrou num certificado. Tudo é sugestão: o aluno confere antes de salvar.
 *
 * @param trechos frases do certificado usadas para preencher os campos, para mostrar ao aluno
 * @param avisos  pontos que o aluno deve conferir (ex.: nome não encontrado, mais de uma carga horária)
 * @param texto   o texto lido (contém dados pessoais: uso interno, nunca exposto na API)
 * @param pdf     metadados do PDF; vazio para imagens
 */
public record ResultadoExtracao(
        MetodoExtracao metodo,
        Optional<String> titulo,
        Optional<String> instituicao,
        Optional<LocalDate> dataInicio,
        Optional<LocalDate> dataFim,
        Optional<BigDecimal> horas,
        Optional<Boolean> nomeConfere,
        List<String> trechos,
        List<String> avisos,
        String texto,
        MetadadosPdf pdf) {

    public ResultadoExtracao {
        texto = texto == null ? "" : texto;
        pdf = pdf == null ? MetadadosPdf.VAZIO : pdf;
        trechos = List.copyOf(trechos);
        avisos = List.copyOf(avisos);
    }

    static ResultadoExtracao falha(MetodoExtracao metodo, String aviso) {
        return new ResultadoExtracao(metodo, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), List.of(), List.of(aviso), "", MetadadosPdf.VAZIO);
    }
}
