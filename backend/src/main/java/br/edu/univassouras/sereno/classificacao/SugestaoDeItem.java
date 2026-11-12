package br.edu.univassouras.sereno.classificacao;

import java.util.List;
import java.util.Optional;

/**
 * Itens prováveis para um certificado, do mais ao menos provável (no máximo 3).
 *
 * @param motivo explicação curta para o aluno ("Encontramos “monitoria” no certificado.")
 */
public record SugestaoDeItem(List<Candidato> candidatos, Confianca confianca, String motivo) {

    public static final SugestaoDeItem NENHUMA =
            new SugestaoDeItem(List.of(), Confianca.BAIXA, "Não reconhecemos o tipo de atividade. Escolha o item.");

    public SugestaoDeItem {
        candidatos = List.copyOf(candidatos);
    }

    public Optional<Candidato> principal() {
        return candidatos.stream().findFirst();
    }

    public List<Integer> alternativas() {
        return candidatos.stream().skip(1).map(Candidato::numero).toList();
    }

    /** Item que deve vir pré-selecionado: só quando a confiança não é baixa. */
    public Optional<Integer> itemParaPreencher() {
        return confianca == Confianca.BAIXA ? Optional.empty() : principal().map(Candidato::numero);
    }

    /** @param palavras palavras-chave do item encontradas no texto */
    public record Candidato(int numero, double pontuacao, List<String> palavras) {
        public Candidato {
            palavras = List.copyOf(palavras);
        }
    }
}
