# Database Reset Script for Adhesion App
# This script completely resets the PostgreSQL database to a fresh state

Write-Host "=== Adhesion App Database Reset Script ===" -ForegroundColor Cyan
Write-Host ""

# Check if Docker container is running
$container = docker ps --filter "name=adhesion_db" --format "{{.Names}}" 2>$null

if ($container -ne "adhesion_db") {
    Write-Host "Starting Docker containers..." -ForegroundColor Yellow
    docker-compose up -d
    Start-Sleep -Seconds 5
}

Write-Host "Resetting database schema..." -ForegroundColor Yellow

# Drop and recreate schema using a single SQL command
$sql = "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO postgres; GRANT ALL ON SCHEMA public TO public;"
docker exec adhesion_db psql -U postgres -d adhesion_db -c $sql

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[OK] Database reset successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Stop any running Spring Boot instance (Ctrl+C)"
    Write-Host "2. Restart the backend: .\mvnw spring-boot:run"
    Write-Host "3. The database will be recreated with fresh tables"
}
else {
    Write-Host ""
    Write-Host "[ERROR] Database reset failed!" -ForegroundColor Red
    Write-Host "Make sure Docker is running and the adhesion_db container is up."
}
