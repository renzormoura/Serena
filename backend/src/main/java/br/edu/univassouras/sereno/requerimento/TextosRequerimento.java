package br.edu.univassouras.sereno.requerimento;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Textos fixos do requerimento, iguais aos da aba INTEGRALIZAÇÃO da planilha modelo. */
final class TextosRequerimento {

    static final String UNIVERSIDADE = "UNIVERSIDADE DE VASSOURAS";
    static final String PRO_REITORIA = "PRÓ-REITORIA DE CIÊNCIAS TECNOLÓGICAS E EXATAS";
    static final String HORAS_INTEGRALIZADAS = "Horas integralizadas:";
    static final String DEFERIDO_EM = "Deferido em:";
    static final String COORDENADOR = "Coordenador do Curso";
    static final String NOME = "NOME DO ALUNO(A):";
    static final String MATRICULA = "MATRÍCULA:";
    static final String RG = "RG:";
    static final String CPF = "CPF:";
    static final String PEDIDO = "VEM REQUERER APROVEITAMENTO DAS ATIVIDADES ABAIXO ASSINALADAS E COMPROVADAS COM AS CÓPIAS DOS "
            + "COMPROVANTES EM ANEXO PARA INTEGRALIZAÇÃO DE CARGA HORÁRIA CORRESPONDENTE ÀS ATIVIDADES COMPLEMENTARES OBRIGATÓRIAS.";
    static final String[] CABECALHO = {"ITEM", "MACROATIVIDADES", "DESCRIÇÃO", "HORAS ACC", "QTD", "HORAS INTEGRALIZADAS"};
    static final String TOTAL = "TOTAL DE HORAS INTEGRALIZADAS NOS ITENS ACIMA";

    /** Logo da universidade, no topo do requerimento (como na planilha modelo). */
    static final byte[] LOGO = carregarLogo();

    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TextosRequerimento() {
    }

    static String curso(String curso) {
        return "CURSO DE " + curso.toUpperCase(Locale.ROOT);
    }

    static String destinatario(String curso) {
        return "AO SR. COORDENADOR DO CURSO DE " + curso.toUpperCase(Locale.ROOT) + " DA UNIVERSIDADE DE VASSOURAS";
    }

    private static byte[] carregarLogo() {
        try (java.io.InputStream in = TextosRequerimento.class.getResourceAsStream("/requerimento/logo-univassouras.png")) {
            if (in == null) {
                throw new IllegalStateException("Logo do requerimento não encontrada no classpath");
            }
            return in.readAllBytes();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** 20.00 → "20"; 4.50 → "4,5". */
    static String horas(BigDecimal valor) {
        return new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR"))).format(valor);
    }
}
