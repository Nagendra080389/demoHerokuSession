package com.example.herokuDemo1.fnol.dto;

import com.example.herokuDemo1.fnol.model.Claim;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for claim operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {

    private Long id;
    private String claimId;
    private String policyNumber;
    private String claimantName;
    private LocalDate lossDate;
    private String lossType;
    private String description;
    private BigDecimal estimatedAmount;
    private String status;
    private Boolean requiresAdjusterReview;
    private String createdBy;
    private Long createdAt;
    private String updatedBy;
    private Long updatedAt;

    public static ClaimResponse from(Claim claim) {
        if (claim == null) {
            return null;
        }
        return ClaimResponse.builder()
                .id(claim.getId())
                .claimId(claim.getId() != null ? claim.getId().toString() : null)
                .policyNumber(claim.getPolicyNumber())
                .claimantName(claim.getClaimantName())
                .lossDate(claim.getLossDate())
                .lossType(claim.getLossType())
                .description(claim.getDescription())
                .estimatedAmount(claim.getEstimatedAmount())
                .status(claim.getStatus() != null ? claim.getStatus().name() : null)
                .requiresAdjusterReview(claim.getRequiresAdjusterReview())
                .createdBy(claim.getCreatedBy())
                .createdAt(claim.getCreatedAt())
                .updatedBy(claim.getUpdatedBy())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}
