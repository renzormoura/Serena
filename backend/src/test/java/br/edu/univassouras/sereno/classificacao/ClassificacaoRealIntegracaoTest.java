package br.edu.univassouras.sereno.classificacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.univassouras.sereno.IntegracaoBase;
import br.edu.univassouras.sereno.regulamento.Catalogo;
import br.edu.univassouras.sereno.regulamento.CatalogoService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Acerto das regras com as palavras-chave reais do banco (migração V8).
 *
 * <p>Dois conjuntos: as 13 descrições da aba DESCRITIVO da planilha modelo (escritas por um aluno de verdade) e
 * trechos no formato dos certificados mais comuns. O item esperado segue a descrição do regulamento, não
 * necessariamente o que o aluno marcou na planilha (ver comentários). Quando houver certificados reais
 * anonimizados, eles entram aqui.
 */
class ClassificacaoRealIntegracaoTest extends IntegracaoBase {

    @Autowired
    ClassificadorDeItens classificador;

    @Autowired
    CatalogoService catalogo;

    /** @param aceitos itens aceitáveis no top-3 quando o caso é ambíguo pelo próprio regulamento */
    record Caso(String texto, String instituicao, int esperado, Set<Integer> aceitos) {
        Caso(String texto, String instituicao, int esperado) {
            this(texto, instituicao, esperado, Set.of(esperado));
        }
    }

    static final List<Caso> PLANILHA = List.of(
            // A planilha marcou 10 (Cursos Livres), mas o item 10 é para cursos "oferecidos pelo curso"; cursos de
            // plataformas externas são o item 46 (Cursos Online e Presenciais, que vale 30 h em vez de 20 h).
            new Caso("Formacao gerente agil alura", "Alura", 46),
            new Caso("Formacao minha carreira alura", "Alura", 46, Set.of(46, 30)),
            new Caso("Formacao figma alura", "Alura", 46),
            new Caso("BGS - evento cultural externo (2022)", null, 44),
            // A planilha marcou 4 (Evento de Cidadania); pelo regulamento é extensão (24) ou semana acadêmica (25).
            new Caso("curso ministrado na semana das engenharias projeto de extensao ensino nas engenharias", null, 24, Set.of(24, 25)),
            // A planilha marcou 19 (Nivelamentos); curso de extensão é o item 10.
            new Caso("curso extensao sql", null, 10),
            new Caso("IV mostra de extensao 2023", null, 24),
            new Caso("mostra de extensao 2022", null, 24),
            new Caso("oficina front end projeto extensao ensino nas engenharias", null, 24),
            new Caso("apresentacao de projeto semana das engenharias", null, 25, Set.of(25, 24, 43)),
            new Caso("autocom - evento cultural externo (2022)", null, 44),
            new Caso("Monitoria banco de dados 2023.2", null, 18),
            // "Desafio das Engenharias" não tem palavra-chave em nenhum item: fica sem sugestão (caso conhecido).
            new Caso("desafio das engenharias 2022", null, 24));

    static final List<Caso> CERTIFICADOS = List.of(
            new Caso("Certificamos que Ana atuou como monitora da disciplina Cálculo I. Universidade de Vassouras.", null, 18),
            new Caso("Alura. Ana concluiu o curso Formação Gerente Ágil com carga horária de 70 horas.", "Alura", 46),
            new Caso("Udemy. Certificado de conclusão do curso Docker do Zero.", "Udemy", 46),
            new Caso("Universidade de Vassouras. Ana participou da IV Mostra de Extensão.", null, 24),
            new Caso("Universidade de Vassouras. Ana participou da palestra Inteligência Artificial na Engenharia.", null, 22),
            new Caso("CREA-RJ. Ana participou da palestra Segurança do Trabalho em Obras.", "CREA", 48),
            new Caso("Universidade de Vassouras. Visita técnica à Estação de Tratamento de Água.", null, 39),
            new Caso("Universidade de Vassouras. Visita técnica à fábrica em Betim, fora do estado do Rio de Janeiro.", null, 50),
            new Caso("Ministério da Defesa. Ana participou do Projeto Rondon, Operação Serra do Mar.", null, 52),
            new Caso("Justiça Eleitoral. Declaramos que Ana trabalhou como mesária nas eleições de 2024.", null, 47, Set.of(47)),
            new Caso("Ana participou do Congresso Brasileiro de Engenharia Civil, realizado em São Paulo.", null, 40),
            new Caso("Universidade de Vassouras. Ana participou da Semana Acadêmica das Engenharias.", null, 25),
            new Caso("Universidade de Vassouras. Ana participou do Simulado ENADE 2025.", null, 13),
            new Caso("Universidade de Vassouras. Declaramos que Ana realizou a prova do ENADE 2025.", null, 12),
            new Caso("Ana foi bolsista PIBIC de iniciação científica no projeto Concreto Sustentável.", null, 16),
            new Caso("Universidade de Vassouras. Ana concluiu o curso de extensão em SQL.", null, 10),
            new Caso("Sebrae. Ana concluiu o curso online Como Abrir uma Empresa, sobre empreendedorismo.", "Sebrae", 46, Set.of(46, 29)),
            new Caso("Declaramos que Ana realizou estágio não obrigatório na empresa Construtora Alfa.", null, 45),
            new Caso("Ingresso: Bienal do Livro Rio 2025.", null, 44),
            new Caso("Amazon Web Services. AWS Certified Cloud Practitioner — certificação.", "Amazon Web Services", 41),
            new Caso("Universidade de Vassouras. Trote Solidário 2024 — arrecadação de alimentos.", null, 9),
            new Caso("Universidade de Vassouras. Declaramos que Ana é representante de turma do 5º período.", null, 36),
            new Caso("Universidade de Vassouras. Ana participou do grupo de estudos em Estruturas.", null, 15),
            new Caso("Wizard. Ana concluiu o curso de inglês intermediário.", null, 46),
            new Caso("Universidade de Vassouras. Hackathon de inovação em engenharia.", null, 27),
            new Caso("Universidade de Vassouras. Ana participou do Encontro com Egressos da Engenharia.", null, 33),
            new Caso("Universidade de Vassouras. Ana fez parte da comissão organizadora da Semana de Engenharia.", null, 31, Set.of(31, 25)),
            new Caso("Ana participou do webinar Normas de Desempenho, promovido pelo Confea.", "Confea", 48),
            new Caso("Ana participou do evento Consciência Negra: relações étnico-raciais na engenharia.", null, 51),
            new Caso("Equipe Baja Univassouras — participação na competição universitária Baja SAE Brasil.", null, 49));

    record Placar(int total, int top1, int top3) {
    }

    private Placar medir(List<Caso> casos) {
        Catalogo vigente = catalogo.vigente().orElseThrow();
        int top1 = 0, top3 = 0;
        for (Caso caso : casos) {
            SugestaoDeItem s = classificador.sugerir(vigente, caso.texto(), Optional.empty(), Optional.ofNullable(caso.instituicao()));
            if (s.principal().map(c -> c.numero() == caso.esperado()).orElse(false)) {
                top1++;
            }
            if (s.candidatos().stream().anyMatch(c -> caso.aceitos().contains(c.numero()))) {
                top3++;
            }
            if (s.principal().map(c -> c.numero() != caso.esperado()).orElse(true)) {
                System.out.printf("[classificacao] esperado %d, sugerido %s (%s): %s%n", caso.esperado(),
                        s.candidatos().stream().map(SugestaoDeItem.Candidato::numero).toList(), s.confianca(), caso.texto());
            }
        }
        Placar placar = new Placar(casos.size(), top1, top3);
        System.out.println("[classificacao] " + placar);
        return placar;
    }

    @Test
    void certificadosComunsAtingemAMetaDoPlano() {
        Placar p = medir(CERTIFICADOS);

        // Meta do plano para a F3: ≥ 80% de acerto no primeiro item e ≥ 95% entre os três sugeridos.
        assertThat((double) p.top1() / p.total()).as("top-1: %s", p).isGreaterThanOrEqualTo(0.80);
        assertThat((double) p.top3() / p.total()).as("top-3: %s", p).isGreaterThanOrEqualTo(0.95);
    }

    @Test
    void descricoesDaPlanilhaModelo() {
        Placar p = medir(PLANILHA);

        // Textos curtos escritos pelo aluno: meta menor. Um caso (Desafio das Engenharias) não tem como ser reconhecido.
        assertThat((double) p.top1() / p.total()).as("top-1: %s", p).isGreaterThanOrEqualTo(0.70);
        assertThat((double) p.top3() / p.total()).as("top-3: %s", p).isGreaterThanOrEqualTo(0.90);
    }
}
