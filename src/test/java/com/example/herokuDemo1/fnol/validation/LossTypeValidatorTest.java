package com.example.herokuDemo1.fnol.validation;

import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LossTypeValidator.
 */
class LossTypeValidatorTest {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Test
    void validLossTypes_shouldPassValidation() {
        // Given valid loss types
        ValidLossTypeAnnotationTester tester = new ValidLossTypeAnnotationTester();
        tester.setLossType("AUTO");

        // When validated
        java.util.Set<javax.validation.ConstraintViolation<ValidLossTypeAnnotationTester>> violations = validator.validate(tester);

        // Then no violations
        assertTrue(violations.isEmpty());
    }

    @Test
    void invalidLossType_shouldFailValidation() {
        // Given invalid loss type
        ValidLossTypeAnnotationTester tester = new ValidLossTypeAnnotationTester();
        tester.setLossType("INVALID");

        // When validated
        java.util.Set<javax.validation.ConstraintViolation<ValidLossTypeAnnotationTester>> violations = validator.validate(tester);

        // Then violation present
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Invalid loss type. Must be AUTO, HOME, or LIABILITY", violations.iterator().next().getMessage());
    }

    @Test
    void nullLossType_shouldFailValidation() {
        // Given null loss type
        ValidLossTypeAnnotationTester tester = new ValidLossTypeAnnotationTester();
        tester.setLossType(null);

        // When validated
        java.util.Set<javax.validation.ConstraintViolation<ValidLossTypeAnnotationTester>> violations = validator.validate(tester);

        // Then violation present
        assertFalse(violations.isEmpty());
    }

    @Test
    void emptyLossType_shouldFailValidation() {
        // Given empty loss type
        ValidLossTypeAnnotationTester tester = new ValidLossTypeAnnotationTester();
        tester.setLossType("");

        // When validated
        java.util.Set<javax.validation.ConstraintViolation<ValidLossTypeAnnotationTester>> violations = validator.validate(tester);

        // Then violation present
        assertFalse(violations.isEmpty());
    }

    @Test
    void lowercaseLossType_shouldPassValidation() {
        // Given lowercase loss type (validator handles case insensitivity)
        ValidLossTypeAnnotationTester tester = new ValidLossTypeAnnotationTester();
        tester.setLossType("auto");

        // When validated
        java.util.Set<javax.validation.ConstraintViolation<ValidLossTypeAnnotationTester>> violations = validator.validate(tester);

        // Then no violations (case normalized)
        assertTrue(violations.isEmpty());
    }

    /**
     * Helper class to test the ValidLossType annotation.
     */
    private static class ValidLossTypeAnnotationTester {
        @ValidLossType
        private String lossType;

        public String getLossType() {
            return lossType;
        }

        public void setLossType(String lossType) {
            this.lossType = lossType;
        }
    }
}
