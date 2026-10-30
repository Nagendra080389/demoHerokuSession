package com.example.herokuDemo1.fnol.exception;

import com.example.herokuDemo1.fnol.dto.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for FNOL service.
 *
 * Handles all application exceptions and returns structured error responses.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ValidationError>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation failed for request: {} {}", request.getMethod(), request.getRequestURI());

        List<ValidationError> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            ValidationError validationError = ValidationError.builder()
                    .field(error.getField())
                    .code(toErrorCode(error.getField(), error.getCode()))
                    .message(error.getDefaultMessage())
                    .build();
            errors.add(validationError);
        });

        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handles invalid status transition errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<List<ValidationError>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument for request: {} {}", request.getMethod(), request.getRequestURI());

        ValidationError error = ValidationError.builder()
                .field("status")
                .code("INVALID_STATUS_TRANSITION")
                .message(ex.getMessage())
                .build();

        List<ValidationError> errors = new ArrayList<>();
        errors.add(error);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
    }

    /**
     * Handles authentication errors.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied for request: {} {}", request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception for request: {} {}", request.getMethod(), request.getRequestURI(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred: " + ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Converts field error codes to standardized error codes.
     */
    private String toErrorCode(String field, String code) {
        if (code == null) {
            return "VALIDATION_ERROR";
        }

        switch (code) {
            case "NotBlank":
                return field.equalsIgnoreCase("policyNumber") ? "POLICY_NUMBER_REQUIRED" :
                       field.equalsIgnoreCase("claimantName") ? "CLAIMANT_NAME_REQUIRED" :
                       field.equalsIgnoreCase("lossDate") ? "LOSS_DATE_REQUIRED" :
                       field.equalsIgnoreCase("lossType") ? "LOSS_TYPE_REQUIRED" :
                       field.equalsIgnoreCase("description") ? "DESCRIPTION_REQUIRED" :
                       field.equalsIgnoreCase("estimatedAmount") ? "ESTIMATED_AMOUNT_REQUIRED" :
                       field.equalsIgnoreCase("status") ? "STATUS_REQUIRED" : "FIELD_REQUIRED";
            case "Size":
                return field.equalsIgnoreCase("policyNumber") ? "INVALID_POLICY_NUMBER_LENGTH" :
                       field.equalsIgnoreCase("claimantName") ? "CLAIMANT_NAME_TOO_LONG" :
                       field.equalsIgnoreCase("description") ? "DESCRIPTION_TOO_LONG" : "FIELD_LENGTH_INVALID";
            case "Pattern":
                return field.equalsIgnoreCase("policyNumber") ? "INVALID_POLICY_NUMBER" :
                       field.equalsIgnoreCase("lossDate") ? "INVALID_DATE_FORMAT" : "FIELD_PATTERN_MISMATCH";
            case "PastOrPresent":
                return field.equalsIgnoreCase("lossDate") ? "LOSS_DATE_FUTURE" : "FIELD_DATE_INVALID";
            case "Min":
                return field.equalsIgnoreCase("estimatedAmount") ? "ESTIMATED_AMOUNT_NEGATIVE" : "FIELD_MIN_INVALID";
            case "Digits":
                return field.equalsIgnoreCase("estimatedAmount") ? "ESTIMATED_AMOUNT_PRECISION" : "FIELD_DIGITS_INVALID";
            default:
                return code.toUpperCase();
        }
    }
}
