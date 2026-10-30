package com.example.herokuDemo1.fnol.repository;

import com.example.herokuDemo1.fnol.model.Claim;
import com.example.herokuDemo1.fnol.model.Status;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of ClaimRepository for demo purposes.
 * This implementation uses a ConcurrentHashMap for thread-safe operations.
 * Should be replaced with SalesforceClaimRepository or similar for production.
 */
@Repository
public class MemoryClaimRepository implements ClaimRepository {

    private final Map<Long, Claim> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @PostConstruct
    public void init() {
        // Initialize with sample data for demo purposes
        store.put(idGenerator.get(), Claim.builder()
                .id(idGenerator.get())
                .policyNumber("ATL-123456")
                .claimantName("John Doe")
                .lossDate(LocalDate.now().minusDays(5))
                .lossType("AUTO")
                .description("Minor fender bender at intersection")
                .estimatedAmount(new BigDecimal("5000.00"))
                .status(Status.SUBMITTED)
                .requiresAdjusterReview(false)
                .build());
        idGenerator.incrementAndGet();
    }

    @Override
    public <S extends Claim> S save(S claim) {
        if (claim.getId() == null) {
            claim.setId(idGenerator.getAndIncrement());
        }
        store.put(claim.getId(), claim);
        return claim;
    }

    @Override
    public Optional<Claim> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Claim> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Claim> findByStatus(Status status) {
        return store.values().stream()
                .filter(claim -> claim.getStatus() == status)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public long countByStatus(Status status) {
        return store.values().stream()
                .filter(claim -> claim.getStatus() == status)
                .collect(java.util.stream.Collectors.toList())
                .size();
    }

    @Override
    public Optional<Claim> findByPolicyNumberAndClaimantNameAndLossDate(String policyNumber, String claimantName, String lossDate) {
        return store.values().stream()
                .filter(claim -> policyNumber.equals(claim.getPolicyNumber())
                        && claimantName.equals(claim.getClaimantName())
                        && lossDate.equals(claim.getLossDate().toString()))
                .findFirst();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public void delete(Claim claim) {
        store.values().removeIf(c -> c.getId().equals(claim.getId()));
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }
}
