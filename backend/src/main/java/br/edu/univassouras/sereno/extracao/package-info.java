/**
 * Leitura automática de certificados: texto do PDF (PDFBox), OCR para imagens (Tesseract) e heurísticas que
 * sugerem carga horária, datas, título, instituição e se o nome do aluno aparece. Não depende de outros módulos.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Extração")
package br.edu.univassouras.sereno.extracao;
