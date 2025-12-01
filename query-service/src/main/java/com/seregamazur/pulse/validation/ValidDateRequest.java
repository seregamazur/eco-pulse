package com.seregamazur.pulse.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ChartRequestValidator.class)
public @interface ValidDateRequest {
    String message() default "Invalid query params";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

