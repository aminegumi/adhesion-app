# Test Report Generation Scripts

## 1. JaCoCo Code Coverage Report (Already Generated)
Report location: `target/site/jacoco/index.html`

To regenerate:
```powershell
.\mvnw.cmd clean test jacoco:report
```

## 2. SonarQube Analysis

### Option A: SonarCloud (Cloud-based)
1. Create account at https://sonarcloud.io
2. Create a project and get your organization key and project key
3. Generate a token at https://sonarcloud.io/account/security
4. Run analysis:
```powershell
.\mvnw.cmd sonar:sonar `
  -Dsonar.host.url=https://sonarcloud.io `
  -Dsonar.organization=YOUR_ORG_KEY `
  -Dsonar.projectKey=YOUR_PROJECT_KEY `
  -Dsonar.token=YOUR_SONAR_TOKEN
```

### Option B: Local SonarQube Server
1. Start SonarQube via Docker:
```powershell
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts
```
2. Wait for SonarQube to start (visit http://localhost:9000)
3. Login with admin/admin and generate a token
4. Run analysis:
```powershell
.\mvnw.cmd sonar:sonar `
  -Dsonar.host.url=http://localhost:9000 `
  -Dsonar.token=YOUR_GENERATED_TOKEN
```

## 3. JMeter Performance Tests

### Run API Performance Tests:
```powershell
# Ensure JMeter is installed
# Download from https://jmeter.apache.org/download_jmeter.cgi

# Create results folder
New-Item -ItemType Directory -Force -Path "results"

# Run performance test (headless mode) - USE ABSOLUTE PATHS from JMeter bin directory
jmeter -n -t "c:\Users\topim\Desktop\adhesion-app\jmeter\adhesion-api-performance-test.jmx" -l "c:\Users\topim\Desktop\adhesion-app\results\performance-results.jtl" -e -o "c:\Users\topim\Desktop\adhesion-app\results\performance-report"

# Run stress test
jmeter -n -t "c:\Users\topim\Desktop\adhesion-app\jmeter\adhesion-stress-test.jmx" -l "c:\Users\topim\Desktop\adhesion-app\results\stress-results.jtl" -e -o "c:\Users\topim\Desktop\adhesion-app\results\stress-report"
```

## 4. Selenium E2E Tests
Selenium tests require both servers running:

```powershell
# Terminal 1 - Start Spring Boot API
.\mvnw.cmd spring-boot:run

# Terminal 2 - Start Angular frontend
cd adhesion-admin-angular
npm start

# Terminal 3 - Run Selenium tests (use -Pe2e profile!)
.\mvnw.cmd test -Pe2e
```

## Test Summary

| Test Type | Location | Command |
|-----------|----------|---------|
| Unit Tests | `src/test/java` | `.\mvnw.cmd test` |
| JaCoCo Coverage | `target/site/jacoco/index.html` | `.\mvnw.cmd jacoco:report` |
| SonarQube | SonarCloud/localhost:9000 | `.\mvnw.cmd sonar:sonar` |
| JMeter | `jmeter/*.jmx` | See JMeter section |
| Selenium E2E | `src/test/java/.../selenium` | `.\mvnw.cmd test -Dgroups=e2e` |

## Current Test Results
- **82 tests passed** (Unit tests for services)
- **E2E tests excluded** from regular run (require running servers)
- **JaCoCo report generated** at target/site/jacoco/index.html

## Test Files Created:
1. `UserServiceTest.java` - User authentication, registration, profile management
2. `TestScoringServiceTest.java` - Psychological test scoring interpretations  
3. `UserMedicationServiceTest.java` - Medication CRUD operations
4. `TreatmentPlanServiceTest.java` - Treatment plan management
5. `AdherencePredictionServiceTest.java` - Adherence prediction service
6. `AuthenticationE2ETest.java` - Selenium login/register tests
7. `DashboardE2ETest.java` - Selenium dashboard tests
8. `AdminDashboardE2ETest.java` - Selenium admin dashboard tests
