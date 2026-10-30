package com.example.herokuDemo1.fnol.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

/**
 * Validator implementation for LossType constraint.
 */
public class LossTypeValidator implements ConstraintValidator<ValidLossType, String> {

    private static final List<String> VALID_LOSS_TYPES = Arrays.asList("AUTO", "HOME", "LIABILITY");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return VALID_LOSS_TYPES.contains(value.toUpperCase());
    }
}
