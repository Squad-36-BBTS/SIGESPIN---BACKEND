package br.com.bbts.sigespin.dto.projeto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = OrigemDemandaValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OrigemDemandaValida {
    String message() default "Demandas induzidas exigem ação de mobilização; demandas espontâneas não podem ter ação associada.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
