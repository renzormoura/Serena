package br.edu.univassouras.sereno.comum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um campo de resposta da API que pode vir {@code null}. Os demais campos de resposta são sempre
 * preenchidos. Vira {@code tipo | null} nos tipos TypeScript gerados do contrato.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface Anulavel {
}
