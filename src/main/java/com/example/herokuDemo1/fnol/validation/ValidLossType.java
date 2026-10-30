package com.example.herokuDemo1.fnol.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Constraint annotation for validating loss type values.
 */
@Documented
@Constraint(validatedBy = LossTypeValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLossType {

    String message() default "Invalid loss type. Must be AUTO, HOME, or LIABILITY";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
