package com.example.herokuDemo1.fnol.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * Request DTO for status update operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {

    @NotNull(message = "Status is required")
    private String status;
}
