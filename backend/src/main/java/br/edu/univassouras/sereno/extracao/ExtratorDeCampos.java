package br.edu.univassouras.sereno.extracao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heurísticas que leem os campos de um certificado a partir do texto. Função pura e determinística: a mesma
 * entrada dá sempre a mesma saída, o que permite medir a acurácia com um conjunto fixo de exemplos.
 *
 * <p>Regra geral: na dúvida, não preencher e avisar. Um campo vazio o aluno completa; um campo errado ele pode
 * não perceber.
 */
final class ExtratorDeCampos {

    static final String VERSAO = "1";

    private static final String NUMERO = "(\\d{1,4}(?:[.,]\\d{1,2})?)";
    private static final String UNIDADE = "(?:horas?(?:[\\s-]*aula)?|hrs?|hs|h/a|h)\\b";
    private static final String EXTENSO_ENTRE_PARENTESES = "(?:\\s*\\(\\s*[a-z\\s]+\\s*\\))?";
    private static final String GATILHO =
            "(?:carga\\s+horaria(?:\\s+total)?|duracao(?:\\s+total)?|total\\s+de|perfazendo|totalizando|com\\s+carga)";

    /** "carga horária de 20 horas", "carga horária: 4h30", "duração total de 40h/a". */
    private static final Pattern HORAS_COM_GATILHO = Pattern.compile(
            GATILHO + "[^0-9a-z]{0,6}(?:de\\s+|:\\s*|equivalente\\s+a\\s+)?" + NUMERO + "\\s*" + EXTENSO_ENTRE_PARENTESES
                    // "4h30" (minutos colados ao h) antes da unidade simples, para não ler só "4h".
                    + "\\s*(?:h\\s*(\\d{2})\\s*(?:min(?:utos)?)?\\b|" + UNIDADE + "|(?=\\s|$|[.,;)])(?!\\s*min))");
    /** "carga horária de vinte horas". */
    private static final Pattern HORAS_POR_EXTENSO = Pattern.compile(
            GATILHO + "[^0-9a-z]{0,6}(?:de\\s+|:\\s*)?(" + NumeroPorExtenso.PADRAO + ")\\s+horas?\\b");
    /** "4h30", "04:30 horas". */
    private static final Pattern HORAS_E_MINUTOS = Pattern.compile("\\b(\\d{1,3})\\s*(?:h|:)\\s*(\\d{2})\\s*(?:min|horas?|h)?\\b");
    /** Qualquer "20 horas", "8h" sem palavra-chave antes. */
    private static final Pattern HORAS_SOLTAS = Pattern.compile("(?<![\\d/:,.])" + NUMERO + EXTENSO_ENTRE_PARENTESES + "\\s*" + UNIDADE);
    /** "90 minutos" com palavra-chave. */
    private static final Pattern MINUTOS_COM_GATILHO = Pattern.compile(GATILHO + "[^0-9a-z]{0,6}(?:de\\s+)?(\\d{2,4})\\s*min(?:utos)?\\b");

    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("janeiro", 1), Map.entry("fevereiro", 2), Map.entry("marco", 3), Map.entry("abril", 4),
            Map.entry("maio", 5), Map.entry("junho", 6), Map.entry("julho", 7), Map.entry("agosto", 8),
            Map.entry("setembro", 9), Map.entry("outubro", 10), Map.entry("novembro", 11), Map.entry("dezembro", 12));
    private static final String MES = "(janeiro|fevereiro|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)";
    private static final String DIA = "(\\d{1,2})(?:º|o|°)?";

    private static final Pattern DATA_NUMERICA = Pattern.compile("\\b(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})\\b");
    private static final Pattern DATA_POR_EXTENSO = Pattern.compile("\\b" + DIA + "\\s+de\\s+" + MES + "\\s+de\\s+(\\d{4})\\b");
    /** "12 a 14 de maio de 2023", "12 e 13 de maio de 2023". */
    private static final Pattern PERIODO_MESMO_MES = Pattern.compile(
            "\\b" + DIA + "\\s+(?:a|e|ate)\\s+" + DIA + "\\s+de\\s+" + MES + "\\s+de\\s+(\\d{4})\\b");
    /** "30 de maio a 2 de junho de 2023". */
    private static final Pattern PERIODO_MESES = Pattern.compile(
            "\\b" + DIA + "\\s+de\\s+" + MES + "\\s+(?:a|ate)\\s+" + DIA + "\\s+de\\s+" + MES + "\\s+de\\s+(\\d{4})\\b");
    /** "de 01/02/2023 a 30/06/2023", "no período de 01/02/2023 até 30/06/2023". */
    private static final Pattern PERIODO_NUMERICO = Pattern.compile(
            "\\b(\\d{1,2}/\\d{1,2}/\\d{4})\\s*(?:a|ate|-|–)\\s*(\\d{1,2}/\\d{1,2}/\\d{4})\\b");

    private static final String FIM_DO_TITULO =
            "(?=\\s*[,;.]|\\s+(?:realizad|promovid|ocorrid|oferecid|ministrad|organizad|no dia|nos dias|no periodo|"
                    + "durante|entre os dias|em \\d|com carga|com dura|com \\d|de \\d{1,2}[/ ]|na universidade|pela|pelo|"
                    + "carga horaria|ha \\d)|$)";
    private static final List<Pattern> TITULOS = List.of(
            Pattern.compile("conclui[ud]\\w*\\s+(?:com\\s+(?:exito|aproveitamento)\\s+)?(?:o|a)\\s+(?:curso|formacao|treinamento|trilha|programa)\\s+(?:livre\\s+|online\\s+)?(?:de\\s+|do\\s+|da\\s+|em\\s+)?\"?(.{4,200}?)\"?" + FIM_DO_TITULO),
            Pattern.compile("conclui[ud]\\w*\\s+(?:com\\s+(?:exito|aproveitamento)\\s+)?(?:o|a)\\s+\"?(.{4,200}?)\"?" + FIM_DO_TITULO),
            Pattern.compile("particip(?:ou|acao|ante)\\s+(?:como\\s+\\w+\\s+)?(?:d[oa]s?|n[oa]s?|em)\\s+\"?(.{4,200}?)\"?" + FIM_DO_TITULO),
            Pattern.compile("atuou\\s+como\\s+(.{4,200}?)" + FIM_DO_TITULO),
            Pattern.compile("(?:curso|evento|palestra|oficina|workshop|minicurso|seminario|congresso)\\s*:\\s*\"?(.{4,200}?)\"?" + FIM_DO_TITULO));

    /** Instituições conhecidas: termo procurado (sem acento) → nome como deve aparecer. */
    private static final Map<String, String> INSTITUICOES = new LinkedHashMap<>();

    static {
        INSTITUICOES.put("universidade de vassouras", "Universidade de Vassouras");
        INSTITUICOES.put("univassouras", "Universidade de Vassouras");
        INSTITUICOES.put("universidade severino sombra", "Universidade de Vassouras");
        INSTITUICOES.put("fundacao bradesco", "Fundação Bradesco");
        INSTITUICOES.put("escola virtual", "Escola Virtual de Governo (ENAP)");
        INSTITUICOES.put("enap", "Escola Virtual de Governo (ENAP)");
        INSTITUICOES.put("digital innovation one", "DIO");
        INSTITUICOES.put("cisco networking academy", "Cisco Networking Academy");
        INSTITUICOES.put("amazon web services", "Amazon Web Services");
        INSTITUICOES.put("fundacao getulio vargas", "FGV");
        INSTITUICOES.put("alura", "Alura");
        INSTITUICOES.put("udemy", "Udemy");
        INSTITUICOES.put("coursera", "Coursera");
        INSTITUICOES.put("sebrae", "Sebrae");
        INSTITUICOES.put("senai", "Senai");
        INSTITUICOES.put("senac", "Senac");
        INSTITUICOES.put("rocketseat", "Rocketseat");
        INSTITUICOES.put("microsoft", "Microsoft");
        INSTITUICOES.put("google", "Google");
        INSTITUICOES.put("ibm", "IBM");
        INSTITUICOES.put("oracle", "Oracle");
        INSTITUICOES.put("fgv", "FGV");
        INSTITUICOES.put("crea", "CREA");
        INSTITUICOES.put("confea", "Confea");
    }

    private static final Set<String> PARTICULAS = Set.of("de", "da", "do", "das", "dos", "e");

    private final LocalDate hoje;

    ExtratorDeCampos(LocalDate hoje) {
        this.hoje = hoje;
    }

    record Campos(
            Optional<String> titulo,
            Optional<String> instituicao,
            Optional<LocalDate> dataInicio,
            Optional<LocalDate> dataFim,
            Optional<BigDecimal> horas,
            Optional<Boolean> nomeConfere,
            List<String> trechos,
            List<String> avisos) {
    }

    Campos extrair(String bruto, String nomeEsperado) {
        TextoNormalizado texto = TextoNormalizado.de(bruto);
        List<String> trechos = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        Optional<BigDecimal> horas = horas(texto, trechos, avisos);
        Periodo periodo = periodo(texto, trechos, avisos);
        Optional<String> titulo = titulo(texto, trechos);
        Optional<String> instituicao = instituicao(texto);
        Optional<Boolean> nome = nomeConfere(texto, nomeEsperado);

        if (horas.isEmpty()) {
            avisos.add("Não encontramos a carga horária. Confira no certificado e preencha.");
        }
        if (nome.isPresent() && !nome.get()) {
            avisos.add("Seu nome não aparece no certificado. Confira se o arquivo é seu.");
        }
        return new Campos(titulo, instituicao, periodo.inicio(), periodo.fim(), horas, nome, trechos, avisos);
    }

    // ---------------------------------------------------------------- horas

    private record Achado(BigDecimal horas, int inicio, int fim, boolean comGatilho) {
    }

    private Optional<BigDecimal> horas(TextoNormalizado texto, List<String> trechos, List<String> avisos) {
        String s = texto.busca();
        List<Achado> comGatilho = new ArrayList<>();

        Matcher m = HORAS_COM_GATILHO.matcher(s);
        while (m.find()) {
            BigDecimal h = decimal(m.group(1));
            if (m.group(2) != null) {
                h = h.add(minutosEmHoras(Integer.parseInt(m.group(2))));
            }
            comGatilho.add(new Achado(h, m.start(), m.end(), true));
        }
        m = HORAS_POR_EXTENSO.matcher(s);
        while (m.find()) {
            OptionalInt v = NumeroPorExtenso.valor(m.group(1));
            if (v.isPresent()) {
                comGatilho.add(new Achado(BigDecimal.valueOf(v.getAsInt()), m.start(), m.end(), true));
            }
        }
        m = MINUTOS_COM_GATILHO.matcher(s);
        while (m.find()) {
            comGatilho.add(new Achado(minutosEmHoras(Integer.parseInt(m.group(1))), m.start(), m.end(), true));
        }

        List<Achado> candidatos = comGatilho.stream().filter(a -> plausivel(a.horas())).toList();
        if (candidatos.isEmpty()) {
            List<Achado> soltos = new ArrayList<>();
            m = HORAS_E_MINUTOS.matcher(s);
            while (m.find()) {
                soltos.add(new Achado(new BigDecimal(m.group(1)).add(minutosEmHoras(Integer.parseInt(m.group(2)))), m.start(), m.end(), false));
            }
            m = HORAS_SOLTAS.matcher(s);
            while (m.find()) {
                int inicio = m.start();
                if (soltos.stream().noneMatch(a -> inicio >= a.inicio() && inicio < a.fim())) {
                    soltos.add(new Achado(decimal(m.group(1)), m.start(), m.end(), false));
                }
            }
            candidatos = soltos.stream().filter(a -> plausivel(a.horas())).toList();
        }
        if (candidatos.isEmpty()) {
            return Optional.empty();
        }

        List<BigDecimal> distintos = candidatos.stream().map(Achado::horas).map(BigDecimal::stripTrailingZeros).distinct().toList();
        Achado escolhido = candidatos.getFirst();
        if (distintos.size() > 1) {
            // Mais de um valor: fica o primeiro, que costuma ser a carga total, e o aluno é avisado.
            avisos.add("Encontramos mais de uma carga horária (" + String.join(", ", distintos.stream()
                    .map(BigDecimal::toPlainString).map(v -> v.replace('.', ',') + " h").toList()) + "). Confira.");
        }
        trechos.add(texto.trecho(escolhido.inicio(), escolhido.fim()));
        return Optional.of(escolhido.horas().setScale(2, RoundingMode.HALF_UP).stripTrailingZeros());
    }

    private static boolean plausivel(BigDecimal horas) {
        return horas.signum() > 0 && horas.compareTo(BigDecimal.valueOf(2000)) <= 0;
    }

    private static BigDecimal decimal(String texto) {
        return new BigDecimal(texto.replace(',', '.'));
    }

    private static BigDecimal minutosEmHoras(int minutos) {
        return BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- datas

    private record Periodo(Optional<LocalDate> inicio, Optional<LocalDate> fim) {
        static final Periodo VAZIO = new Periodo(Optional.empty(), Optional.empty());
    }

    private Periodo periodo(TextoNormalizado texto, List<String> trechos, List<String> avisos) {
        String s = texto.busca();

        Matcher m = PERIODO_MESES.matcher(s);
        if (m.find()) {
            Optional<LocalDate> inicio = data(m.group(1), MESES.get(m.group(2)), m.group(5));
            Optional<LocalDate> fim = data(m.group(3), MESES.get(m.group(4)), m.group(5));
            if (inicio.isPresent() && fim.isPresent() && !fim.get().isBefore(inicio.get())) {
                trechos.add(texto.trecho(m.start(), m.end()));
                return new Periodo(inicio, fim);
            }
        }
        m = PERIODO_MESMO_MES.matcher(s);
        if (m.find()) {
            int mes = MESES.get(m.group(3));
            Optional<LocalDate> inicio = data(m.group(1), mes, m.group(4));
            Optional<LocalDate> fim = data(m.group(2), mes, m.group(4));
            if (inicio.isPresent() && fim.isPresent() && !fim.get().isBefore(inicio.get())) {
                trechos.add(texto.trecho(m.start(), m.end()));
                return new Periodo(inicio, fim);
            }
        }
        m = PERIODO_NUMERICO.matcher(s);
        if (m.find()) {
            Optional<LocalDate> inicio = dataNumerica(m.group(1));
            Optional<LocalDate> fim = dataNumerica(m.group(2));
            if (inicio.isPresent() && fim.isPresent() && !fim.get().isBefore(inicio.get())) {
                trechos.add(texto.trecho(m.start(), m.end()));
                return new Periodo(inicio, fim);
            }
        }

        // Sem período explícito: datas soltas. A mais antiga costuma ser a da atividade; as outras, de emissão.
        record DataNoTexto(LocalDate data, int inicio, int fim) {
        }
        List<DataNoTexto> datas = new ArrayList<>();
        m = DATA_NUMERICA.matcher(s);
        while (m.find()) {
            final int inicio = m.start(), fim = m.end();
            data(m.group(1), Integer.parseInt(m.group(2)), m.group(3)).ifPresent(d -> datas.add(new DataNoTexto(d, inicio, fim)));
        }
        m = DATA_POR_EXTENSO.matcher(s);
        while (m.find()) {
            final int inicio = m.start(), fim = m.end();
            data(m.group(1), MESES.get(m.group(2)), m.group(3)).ifPresent(d -> datas.add(new DataNoTexto(d, inicio, fim)));
        }
        if (datas.isEmpty()) {
            return Periodo.VAZIO;
        }
        DataNoTexto primeira = datas.stream().min(Comparator.comparing(DataNoTexto::data)).orElseThrow();
        if (datas.stream().map(DataNoTexto::data).distinct().count() > 1) {
            avisos.add("O certificado tem mais de uma data. Usamos a mais antiga como início; confira.");
        }
        trechos.add(texto.trecho(primeira.inicio(), primeira.fim()));
        return new Periodo(Optional.of(primeira.data()), Optional.empty());
    }

    private Optional<LocalDate> dataNumerica(String ddmmaaaa) {
        String[] p = ddmmaaaa.split("/");
        return data(p[0], Integer.parseInt(p[1]), p[2]);
    }

    /** Só datas válidas, de 2000 até hoje: datas no futuro ou absurdas são erro de leitura. */
    private Optional<LocalDate> data(String dia, int mes, String ano) {
        try {
            LocalDate d = LocalDate.of(Integer.parseInt(ano), mes, Integer.parseInt(dia));
            return d.getYear() >= 2000 && !d.isAfter(hoje) ? Optional.of(d) : Optional.empty();
        } catch (DateTimeException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    // ---------------------------------------------------------------- título, instituição, nome

    private Optional<String> titulo(TextoNormalizado texto, List<String> trechos) {
        for (Pattern padrao : TITULOS) {
            Matcher m = padrao.matcher(texto.busca());
            if (m.find()) {
                String titulo = limparTitulo(texto.trecho(m.start(1), m.end(1)));
                if (titulo.chars().filter(Character::isLetter).count() >= 4) {
                    trechos.add(texto.trecho(m.start(), m.end()));
                    return Optional.of(titulo);
                }
            }
        }
        return Optional.empty();
    }

    private static String limparTitulo(String bruto) {
        String t = bruto.replaceAll("^[\"“”'«»\\s]+|[\"“”'«»\\s]+$", "").strip();
        if (t.length() > 200) {
            t = t.substring(0, 200).strip();
        }
        // Títulos em caixa alta ficam mais legíveis só com a primeira letra maiúscula.
        if (t.equals(t.toUpperCase()) && t.chars().anyMatch(Character::isLetter)) {
            t = t.charAt(0) + t.substring(1).toLowerCase();
        }
        return t;
    }

    private static Optional<String> instituicao(TextoNormalizado texto) {
        String s = " " + texto.busca().replaceAll("[^a-z0-9]+", " ") + " ";
        return INSTITUICOES.entrySet().stream()
                .filter(e -> s.contains(" " + e.getKey() + " "))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * O nome confere quando primeiro e último nomes aparecem e pelo menos 60% das partes do nome também
     * (tolerando nomes do meio abreviados ou omitidos). Vazio quando não há texto para comparar.
     */
    private static Optional<Boolean> nomeConfere(TextoNormalizado texto, String nomeEsperado) {
        if (texto.vazio() || nomeEsperado == null || nomeEsperado.isBlank()) {
            return Optional.empty();
        }
        List<String> partes = Arrays.stream(TextoNormalizado.normalizar(nomeEsperado).split("[^a-z]+"))
                .filter(p -> p.length() > 1 && !PARTICULAS.contains(p))
                .toList();
        if (partes.isEmpty()) {
            return Optional.empty();
        }
        String s = " " + texto.busca().replaceAll("[^a-z]+", " ") + " ";
        long presentes = partes.stream().filter(p -> s.contains(" " + p + " ")).count();
        boolean pontas = s.contains(" " + partes.getFirst() + " ") && s.contains(" " + partes.getLast() + " ");
        return Optional.of(pontas && presentes >= Math.ceil(partes.size() * 0.6));
    }
}
