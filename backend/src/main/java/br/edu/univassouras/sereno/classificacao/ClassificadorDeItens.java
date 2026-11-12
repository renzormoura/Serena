package br.edu.univassouras.sereno.classificacao;

import br.edu.univassouras.sereno.comum.Textos;
import br.edu.univassouras.sereno.regulamento.Catalogo;
import br.edu.univassouras.sereno.regulamento.OrigemEsperada;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Sugere o item do regulamento por regras, sem IA (decisão D7). Determinístico e explicável: cada sugestão diz
 * quais palavras-chave do item apareceram no certificado.
 *
 * <p>Pontuação de um item:
 * <ol>
 *   <li>cada palavra-chave encontrada vale o número de palavras dela ("projeto extensao" vale 2, "curso" vale 1),
 *       então expressões específicas pesam mais que termos genéricos;</li>
 *   <li>palavras-chave "fortes", escritas com {@code !} na frente ({@code !monitor}), valem o dobro: o papel do
 *       aluno (monitor, organizador, presidente) decide mais que o nome do evento onde ele atuou;</li>
 *   <li>a mesma palavra-chave no título da atividade vale metade a mais;</li>
 *   <li>a origem desempata itens parecidos: se o certificado é da Universidade de Vassouras, itens "internos"
 *       ganham +1 e os "externos" caem para 40% (e vice-versa). Itens "qualquer" não mudam.</li>
 * </ol>
 */
@Service
public class ClassificadorDeItens {

    static final double BONUS_ORIGEM = 1.0;
    static final double FATOR_ORIGEM_OPOSTA = 0.4;
    static final double BONUS_TITULO = 0.5;
    static final String MARCA_FORTE = "!";
    private static final int MAXIMO_CANDIDATOS = 3;

    private enum OrigemDoCertificado { INTERNA, EXTERNA, DESCONHECIDA }

    public SugestaoDeItem sugerir(Catalogo catalogo, String texto, Optional<String> titulo, Optional<String> instituicao) {
        String busca = " " + Textos.paraBusca(texto) + " ";
        String buscaTitulo = " " + Textos.paraBusca(titulo.orElse("")) + " ";
        if (busca.isBlank()) {
            return SugestaoDeItem.NENHUMA;
        }
        OrigemDoCertificado origem = origem(busca, instituicao);

        List<SugestaoDeItem.Candidato> candidatos = new ArrayList<>();
        for (Catalogo.MacroDto macro : catalogo.macroatividades()) {
            for (Catalogo.ItemDto item : macro.itens()) {
                List<String> encontradas = new ArrayList<>();
                double pontos = 0;
                for (String bruta : item.palavrasChave()) {
                    boolean forte = bruta.strip().startsWith(MARCA_FORTE);
                    String legivel = forte ? bruta.strip().substring(MARCA_FORTE.length()).strip() : bruta.strip();
                    String chave = Textos.paraBusca(legivel);
                    if (chave.isEmpty() || !busca.contains(" " + chave + " ")) {
                        continue;
                    }
                    int peso = chave.split(" ").length * (forte ? 2 : 1);
                    pontos += peso;
                    if (buscaTitulo.contains(" " + chave + " ")) {
                        pontos += peso * BONUS_TITULO;
                    }
                    encontradas.add(legivel);
                }
                if (pontos == 0) {
                    continue;
                }
                pontos = ajustarPelaOrigem(pontos, item.origemEsperada(), origem);
                candidatos.add(new SugestaoDeItem.Candidato(item.numero(), pontos, encontradas));
            }
        }
        if (candidatos.isEmpty()) {
            return SugestaoDeItem.NENHUMA;
        }
        candidatos.sort(Comparator.comparingDouble(SugestaoDeItem.Candidato::pontuacao).reversed()
                .thenComparingInt(SugestaoDeItem.Candidato::numero));
        List<SugestaoDeItem.Candidato> melhores = candidatos.subList(0, Math.min(MAXIMO_CANDIDATOS, candidatos.size()));

        double primeiro = melhores.getFirst().pontuacao();
        double segundo = melhores.size() > 1 ? melhores.get(1).pontuacao() : 0;
        Confianca confianca = primeiro >= 2 && primeiro >= segundo * 1.6 ? Confianca.ALTA
                : primeiro >= 1 && primeiro > segundo * 1.15 ? Confianca.MEDIA
                : Confianca.BAIXA;
        return new SugestaoDeItem(melhores, confianca, motivo(melhores.getFirst(), origem, confianca));
    }

    private static OrigemDoCertificado origem(String busca, Optional<String> instituicao) {
        if (busca.contains(" vassouras ") || busca.contains(" univassouras ") || busca.contains(" severino sombra ")) {
            return OrigemDoCertificado.INTERNA;
        }
        if (busca.contains(" externo ") || busca.contains(" externa ")) {
            return OrigemDoCertificado.EXTERNA;
        }
        return instituicao.isPresent() ? OrigemDoCertificado.EXTERNA : OrigemDoCertificado.DESCONHECIDA;
    }

    private static double ajustarPelaOrigem(double pontos, OrigemEsperada esperada, OrigemDoCertificado origem) {
        if (origem == OrigemDoCertificado.DESCONHECIDA || esperada == OrigemEsperada.QUALQUER) {
            return pontos;
        }
        boolean mesma = (esperada == OrigemEsperada.INTERNA) == (origem == OrigemDoCertificado.INTERNA);
        return mesma ? pontos + BONUS_ORIGEM : pontos * FATOR_ORIGEM_OPOSTA;
    }

    private static String motivo(SugestaoDeItem.Candidato principal, OrigemDoCertificado origem, Confianca confianca) {
        String palavras = String.join(", ", principal.palavras().stream().map(p -> "“" + p + "”").toList());
        String deOnde = switch (origem) {
            case INTERNA -> " O certificado é da Universidade de Vassouras.";
            case EXTERNA -> " O certificado é de fora da universidade.";
            case DESCONHECIDA -> "";
        };
        String cuidado = confianca == Confianca.BAIXA ? " Mais de um item é possível: confira." : "";
        return "Encontramos " + palavras + " no certificado." + deOnde + cuidado;
    }
}
