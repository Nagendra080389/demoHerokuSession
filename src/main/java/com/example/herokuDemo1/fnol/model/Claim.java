package com.example.herokuDemo1.fnol.model;

import com.example.herokuDemo1.fnol.validation.ValidLossType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a First Notice of Loss (FNOL) claim.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Policy number is required")
    @Size(min = 8, max = 8, message = "Policy number must be exactly 8 characters (ATL-XXXXXX)")
    @Pattern(regexp = "^ATL-\\d{6}$", message = "Policy number must match pattern ATL-######")
    @Column(nullable = false, unique = false)
    private String policyNumber;

    @NotBlank(message = "Claimant name is required")
    @Size(max = 100, message = "Claimant name must not exceed 100 characters")
    @Column(nullable = false)
    private String claimantName;

    @NotNull(message = "Loss date is required")
    @PastOrPresent(message = "Loss date cannot be in the future")
    @Column(nullable = false)
    private LocalDate lossDate;

    @NotBlank(message = "Loss type is required")
    @ValidLossType(message = "Loss type must be AUTO, HOME, or LIABILITY")
    @Column(nullable = false)
    private String lossType;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Column(length = 2000, nullable = false)
    private String description;

    @NotNull(message = "Estimated amount is required")
    @Min(value = 0, message = "Estimated amount must be >= 0")
    @Digits(integer = 10, fraction = 2, message = "Estimated amount must have at most 2 decimal places")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.SUBMITTED;

    @Column(nullable = false)
    private Boolean requiresAdjusterReview = Boolean.FALSE;

    // Audit fields
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
