package com.example.herokuDemo1.fnol.dto;

import com.example.herokuDemo1.fnol.validation.ValidLossType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Data Transfer Object for claim creation requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimCreateRequest {

    @NotBlank(message = "Policy number is required")
    @Size(min = 8, max = 8, message = "Policy number must be exactly 8 characters (ATL-XXXXXX)")
    @Pattern(regexp = "^ATL-\\d{6}$", message = "Policy number must match pattern ATL-######")
    private String policyNumber;

    @NotBlank(message = "Claimant name is required")
    @Size(max = 100, message = "Claimant name must not exceed 100 characters")
    private String claimantName;

    @NotNull(message = "Loss date is required")
    @PastOrPresent(message = "Loss date cannot be in the future")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Loss date must be in ISO 8601 format (yyyy-MM-dd)")
    private String lossDate;

    @NotBlank(message = "Loss type is required")
    @ValidLossType(message = "Loss type must be AUTO, HOME, or LIABILITY")
    private String lossType;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Estimated amount is required")
    @Min(value = 0, message = "Estimated amount must be >= 0")
    @Digits(integer = 10, fraction = 2, message = "Estimated amount must have at most 2 decimal places")
    private BigDecimal estimatedAmount;
}
