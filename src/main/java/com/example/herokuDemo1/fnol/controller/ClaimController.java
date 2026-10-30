package com.example.herokuDemo1.fnol.controller;

import com.example.herokuDemo1.fnol.dto.ClaimCreateRequest;
import com.example.herokuDemo1.fnol.dto.ClaimResponse;
import com.example.herokuDemo1.fnol.dto.StatusUpdateRequest;
import com.example.herokuDemo1.fnol.service.ClaimService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * REST Controller for FNOL claim operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/claims")
@Validated
public class ClaimController {

    private final ClaimService claimService;

    @Autowired
    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    /**
     * Creates a new claim.
     *
     * @param request the claim creation request
     * @param idempotencyKey the optional idempotency key for deduplication
     * @return 201 Created with the claim response
     */
    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(
            @Valid @RequestBody ClaimCreateRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Creating claim for policy: {}, claimant: {}", request.getPolicyNumber(), request.getClaimantName());

        ClaimResponse claimResponse = claimService.createClaim(request, idempotencyKey);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/claims/" + claimResponse.getId())
                .body(claimResponse);
    }

    /**
     * Retrieves a claim by its ID.
     *
     * @param id the claim ID
     * @return 200 OK with claim response, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> getClaim(@PathVariable Long id) {
        log.info("Retrieving claim with id: {}", id);

        return claimService.getClaimById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all claims, optionally filtered by status.
     *
     * @param status optional status filter
     * @return 200 OK with list of claims
     */
    @GetMapping
    public ResponseEntity<List<ClaimResponse>> getAllClaims(
            @RequestParam(value = "status", required = false) String status) {

        log.info("Retrieving claims with status filter: {}", status);

        List<ClaimResponse> claims;
        if (status != null && !status.isEmpty()) {
            claims = claimService.getClaimsByStatus(status);
        } else {
            claims = claimService.getAllClaims();
        }

        return ResponseEntity.ok(claims);
    }

    /**
     * Updates the status of a claim.
     *
     * @param id the claim ID
     * @param request the status update request
     * @return 200 OK with updated claim, 404 Not Found, or 409 Conflict for invalid transition
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ClaimResponse> updateClaimStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {

        log.info("Updating status for claim id: {} to: {}", id, request.getStatus());

        try {
            ClaimResponse updatedClaim = claimService.updateClaimStatus(id, request);
            return ResponseEntity.ok(updatedClaim);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }
}
