package com.example.herokuDemo1.fnol.service;

import com.example.herokuDemo1.fnol.dto.ClaimCreateRequest;
import com.example.herokuDemo1.fnol.dto.ClaimResponse;
import com.example.herokuDemo1.fnol.dto.StatusUpdateRequest;
import com.example.herokuDemo1.fnol.model.Claim;
import com.example.herokuDemo1.fnol.model.Status;
import com.example.herokuDemo1.fnol.repository.ClaimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for claim operations.
 */
@Service
@Transactional
public class ClaimService {

    private static final BigDecimal ADJUSTER_REVIEW_THRESHOLD = new BigDecimal("10000");

    private final ClaimRepository repository;
    private final SecurityContextService securityContextService;

    @Autowired
    public ClaimService(ClaimRepository repository, SecurityContextService securityContextService) {
        this.repository = repository;
        this.securityContextService = securityContextService;
    }

    /**
     * Creates a new claim from the provided request.
     *
     * @param request the claim creation request
     * @param idempotencyKey optional idempotency key for deduplication
     * @return the created claim response
     */
    @Transactional
    public ClaimResponse createClaim(ClaimCreateRequest request, String idempotencyKey) {
        // Check for idempotency
        Optional<Claim> existingClaim = repository.findByPolicyNumberAndClaimantNameAndLossDate(
                request.getPolicyNumber(),
                request.getClaimantName(),
                request.getLossDate());

        if (existingClaim.isPresent()) {
            return ClaimResponse.from(existingClaim.get());
        }

        // Extract loss date from ISO format string
        LocalDate lossDate = LocalDate.parse(request.getLossDate(), DateTimeFormatter.ISO_DATE);

        // Build the claim entity
        Claim claim = Claim.builder()
                .policyNumber(request.getPolicyNumber())
                .claimantName(request.getClaimantName().trim())
                .lossDate(lossDate)
                .lossType(request.getLossType().toUpperCase())
                .description(request.getDescription().trim())
                .estimatedAmount(request.getEstimatedAmount())
                .status(Status.SUBMITTED)
                .requiresAdjusterReview(request.getEstimatedAmount().compareTo(ADJUSTER_REVIEW_THRESHOLD) > 0)
                .build();

        // Set audit fields
        String principalName = securityContextService.getPrincipalName();
        claim.setCreatedBy(principalName);
        claim.setUpdatedAt(System.currentTimeMillis());
        claim.setCreatedAt(System.currentTimeMillis());

        // Save to repository
        Claim savedClaim = repository.save(claim);

        return ClaimResponse.from(savedClaim);
    }

    /**
     * Retrieves a claim by its ID.
     *
     * @param id the claim ID
     * @return optional claim response
     */
    public Optional<ClaimResponse> getClaimById(Long id) {
        return repository.findById(id)
                .map(ClaimResponse::from);
    }

    /**
     * Retrieves all claims.
     *
     * @return list of all claim responses
     */
    public List<ClaimResponse> getAllClaims() {
        return repository.findAll().stream()
                .map(ClaimResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves claims filtered by status.
     *
     * @param status the status to filter by
     * @return list of claims with the specified status
     */
    public List<ClaimResponse> getClaimsByStatus(String status) {
        Status targetStatus = Status.valueOf(status);
        return repository.findByStatus(targetStatus).stream()
                .map(ClaimResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Updates the status of a claim.
     *
     * @param id the claim ID
     * @param request the status update request
     * @return the updated claim response
     * @throws IllegalArgumentException if the status transition is invalid
     */
    public ClaimResponse updateClaimStatus(Long id, StatusUpdateRequest request) {
        Status newStatus = Status.valueOf(request.getStatus().toUpperCase());
        Claim claim = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found with id: " + id));

        // Validate status transition
        validateStatusTransition(claim.getStatus(), newStatus);

        // Update status and audit fields
        claim.setStatus(newStatus);
        String principalName = securityContextService.getPrincipalName();
        claim.setUpdatedBy(principalName);
        claim.setUpdatedAt(System.currentTimeMillis());

        Claim updatedClaim = repository.save(claim);

        return ClaimResponse.from(updatedClaim);
    }

    /**
     * Validates that the status transition is allowed.
     *
     * @param currentStatus the current status
     * @param newStatus the desired new status
     * @throws IllegalArgumentException if the transition is not valid
     */
    private void validateStatusTransition(Status currentStatus, Status newStatus) {
        boolean isValid = false;

        switch (currentStatus) {
            case SUBMITTED:
                isValid = (newStatus == Status.UNDER_REVIEW);
                break;
            case UNDER_REVIEW:
                isValid = (newStatus == Status.APPROVED || newStatus == Status.REJECTED);
                break;
            case APPROVED:
            case REJECTED:
                // Terminal states - no transitions allowed
                isValid = false;
                break;
        }

        if (!isValid) {
            throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }
}
