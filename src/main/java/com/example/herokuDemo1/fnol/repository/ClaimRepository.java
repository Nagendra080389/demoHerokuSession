package com.example.herokuDemo1.fnol.repository;

import com.example.herokuDemo1.fnol.model.Claim;
import com.example.herokuDemo1.fnol.model.Status;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Claim entities.
 * This interface defines the contract for claim persistence operations.
 * Implementations can switch from in-memory to Salesforce or other storage systems.
 */
@Repository
public interface ClaimRepository {

    /**
     * Saves a claim entity.
     *
     * @param claim the claim to save
     * @param <S> the type of claim
     * @return the saved claim
     */
    <S extends Claim> S save(S claim);

    /**
     * Finds a claim by its ID.
     *
     * @param id the claim ID
     * @return optional claim if found
     */
    Optional<Claim> findById(Long id);

    /**
     * Finds all claims.
     *
     * @return list of all claims
     */
    List<Claim> findAll();

    /**
     * Finds claims by status.
     *
     * @param status the claim status
     * @return list of claims with the specified status
     */
    List<Claim> findByStatus(Status status);

    /**
     * Counts claims by status.
     *
     * @param status the claim status
     * @return count of claims with the specified status
     */
    long countByStatus(Status status);

    /**
     * Finds a claim by policy number, claimant name, and loss date (for idempotency).
     *
     * @param policyNumber the policy number
     * @param claimantName the claimant name
     * @param lossDate the loss date
     * @return optional claim if found
     */
    Optional<Claim> findByPolicyNumberAndClaimantNameAndLossDate(String policyNumber, String claimantName, String lossDate);

    /**
     * Deletes a claim by ID.
     *
     * @param id the claim ID
     */
    void deleteById(Long id);

    /**
     * Deletes a claim.
     *
     * @param claim the claim to delete
     */
    void delete(Claim claim);

    /**
     * Deletes all claims.
     */
    void deleteAll();

    /**
     * Counts total claims.
     *
     * @return total count of claims
     */
    long count();

    /**
     * Checks if a claim exists by ID.
     *
     * @param id the claim ID
     * @return true if exists, false otherwise
     */
    boolean existsById(Long id);
}
