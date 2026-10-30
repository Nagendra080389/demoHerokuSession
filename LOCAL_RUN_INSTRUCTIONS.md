# FNOL Intake Service - Local Run Instructions

## Prerequisites

- Java 8 (JDK 1.8+)
- Maven 3.8+
- Docker (optional, for containerized deployment)

## Local Development Setup

### 1. Download and Install Java 8

```bash
# Using SDKMAN (recommended)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 8.0.392-tem

# Or download directly
curl -L https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u392-b08/OpenJDK8U-jdk_aarch64_linux_hotspot_8u392b08.tar.gz -o /tmp/openjdk.tar.gz
tar -xzf /tmp/openjdk.tar.gz -C /opt
```

### 2. Configure Environment

```bash
export JAVA_HOME=/opt/jdk8u392-b08
export PATH=$JAVA_HOME/bin:$PATH
java -version  # Should show OpenJDK 8
```

### 3. Install Maven (if not available)

```bash
# Using SDKMAN
sdk install maven 3.8.8

# Or download directly
curl -L https://archive.apache.org/dist/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz -o /tmp/maven.tar.gz
tar -xzf /tmp/maven.tar.gz -C /opt
export M2_HOME=/opt/apache-maven-3.8.8
export PATH=$M2_HOME/bin:$PATH
mvn --version
```

### 4. Build the Project

```bash
cd /workspace
mvn clean compile
```

### 5. Run Unit Tests

```bash
mvn test
```

Expected output:
```
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

### 6. Run the Application

```bash
mvn spring-boot:run
```

The service will start on port 8080.

### 7. Verify the Server is Running

```bash
# Check the application health
curl http://localhost:8080/actuator/health
# (ifspring-boot-starter-actuator is added)

# Or simply try to access a protected endpoint
curl -s http://localhost:8080/api/claims
# Should return 401 Unauthorized (authentication required)
```

## API Endpoints

### Create a Claim (POST /api/claims)

```bash
curl -X POST http://localhost:8080/api/claims \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: test-key-123" \
  -d '{
    "policyNumber": "ATL-123456",
    "claimantName": "John Doe",
    "lossDate": "2026-10-30",
    "lossType": "AUTO",
    "description": "Minor fender bender at intersection",
    "estimatedAmount": 5000.00
  }'
```

Expected response (201 Created):
```json
{
  "id": 1,
  "claimId": "1",
  "policyNumber": "ATL-123456",
  "claimantName": "John Doe",
  "lossDate": "2026-10-30",
  "lossType": "AUTO",
  "description": "Minor fender bender at intersection",
  "estimatedAmount": 5000.00,
  "status": "SUBMITTED",
  "requiresAdjusterReview": false
}
```

### Get Claims (GET /api/claims)

```bash
curl http://localhost:8080/api/claims?status=SUBMITTED
```

### Get Claim by ID (GET /api/claims/{id})

```bash
curl http://localhost:8080/api/claims/1
```

### Update Claim Status (PATCH /api/claims/{id}/status)

```bash
curl -X PATCH http://localhost:8080/api/claims/1/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "UNDER_REVIEW"
  }'
```

## Security Configuration

### Development Mode (No Authentication)

For local development, authentication is disabled. The service uses HTTP Basic with a generated password (see logs).

### Production Mode (OAuth2/JWT)

Update `src/main/resources/application.properties`:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://your-auth-server.com
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://your-auth-server.com/.well-known/jwks.json
```

## H2 Database Console

The service includes H2 in-memory database for demo purposes.

Access the H2 console at: `http://localhost:8080/h2-console`

Connection details:
- JDBC URL: `jdbc:h2:mem:fnoldb`
- Username: `sa`
- Password: (empty)

## Docker Deployment

### Build Docker Image

```bash
docker build -t fnol-intake-service:latest .
```

### Run with Docker

```bash
docker run -p 8080:8080 fnol-intake-service:latest
```

### Run with Docker Compose

Create `docker-compose.yml`:

```yaml
version: '3.8'
services:
  fnol-service:
    image: fnol-intake-service:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
```

```bash
docker-compose up
```

## OpenAPI/Swagger Documentation

If Springfox is enabled, access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

## Troubleshooting

### Java Version Mismatch

If you see `Unsupported major.minor version` error:
```bash
java -version  # Ensure version is 1.8.x
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
```

### Maven Build Errors

```bash
# Clear local repository cache
rm -rf ~/.m2/repository/com/example/herokuDemo1
mvn clean install
```

### Port Already in Use

```bash
# Find process using port 8080
netstat -tlnp | grep 8080

# Or run on different port
mvn spring-boot:run -Dserver.port=8081
```

## Next Steps

1. Verify all acceptance criteria are met
2. Review the design document at `docs/design/fnol-design.md`
3. Proceed with Salesforce integration for production
4. Add AWS Textract integration for document extraction
