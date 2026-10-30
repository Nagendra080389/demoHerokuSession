package com.example.herokuDemo1.fnol.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a validation error response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {

    private String field;
    private String code;
    private String message;
}
