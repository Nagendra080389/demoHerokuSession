# First Notice of Loss (FNOL) Intake Service - Solution Design

## Overview

This document describes the solution design for the FNOL (First Notice of Loss) intake service for the Atlas Insurance Connected Claims project. The service serves as the initial claims capture layer before onward processing to Salesforce Service Cloud and integration with AWS Textract for document extraction.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API Gateway / Load Balancer                        │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
┌─────────────────────────────────────▼───────────────────────────────────────┐
│                          FNOL Intake Service                                 │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                         Controller Layer                              │  │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐    │  │
│  │  │   ClaimController│  │  SecurityFilter  │  │ExceptionHandling │    │  │
│  │  └────────┬─────────┘  └──────────────────┘  └──────────────────┘    │  │
│  └───────────┼───────────────────────────────────────────────────────────┘  │
│              │                                                                │
│  ┌───────────▼───────────────────────────────────────────────────────────┐  │
│  │                         Service Layer                                 │  │
│  │  ┌──────────────────┐  ┌──────────────────┐                          │  │
│  │  │   ClaimService   │  │SecurityContext   │                          │  │
│  │  └────────┬─────────┘  └──────────────────┘                          │  │
│  └───────────┼───────────────────────────────────────────────────────────┘  │
│              │                                                                │
│  ┌───────────▼───────────────────────────────────────────────────────────┐  │
│  │                      Repository Layer                                  │  │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐    │  │
│  │  │ ClaimRepository  │  │MemoryClaimRepo   │  │SalesforceClaimRepo│    │  │
│  │  └──────────────────┘  └──────────────────┘  └──────────────────┘    │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            │                         │                         │
    ┌───────▼───────┐     ┌───────────▼───────────┐   ┌───────────▼───────────┐
    │  H2 (Demo)    │     │    Salesforce Org     │   │     AWS Textract      │
    │  In-Memory DB │     │   (Production)        │   │   (Document Mgmt)     │
    └───────────────┘     └───────────────────────┘   └───────────────────────┘
```

## Domain Model

### Claim Entity

```
┌───────────────────────────────────────────────────────────────────────┐
│                             Claim                                     │
├───────────────────────────────────────────────────────────────────────┤
│ + id: Long                                                            │
│ + policyNumber: String (ATL-######)                                  │
│ + claimantName: String (max 100 chars)                               │
│ + lossDate: LocalDate (ISO 8601)                                     │
│ + lossType: String (AUTO, HOME, LIABILITY)                          │
│ + description: String (max 2000 chars)                               │
│ + estimatedAmount: BigDecimal (max 2 decimals)                       │
│ + status: Status (SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED)      │
│ + requiresAdjusterReview: Boolean                                    │
│ + createdBy: String                                                  │
│ + createdAt: Long (timestamp)                                        │
│ + updatedBy: String                                                  │
│ + updatedAt: Long (timestamp)                                        │
└───────────────────────────────────────────────────────────────────────┘
```

### Status Enum

```
┌───────────────────────────────────────────────────────────────────────┐
│                             Status                                    │
├───────────────────────────────────────────────────────────────────────┤
│ • SUBMITTED                                                           │
│ • UNDER_REVIEW                                                        │
│ • APPROVED                                                            │
│ • REJECTED                                                            │
└───────────────────────────────────────────────────────────────────────┘
```

## ERD

```
┌───────────────────────────────────────────────────────────────────────┐
│                              claims                                   │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  id                   BIGINT        PK                                │
│  policy_number        VARCHAR(8)    NOT NULL                         │
│  claimant_name        VARCHAR(100)  NOT NULL                         │
│  loss_date            DATE          NOT NULL                         │
│  loss_type            VARCHAR(10)   NOT NULL                         │
│  description          TEXT          NOT NULL                         │
│  estimated_amount     DECIMAL(12,2) NOT NULL                         │
│  status               VARCHAR(20)   NOT NULL                         │
│  requires_adjuster_review BOOLEAN       NOT NULL                     │
│  created_by           VARCHAR(100)  NOT NULL                         │
│  created_at           BIGINT        NOT NULL                         │
│  updated_by           VARCHAR(100)                                   │
│  updated_at           BIGINT                                         │
│                                                                       │
│  INDEX idx_policy_number (policy_number)                            │
│  INDEX idx_status (status)                                          │
│  INDEX idx_loss_date (loss_date)                                    │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

## Sequence Diagrams

### Create Claim (Idempotent)

```
Client                    Controller             Service            Repository
  |                          |                    |                   |
  | POST /api/claims         |                    |                   |
  |------------------------->|                    |                   |
  | X-Idempotency-Key        |                    |                   |
  |                          |                    |                   |
  |                          | validate()         |                   |
  |                          |------------------->|                   |
  |                          |                    |                   |
  |                          |  check existing    |                   |
  |                          |------------------->|                   |
  |                          |  findByPolicy...   |                   |
  |                          |<-------------------|                   |
  |                          |                    |                   |
  |                          |                    |                   |
  | 201 Created              |                    |                   |
  |<-------------------------|                    |                   |
  | { claimId, status }      |                    |                   |
```

### Update Status

```
Client                    Controller             Service            Repository
  |                          |                    |                   |
  | PATCH /api/claims/{id}   |                    |                   |
  |------------------------->|                    |                   |
  | { status }               |                    |                   |
  |                          |                    |                   |
  |                          | getClaimById()     |                   |
  |                          |------------------->|                   |
  |                          |                    |                   |
  |                          | validateTransition |                   |
  |                          |                    |                   |
  |                          | updateStatus()     |                   |
  |                          |------------------->|                   |
  |                          |                    | save()            |
  |                          |                    |------------------>|
  |                          |                    |                   |
  | 200 OK                   |                    |                   |
  |<-------------------------|                    |                   |
  | { updated claim }        |                    |                   |
```

## API Endpoints

### POST /api/claims

Creates a new claim.

**Request Body:**
```json
{
  "policyNumber": "ATL-123456",
  "claimantName": "John Doe",
  "lossDate": "2026-10-30",
  "lossType": "AUTO",
  "description": "Minor fender bender at intersection",
  "estimatedAmount": 5000.00
}
```

**Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <token>`
- `X-Idempotency-Key: <optional-unique-key>`

**Responses:**
- `201 Created`: Claim created successfully
- `400 Bad Request`: Validation failed
- `401 Unauthorized`: Missing/invalid token
- `403 Forbidden`: Insufficient permissions

### GET /api/claims/{id}

Retrieves a claim by ID.

**Headers:**
- `Authorization: Bearer <token>`

**Responses:**
- `200 OK`: Claim found
- `404 Not Found`: Claim does not exist
- `401 Unauthorized`: Missing/invalid token

### GET /api/claims

Retrieves all claims, optionally filtered by status.

**Query Parameters:**
- `status`: Optional filter (SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED)

**Headers:**
- `Authorization: Bearer <token>`

**Responses:**
- `200 OK`: List of claims
- `401 Unauthorized`: Missing/invalid token

### PATCH /api/claims/{id}/status

Updates the status of a claim.

**Request Body:**
```json
{
  "status": "UNDER_REVIEW"
}
```

**Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <token>`

**Responses:**
- `200 OK`: Status updated
- `404 Not Found`: Claim does not exist
- `409 Conflict`: Invalid status transition
- `401 Unauthorized`: Missing/invalid token

## Validation Rules

| Field | Rule | Error Code |
|-------|------|------------|
| policyNumber | Must match `^ATL-\d{6}$` | INVALID_POLICY_NUMBER |
| claimantName | Non-empty, max 100 chars | CLAIMANT_NAME_REQUIRED |
| lossDate | ISO 8601 format, not in future | LOSS_DATE_FUTURE |
| lossType | One of: AUTO, HOME, LIABILITY | INVALID_LOSS_TYPE |
| description | Non-empty, max 2000 chars | DESCRIPTION_REQUIRED |
| estimatedAmount | Numeric >= 0, max 2 decimals | ESTIMATED_AMOUNT_NEGATIVE |

## Business Rules

1. **Adjuster Review Flag**: `requiresAdjusterReview = true` if `estimatedAmount > 10000`
2. **Status Transitions**:
   - `SUBMITTED` → `UNDER_REVIEW` ✓
   - `SUBMITTED` → `APPROVED` ✗ (invalid)
   - `SUBMITTED` → `REJECTED` ✗ (invalid)
   - `UNDER_REVIEW` → `APPROVED` ✓
   - `UNDER_REVIEW` → `REJECTED` ✓
   - `APPROVED` → any ✗ (terminal state)
   - `REJECTED` → any ✗ (terminal state)

## Security Configuration

### Authentication
- OAuth2 Resource Server
- JWT token validation

### Authorization Scopes
- `Claims.READ`: Read-only access (GET endpoints)
- `Claims.WRITE`: Write access (POST, PATCH endpoints)

### Audit Fields
- `createdBy`: Set from security context on create
- `createdAt`: Set on create (timestamp)
- `updatedBy`: Set from security context on update
- `updatedAt`: Set on update (timestamp)

## Repository Layer

### Interface: ClaimRepository

```java
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByStatus(Status status);
    long countByStatus(Status status);
    Optional<Claim> findByPolicyNumberAndClaimantNameAndLossDate(
        String policyNumber, String claimantName, String lossDate);
}
```

### Implementations

1. **MemoryClaimRepository** (current)
   - Thread-safe ConcurrentHashMap
   - Used for development/demo
   - In-memory storage only

2. **SalesforceClaimRepository** (future)
   - Integrate with Salesforce REST API
   - Synchronize with Salesforce Service Cloud
   - Production-ready persistence

## Testing Strategy

### Unit Tests
- ClaimValidatorTest
- MemoryClaimRepositoryTest
- ClaimServiceTest
- LossTypeValidatorTest

### Integration Tests
- ClaimControllerIntegrationTest
- IdempotencyIntegrationTest
- StatusTransitionIntegrationTest
- SecurityIntegrationTest

### Test Coverage Target
- ≥ 90% for service and controller layers

## Deployment Considerations

### Development/Local
- H2 in-memory database
- OAuth2 disabled (for testing)
- Run: `mvn spring-boot:run`

### Production
- JDBC-compatible database (PostgreSQL, Oracle)
- OAuth2 with proper JWT issuer configuration
- Salesforce integration
- AWS Textract integration for document extraction

## Future Enhancements

1. **Integration with AWS Textract**
   - Extract claim document data
   - Auto-populate claim fields
   - Send for human review if confidence < threshold

2. **Salesforce Integration**
   - Sync claims to Salesforce Service Cloud
   - Create Cases from claims
   - Bidirectional synchronization

3. **Workflow Engine**
   - Auto-assign adjusters based on rules
   - Escalation workflows
   - Notification workflows

4. **Advanced Features**
   - Batch claim creation
   - Claim analytics dashboard
   - Export functionality

## Open Questions

1. Should retry logic be built into submission endpoint?
   - **Decision**: No, for initial release
2. Will claim owner be derived from token or explicit payload?
   - **Decision**: Derived from authenticated token (`createdBy = principalName`)

## References

- Project Documentation: [Atlas Insurance Connected Claims]
- Requirements: [FNOL Intake Service Requirements]
- API Contract: [OpenAPI Specification - TBD]
