package com.example.herokuDemo1.fnol.service;

import com.example.herokuDemo1.fnol.dto.ClaimCreateRequest;
import com.example.herokuDemo1.fnol.dto.ClaimResponse;
import com.example.herokuDemo1.fnol.dto.StatusUpdateRequest;
import com.example.herokuDemo1.fnol.model.Claim;
import com.example.herokuDemo1.fnol.model.Status;
import com.example.herokuDemo1.fnol.repository.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClaimService.
 */
class ClaimServiceTest {

    private ClaimRepository repository;
    private SecurityContextService securityContextService;
    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        repository = mock(ClaimRepository.class);
        securityContextService = mock(SecurityContextService.class);
        claimService = new ClaimService(repository, securityContextService);
    }

    @Test
    void createClaim_withValidRequest_shouldReturnCreatedClaim() {
        // Given
        when(securityContextService.getPrincipalName()).thenReturn("test-user");

        ClaimCreateRequest request = ClaimCreateRequest.builder()
                .policyNumber("ATL-123456")
                .claimantName("John Doe")
                .lossDate("2026-10-30")
                .lossType("AUTO")
                .description("Test description")
                .estimatedAmount(BigDecimal.valueOf(5000))
                .build();

        Claim expectedClaim = createClaimEntity(request, Status.SUBMITTED, false);
        when(repository.findByPolicyNumberAndClaimantNameAndLossDate(
                eq("ATL-123456"), eq("John Doe"), eq("2026-10-30")))
                .thenReturn(Optional.empty());

        when(repository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ClaimResponse response = claimService.createClaim(request, null);

        // Then
        assertNotNull(response);
        assertEquals("ATL-123456", response.getPolicyNumber());
        assertEquals(Status.SUBMITTED.name(), response.getStatus());
        assertFalse(response.getRequiresAdjusterReview());
    }

    @Test
    void createClaim_withAmountOver10000_shouldSetRequiresAdjusterReview() {
        // Given
        when(securityContextService.getPrincipalName()).thenReturn("test-user");

        ClaimCreateRequest request = ClaimCreateRequest.builder()
                .policyNumber("ATL-123456")
                .claimantName("John Doe")
                .lossDate("2026-10-30")
                .lossType("AUTO")
                .description("Test description")
                .estimatedAmount(BigDecimal.valueOf(15000))
                .build();

        Claim expectedClaim = createClaimEntity(request, Status.SUBMITTED, true);
        when(repository.findByPolicyNumberAndClaimantNameAndLossDate(
                eq("ATL-123456"), eq("John Doe"), eq("2026-10-30")))
                .thenReturn(Optional.empty());

        when(repository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ClaimResponse response = claimService.createClaim(request, null);

        // Then
        assertTrue(response.getRequiresAdjusterReview());
    }

    @Test
    void createClaim_withIdempotencyKey_shouldReturnExistingClaim() {
        // Given
        ClaimCreateRequest request = ClaimCreateRequest.builder()
                .policyNumber("ATL-123456")
                .claimantName("John Doe")
                .lossDate("2026-10-30")
                .lossType("AUTO")
                .description("Test description")
                .estimatedAmount(BigDecimal.valueOf(5000))
                .build();

        Claim existingClaim = createClaimEntity(request, Status.SUBMITTED, false);
        existingClaim.setId(1L);

        when(repository.findByPolicyNumberAndClaimantNameAndLossDate(
                eq("ATL-123456"), eq("John Doe"), eq("2026-10-30")))
                .thenReturn(Optional.of(existingClaim));

        // When
        ClaimResponse response = claimService.createClaim(request, "idempotency-key-123");

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId().longValue());
        // Verify repository.save was not called
        verify(repository, never()).save(any(Claim.class));
    }

    @Test
    void getClaimById_withExistingId_shouldReturnClaim() {
        // Given
        Claim claim = createClaimEntity(Status.SUBMITTED, false);
        claim.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(claim));

        // When
        java.util.Optional<ClaimResponse> result = claimService.getClaimById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(Status.SUBMITTED.name(), result.get().getStatus());
    }

    @Test
    void getClaimById_withNonExistentId_shouldReturnEmpty() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When
        java.util.Optional<ClaimResponse> result = claimService.getClaimById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void updateClaimStatus_withValidTransition_shouldUpdateStatus() {
        // Given
        Claim claim = createClaimEntity(Status.SUBMITTED, false);
        claim.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(claim));
        when(securityContextService.getPrincipalName()).thenReturn("adjuster-user");
        when(repository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StatusUpdateRequest request = StatusUpdateRequest.builder()
                .status("UNDER_REVIEW")
                .build();

        // When
        ClaimResponse response = claimService.updateClaimStatus(1L, request);

        // Then
        assertNotNull(response);
        assertEquals(Status.UNDER_REVIEW.name(), response.getStatus());
        assertEquals("adjuster-user", response.getUpdatedBy());
    }

    @Test
    void updateClaimStatus_withInvalidTransition_shouldThrowException() {
        // Given
        Claim claim = createClaimEntity(Status.SUBMITTED, false);
        claim.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(claim));

        StatusUpdateRequest request = StatusUpdateRequest.builder()
                .status("APPROVED")  // Invalid: cannot skip UNDER_REVIEW
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> claimService.updateClaimStatus(1L, request)
        );
        assertTrue(exception.getMessage().contains("Invalid status transition"));
    }

    @Test
    void updateClaimStatus_withTerminalState_shouldThrowException() {
        // Given
        Claim claim = createClaimEntity(Status.APPROVED, false);
        claim.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(claim));

        StatusUpdateRequest request = StatusUpdateRequest.builder()
                .status("SUBMITTED")  // Invalid: cannot go back
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> claimService.updateClaimStatus(1L, request)
        );
        assertTrue(exception.getMessage().contains("Invalid status transition"));
    }

    @Test
    void getClaimsByStatus_shouldFilterByStatus() {
        // Given
        Claim submitted = createClaimEntity(Status.SUBMITTED, false);
        submitted.setId(1L);
        Claim underReview = createClaimEntity(Status.UNDER_REVIEW, false);
        underReview.setId(2L);

        when(repository.findByStatus(Status.SUBMITTED)).thenReturn(java.util.Arrays.asList(submitted));

        // When
        java.util.List<ClaimResponse> result = claimService.getClaimsByStatus("SUBMITTED");

        // Then
        assertEquals(1, result.size());
        assertEquals(Status.SUBMITTED.name(), result.get(0).getStatus());
    }

    @Test
    void getAllClaims_shouldReturnAllClaims() {
        // Given
        Claim claim1 = createClaimEntity(Status.SUBMITTED, false);
        claim1.setId(1L);
        Claim claim2 = createClaimEntity(Status.UNDER_REVIEW, false);
        claim2.setId(2L);

        when(repository.findAll()).thenReturn(java.util.Arrays.asList(claim1, claim2));

        // When
        java.util.List<ClaimResponse> result = claimService.getAllClaims();

        // Then
        assertEquals(2, result.size());
    }

    private Claim createClaimEntity(Status status, boolean requiresReview) {
        return createClaimEntity(
                ClaimCreateRequest.builder()
                        .policyNumber("ATL-123456")
                        .claimantName("John Doe")
                        .lossDate("2026-10-30")
                        .lossType("AUTO")
                        .description("Test description")
                        .estimatedAmount(BigDecimal.valueOf(5000))
                        .build(),
                status, requiresReview);
    }

    private Claim createClaimEntity(ClaimCreateRequest request, Status status, boolean requiresReview) {
        return Claim.builder()
                .policyNumber(request.getPolicyNumber())
                .claimantName(request.getClaimantName())
                .lossDate(LocalDate.parse(request.getLossDate()))
                .lossType(request.getLossType())
                .description(request.getDescription())
                .estimatedAmount(request.getEstimatedAmount())
                .status(status)
                .requiresAdjusterReview(requiresReview)
                .build();
    }
}
